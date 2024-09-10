package com.bilibili.rag.service;

import com.bilibili.rag.utils.HttpUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * B站视频信息爬取服务
 * 通过 B站公开 API 获取视频标题、简介、字幕等内容
 */
@Service
public class BilibiliCrawlService {

    private static final String API_VIDEO_INFO = "https://api.bilibili.com/x/web-interface/view?bvid=";
    private static final String API_SUBTITLE    = "https://api.bilibili.com/x/player/v2?bvid=%s&cid=%s";
    private static final String API_AI_SUMMARY  = "https://api.bilibili.com/x/web-interface/view/conclusion/get?bvid=%s&cid=%s&up_mid=%s";

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 从 URL 或 BV号 提取 bvid
     * 支持格式：
     *   BV1xx411c7mD
     *   https://www.bilibili.com/video/BV1xx411c7mD
     *   https://b23.tv/xxxxx（短链，需要先重定向）
     */
    public String extractBvid(String input) {
        if (input == null) return null;
        input = input.trim();

        // 直接是 BV 号
        if (input.matches("BV[a-zA-Z0-9]{10}")) return input;

        // 从 URL 提取
        Pattern p = Pattern.compile("BV([a-zA-Z0-9]{10})");
        Matcher m = p.matcher(input);
        if (m.find()) return "BV" + m.group(1);

        return null;
    }

    /**
     * 获取视频完整信息（标题、简介、UP主、封面、时长、cid）
     */
    public Map<String, Object> fetchVideoInfo(String bvid) throws Exception {
        String json = HttpUtils.get(API_VIDEO_INFO + bvid);
        JsonNode root = objectMapper.readTree(json);

        if (root.path("code").asInt() != 0) {
            throw new RuntimeException("B站 API 返回错误：" + root.path("message").asText());
        }

        JsonNode data = root.path("data");
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("bvid", bvid);
        info.put("title", data.path("title").asText());
        info.put("description", data.path("desc").asText());
        info.put("pic_url", data.path("pic").asText());
        info.put("duration", data.path("duration").asInt());
        info.put("owner_name", data.path("owner").path("name").asText());
        info.put("owner_mid", data.path("owner").path("mid").asLong());

        // 取第一个分P的 cid
        JsonNode pages = data.path("pages");
        if (pages.isArray() && pages.size() > 0) {
            info.put("cid", pages.get(0).path("cid").asLong());
        }

        return info;
    }

    /**
     * 获取视频字幕（CC字幕）
     * 返回纯文本内容，如果没有字幕返回 null
     */
    public String fetchSubtitle(String bvid, long cid) throws Exception {
        String json = HttpUtils.get(String.format(API_SUBTITLE, bvid, cid));
        JsonNode root = objectMapper.readTree(json);

        if (root.path("code").asInt() != 0) return null;

        JsonNode subtitles = root.path("data").path("subtitle").path("subtitles");
        if (!subtitles.isArray() || subtitles.isEmpty()) return null;

        // 优先取中文字幕
        String subtitleUrl = null;
        for (JsonNode sub : subtitles) {
            String lang = sub.path("lan").asText();
            if (lang.contains("zh") || lang.contains("cn")) {
                subtitleUrl = sub.path("subtitle_url").asText();
                break;
            }
        }
        // 没有中文就取第一个
        if (subtitleUrl == null) {
            subtitleUrl = subtitles.get(0).path("subtitle_url").asText();
        }

        if (subtitleUrl.isBlank()) return null;

        // 字幕 URL 可能是 // 开头
        if (subtitleUrl.startsWith("//")) subtitleUrl = "https:" + subtitleUrl;

        return parseSubtitleJson(subtitleUrl);
    }

    /**
     * 获取 B站 AI 视频总结（需要视频有 AI 摘要）
     */
    public String fetchAiSummary(String bvid, long cid, long upMid) throws Exception {
        String json = HttpUtils.get(String.format(API_AI_SUMMARY, bvid, cid, upMid));
        JsonNode root = objectMapper.readTree(json);

        if (root.path("code").asInt() != 0) return null;

        JsonNode modelResult = root.path("data").path("model_result");
        if (modelResult.isMissingNode()) return null;

        StringBuilder sb = new StringBuilder();

        // 总结
        String summary = modelResult.path("summary").asText();
        if (!summary.isBlank()) sb.append(summary).append("\n\n");

        // 章节要点
        JsonNode outline = modelResult.path("outline");
        if (outline.isArray()) {
            for (JsonNode section : outline) {
                String title = section.path("title").asText();
                if (!title.isBlank()) sb.append("【").append(title).append("】\n");
                JsonNode parts = section.path("part_outline");
                if (parts.isArray()) {
                    for (JsonNode part : parts) {
                        String content = part.path("content").asText();
                        if (!content.isBlank()) sb.append("• ").append(content).append("\n");
                    }
                }
                sb.append("\n");
            }
        }

        return sb.toString().trim().isEmpty() ? null : sb.toString().trim();
    }

    /**
     * 一站式：给定 bvid，自动获取最佳内容
     * 优先级：AI总结 > CC字幕 > 视频简介
     */
    public CrawlResult crawl(String bvid) throws Exception {
        Map<String, Object> info = fetchVideoInfo(bvid);

        String title = (String) info.get("title");
        String description = (String) info.get("description");
        String ownerName = (String) info.get("owner_name");
        String picUrl = (String) info.get("pic_url");
        int duration = (int) info.getOrDefault("duration", 0);
        long cid = info.get("cid") != null ? ((Number) info.get("cid")).longValue() : 0;
        long ownerMid = info.get("owner_mid") != null ? ((Number) info.get("owner_mid")).longValue() : 0;

        String content = null;
        String contentSource = "description";

        // 1. 尝试获取 AI 总结
        if (cid > 0 && ownerMid > 0) {
            try {
                String aiSummary = fetchAiSummary(bvid, cid, ownerMid);
                if (aiSummary != null && aiSummary.length() > 50) {
                    content = aiSummary;
                    contentSource = "ai_summary";
                }
            } catch (Exception ignored) {}
        }

        // 2. 尝试获取 CC 字幕
        if (content == null && cid > 0) {
            try {
                String subtitle = fetchSubtitle(bvid, cid);
                if (subtitle != null && subtitle.length() > 100) {
                    content = subtitle;
                    contentSource = "subtitle";
                }
            } catch (Exception ignored) {}
        }

        // 3. 降级到视频简介
        if (content == null || content.isBlank()) {
            content = description;
            contentSource = "description";
        }

        // 拼接标题到内容开头，增强检索效果
        String fullContent = title + "\n" + (content != null ? content : "");

        CrawlResult result = new CrawlResult();
        result.bvid = bvid;
        result.title = title;
        result.description = description;
        result.ownerName = ownerName;
        result.picUrl = picUrl;
        result.duration = duration;
        result.content = fullContent;
        result.contentSource = contentSource;
        return result;
    }

    /**
     * 解析 B站字幕 JSON 文件，提取纯文本
     */
    private String parseSubtitleJson(String url) throws Exception {
        String json = HttpUtils.get(url);
        JsonNode root = objectMapper.readTree(json);
        JsonNode body = root.path("body");
        if (!body.isArray()) return null;

        StringBuilder sb = new StringBuilder();
        for (JsonNode item : body) {
            String content = item.path("content").asText();
            if (!content.isBlank()) sb.append(content).append(" ");
        }
        return sb.toString().trim();
    }

    public static class CrawlResult {
        public String bvid;
        public String title;
        public String description;
        public String ownerName;
        public String picUrl;
        public int duration;
        public String content;
        public String contentSource; // subtitle / ai_summary / description
    }
}

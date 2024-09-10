package com.bilibili.rag.controller;

import com.bilibili.rag.model.VideoCache;
import com.bilibili.rag.service.BilibiliCrawlService;
import com.bilibili.rag.service.VideoProcessService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/videos")
public class VideoUploadController {

    private static final Logger log = LoggerFactory.getLogger(VideoUploadController.class);

    private final VideoProcessService videoProcessService;
    private final BilibiliCrawlService crawlService;

    @Autowired
    public VideoUploadController(VideoProcessService videoProcessService,
                                  BilibiliCrawlService crawlService) {
        this.videoProcessService = videoProcessService;
        this.crawlService = crawlService;
    }

    // ─────────────────────────────────────────────
    // 1. 通过 B站链接 / BV号 自动爬取
    // POST /videos/crawl
    // Body: { "url": "https://www.bilibili.com/video/BVxxx" }
    //    or { "url": "BV1xx411c7mD" }
    // ─────────────────────────────────────────────
    @PostMapping("/crawl")
    public Map<String, Object> crawlFromUrl(@RequestBody Map<String, Object> body) {
        String url = (String) body.getOrDefault("url", "");
        if (url.isBlank()) {
            return Map.of("success", false, "message", "请输入 B站视频链接或 BV 号");
        }

        String bvid = crawlService.extractBvid(url);
        if (bvid == null) {
            return Map.of("success", false, "message", "无法识别 BV 号，请输入正确的 B站视频链接或 BV 号（如 BV1xx411c7mD）");
        }

        log.info("开始爬取 B站视频: {}", bvid);
        try {
            BilibiliCrawlService.CrawlResult result = crawlService.crawl(bvid);

            if (result.content == null || result.content.isBlank()) {
                return Map.of("success", false, "message", "该视频没有可提取的内容（无字幕、无简介）");
            }

            VideoCache video = videoProcessService.saveVideoWithContent(
                    result.bvid, result.title, result.description,
                    result.ownerName, result.content, result.contentSource,
                    result.picUrl, result.duration);

            videoProcessService.processAndIndex(video.getId());

            log.info("视频 {} 已入库，内容来源: {}，内容长度: {}", bvid, result.contentSource, result.content.length());

            return Map.of(
                    "success", true,
                    "message", "爬取成功，正在向量化入库",
                    "bvid", result.bvid,
                    "title", result.title,
                    "content_source", result.contentSource,
                    "content_length", result.content.length()
            );
        } catch (Exception e) {
            log.error("爬取视频 {} 失败: {}", bvid, e.getMessage(), e);
            return Map.of("success", false, "message", "爬取失败：" + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // 2. 手动填写内容入库
    // POST /videos/add
    // ─────────────────────────────────────────────
    @PostMapping("/add")
    public Map<String, Object> addVideo(@RequestBody Map<String, Object> body) {
        String bvid        = (String) body.getOrDefault("bvid", "");
        String title       = (String) body.getOrDefault("title", "");
        String content     = (String) body.getOrDefault("content", "");
        String description = (String) body.getOrDefault("description", "");
        String ownerName   = (String) body.getOrDefault("owner_name", "");
        String picUrl      = (String) body.getOrDefault("pic_url", "");
        Integer duration   = body.get("duration") instanceof Number
                ? ((Number) body.get("duration")).intValue() : 0;

        if (title.isBlank()) return Map.of("success", false, "message", "标题不能为空");
        if (content.isBlank()) return Map.of("success", false, "message", "内容不能为空");

        if (bvid.isBlank()) {
            bvid = "LOCAL_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        }

        log.info("手动添加视频: title={}, bvid={}, contentLen={}", title, bvid, content.length());
        try {
            VideoCache video = videoProcessService.saveVideoWithContent(
                    bvid, title, description, ownerName, content, "manual", picUrl, duration);
            videoProcessService.processAndIndex(video.getId());

            return Map.of("success", true, "message", "已提交，正在向量化入库", "bvid", video.getBvid());
        } catch (Exception e) {
            log.error("手动添加视频失败: {}", e.getMessage(), e);
            return Map.of("success", false, "message", "添加失败：" + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // 3. 上传字幕/文本文件
    // POST /videos/upload  (multipart)
    // ─────────────────────────────────────────────
    @PostMapping("/upload")
    public Map<String, Object> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "bvid", required = false, defaultValue = "") String bvid,
            @RequestParam(value = "description", required = false, defaultValue = "") String description,
            @RequestParam(value = "owner_name", required = false, defaultValue = "") String ownerName) {

        if (file.isEmpty()) return Map.of("success", false, "message", "文件不能为空");
        if (title.isBlank()) return Map.of("success", false, "message", "标题不能为空");

        String filename = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase();
        if (!filename.endsWith(".srt") && !filename.endsWith(".txt") && !filename.endsWith(".vtt")) {
            return Map.of("success", false, "message", "仅支持 .srt / .vtt / .txt 格式");
        }

        try {
            String rawContent = new String(file.getBytes(), StandardCharsets.UTF_8);
            String content = filename.endsWith(".srt") ? parseSrt(rawContent)
                    : filename.endsWith(".vtt") ? parseVtt(rawContent)
                    : rawContent;

            if (content.isBlank()) {
                return Map.of("success", false, "message", "文件内容为空，请检查文件");
            }

            if (bvid.isBlank()) {
                bvid = "LOCAL_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
            }

            log.info("上传字幕文件: title={}, file={}, contentLen={}", title, filename, content.length());

            VideoCache video = videoProcessService.saveVideoWithContent(
                    bvid, title, description, ownerName, content, "upload", "", 0);
            videoProcessService.processAndIndex(video.getId());

            return Map.of(
                    "success", true,
                    "message", "上传成功，正在向量化入库",
                    "bvid", video.getBvid(),
                    "content_length", content.length()
            );
        } catch (Exception e) {
            log.error("上传文件失败: {}", e.getMessage(), e);
            return Map.of("success", false, "message", "上传失败：" + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // 4. 获取视频列表
    // GET /videos/list
    // ─────────────────────────────────────────────
    @GetMapping("/list")
    public Map<String, Object> listVideos() {
        try {
            List<VideoCache> videos = videoProcessService.getAllVideos();
            List<Map<String, Object>> list = videos.stream().map(v -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", v.getId());
                m.put("bvid", v.getBvid());
                m.put("title", v.getTitle());
                m.put("description", v.getDescription());
                m.put("owner_name", v.getOwnerName());
                m.put("content_source", v.getContentSource());
                m.put("pic_url", v.getPicUrl());
                m.put("duration", v.getDuration());
                m.put("is_processed", v.isProcessed());
                m.put("process_error", v.getProcessError());
                m.put("created_at", v.getCreatedAt() != null ? v.getCreatedAt().toString() : null);
                String c = v.getContent();
                m.put("content_preview", c != null && c.length() > 150 ? c.substring(0, 150) + "..." : c);
                return m;
            }).collect(Collectors.toList());

            return Map.of("videos", list, "total", list.size());
        } catch (Exception e) {
            log.error("获取视频列表失败: {}", e.getMessage(), e);
            return Map.of("error", e.getMessage(), "videos", List.of(), "total", 0);
        }
    }

    // ─────────────────────────────────────────────
    // 5. 删除视频
    // DELETE /videos/{bvid}
    // ─────────────────────────────────────────────
    @DeleteMapping("/{bvid}")
    public Map<String, Object> deleteVideo(@PathVariable String bvid) {
        try {
            videoProcessService.deleteVideo(bvid);
            return Map.of("success", true, "message", "删除成功");
        } catch (Exception e) {
            log.error("删除视频 {} 失败: {}", bvid, e.getMessage(), e);
            return Map.of("success", false, "message", "删除失败：" + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // 6. 重新向量化
    // POST /videos/{bvid}/reindex
    // ─────────────────────────────────────────────
    @PostMapping("/{bvid}/reindex")
    public Map<String, Object> reindex(@PathVariable String bvid) {
        try {
            VideoCache video = videoProcessService.getByBvid(bvid);
            if (video == null) return Map.of("success", false, "message", "视频不存在");
            video.setProcessed(false);
            video.setProcessError(null);
            videoProcessService.processAndIndex(video.getId());
            return Map.of("success", true, "message", "已重新提交向量化");
        } catch (Exception e) {
            return Map.of("success", false, "message", "操作失败：" + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // 字幕解析工具
    // ─────────────────────────────────────────────

    private String parseSrt(String srt) {
        StringBuilder sb = new StringBuilder();
        for (String line : srt.split("\n")) {
            line = line.trim();
            if (line.isEmpty() || line.matches("\\d+") || line.contains("-->")) continue;
            sb.append(line.replaceAll("<[^>]+>", "")).append(" ");
        }
        return sb.toString().trim();
    }

    private String parseVtt(String vtt) {
        StringBuilder sb = new StringBuilder();
        boolean started = false;
        for (String line : vtt.split("\n")) {
            line = line.trim();
            if (!started) { if (line.startsWith("WEBVTT")) started = true; continue; }
            if (line.isEmpty() || line.contains("-->") || line.matches("\\d+")) continue;
            String clean = line.replaceAll("<[^>]+>", "").trim();
            if (!clean.isEmpty()) sb.append(clean).append(" ");
        }
        return sb.toString().trim();
    }
}

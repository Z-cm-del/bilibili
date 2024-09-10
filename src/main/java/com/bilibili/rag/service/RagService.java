package com.bilibili.rag.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import com.bilibili.rag.model.VideoCache;
import com.bilibili.rag.model.VideoEmbedding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG 核心服务：向量化、存储、检索、生成回答
 */
@Service
public class RagService {

    private static final String INDEX = "video_embeddings";
    private static final int EMBEDDING_DIM = 1536; // text-embedding-3-small

    private final EmbeddingService embeddingService;
    private final LlmService llmService;
    private final ElasticsearchClient esClient;

    @Autowired
    public RagService(EmbeddingService embeddingService,
                      LlmService llmService,
                      ElasticsearchClient esClient) {
        this.embeddingService = embeddingService;
        this.llmService = llmService;
        this.esClient = esClient;
    }

    /**
     * 将视频内容向量化并存入 ES
     */
    public void indexVideo(VideoCache video) throws Exception {
        String text = buildIndexText(video);
        List<Float> embedding = embeddingService.embed(text);

        Map<String, Object> doc = new HashMap<>();
        doc.put("bvid", video.getBvid());
        doc.put("title", video.getTitle());
        doc.put("description", video.getDescription());
        doc.put("owner_name", video.getOwnerName());
        doc.put("content", video.getContent());
        doc.put("pic_url", video.getPicUrl());
        doc.put("duration", video.getDuration());
        doc.put("embedding", embedding);

        esClient.index(i -> i
                .index(INDEX)
                .id(video.getBvid())
                .document(doc)
        );
    }

    /**
     * KNN 语义搜索，返回最相关的视频片段
     */
    public List<Map<String, Object>> search(String query, int k) throws Exception {
        List<Float> queryEmbedding = embeddingService.embed(query);
        // ES Java Client knn queryVector 需要 List<Float>，直接传入
        List<Float> vector = queryEmbedding;

        SearchResponse<Map> response = esClient.search(s -> s
                        .index(INDEX)
                        .knn(knn -> knn
                                .field("embedding")
                                .queryVector(vector)
                                .k(k)
                                .numCandidates(k * 5)
                        )
                        .size(k),
                Map.class
        );

        List<Map<String, Object>> results = new ArrayList<>();
        for (Hit<Map> hit : response.hits().hits()) {
            Map<String, Object> source = hit.source();
            if (source != null) {
                Map<String, Object> result = new HashMap<>(source);
                result.put("_score", hit.score());
                results.add(result);
            }
        }
        return results;
    }

    /**
     * RAG 问答：检索 + 生成（支持多轮对话历史）
     */
    public Map<String, Object> ask(String question, int k, List<Map<String, String>> history) throws Exception {
        List<Map<String, Object>> docs = search(question, k);

        if (docs.isEmpty()) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("answer", "暂时没有找到相关内容，请先在左侧知识库管理中上传视频字幕或笔记。");
            resp.put("sources", Collections.emptyList());
            return resp;
        }

        // 构建上下文
        StringBuilder context = new StringBuilder();
        List<Map<String, Object>> sources = new ArrayList<>();

        for (int i = 0; i < docs.size(); i++) {
            Map<String, Object> doc = docs.get(i);
            String bvid = (String) doc.get("bvid");
            String title = (String) doc.get("title");
            String content = (String) doc.get("content");

            context.append(String.format("[%d] 视频《%s》", i + 1, title));
            String ownerName = (String) doc.get("owner_name");
            if (ownerName != null && !ownerName.isBlank()) {
                context.append(String.format("（UP主：%s）", ownerName));
            }
            context.append("\n");
            if (content != null && !content.isBlank()) {
                int maxLen = Math.min(content.length(), 1500);
                context.append(content, 0, maxLen).append("\n\n");
            }

            Map<String, Object> source = new HashMap<>();
            source.put("bvid", bvid);
            source.put("title", title);
            source.put("url", bvid != null && bvid.startsWith("LOCAL_")
                    ? "#" : "https://www.bilibili.com/video/" + bvid);
            source.put("pic_url", doc.get("pic_url"));
            sources.add(source);
        }

        String systemPrompt = """
                你是 BiliMind，一个哔哩哔哩收藏夹知识助手。用户将学习视频的字幕或笔记存入了知识库，你需要基于这些内容回答问题。
                
                回答规范：
                1. 严格基于提供的视频内容回答，不要编造不存在的信息
                2. 如果内容中有相关知识点，请结构化、详细地解释（可用列表、代码块等）
                3. 在回答末尾用"参考来源：[序号]"标注引用了哪些视频
                4. 使用中文回答，语言专业但易懂
                5. 如果知识库内容不足以完整回答，请明确说明哪些部分无法从知识库中找到
                6. 如果用户问的是与视频内容无关的问题，礼貌地引导用户提问知识库相关内容
                """;

        String answer = llmService.chatWithHistory(systemPrompt, context.toString(), question, history);

        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer);
        result.put("sources", sources);
        return result;
    }

    /**
     * 兼容旧接口（无历史）
     */
    public Map<String, Object> ask(String question, int k) throws Exception {
        return ask(question, k, Collections.emptyList());
    }

    /**
     * 删除 ES 中的视频索引
     */
    public void deleteVideo(String bvid) throws Exception {
        esClient.delete(d -> d.index(INDEX).id(bvid));
    }

    /**
     * 确保 ES 索引存在（含 dense_vector mapping）
     */
    public void ensureIndex() {
        try {
            boolean exists = esClient.indices().exists(e -> e.index(INDEX)).value();
            if (!exists) {
                esClient.indices().create(c -> c
                        .index(INDEX)
                        .mappings(m -> m
                                .properties("bvid", p -> p.keyword(k -> k))
                                .properties("title", p -> p.text(t -> t))
                                .properties("description", p -> p.text(t -> t))
                                .properties("owner_name", p -> p.keyword(k -> k))
                                .properties("content", p -> p.text(t -> t))
                                .properties("pic_url", p -> p.keyword(k -> k))
                                .properties("duration", p -> p.integer(i -> i))
                                .properties("embedding", p -> p.denseVector(dv -> dv
                                        .dims(EMBEDDING_DIM)
                                        .index(true)
                                        .similarity("cosine")
                                ))
                        )
                );
            }
        } catch (Exception e) {
            // 索引已存在或 ES 未启动，忽略
        }
    }

    private String buildIndexText(VideoCache video) {
        StringBuilder sb = new StringBuilder();
        sb.append(video.getTitle()).append("\n");
        if (video.getDescription() != null) sb.append(video.getDescription()).append("\n");
        if (video.getContent() != null) sb.append(video.getContent());
        return sb.toString();
    }
}

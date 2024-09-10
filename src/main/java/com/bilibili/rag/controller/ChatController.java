package com.bilibili.rag.controller;

import com.bilibili.rag.service.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final RagService ragService;

    @Autowired
    public ChatController(RagService ragService) {
        this.ragService = ragService;
    }

    /**
     * RAG 问答（支持多轮对话历史）
     * POST /chat/ask
     * Body: {
     *   "question": "...",
     *   "k": 5,
     *   "history": [{"role":"user","content":"..."},{"role":"assistant","content":"..."}]
     * }
     */
    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestBody Map<String, Object> body) {
        String question = (String) body.getOrDefault("question", "");
        int k = body.get("k") instanceof Number ? ((Number) body.get("k")).intValue() : 5;

        @SuppressWarnings("unchecked")
        List<Map<String, String>> history = body.get("history") instanceof List
                ? (List<Map<String, String>>) body.get("history")
                : Collections.emptyList();

        if (question.isBlank()) {
            return Map.of("error", "问题不能为空", "answer", "", "sources", List.of());
        }

        try {
            return ragService.ask(question, k, history);
        } catch (Exception e) {
            return Map.of(
                    "error", e.getMessage(),
                    "answer", "抱歉，处理您的问题时出现了错误：" + e.getMessage(),
                    "sources", List.of()
            );
        }
    }

    /**
     * 纯语义搜索（不生成回答，用于搜索页）
     * GET /chat/search?query=xxx&k=5
     */
    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam String query,
                                       @RequestParam(defaultValue = "8") int k) {
        if (query == null || query.isBlank()) {
            return Map.of("results", List.of(), "total", 0);
        }
        try {
            List<Map<String, Object>> results = ragService.search(query, k);
            // 补充 B站链接
            results.forEach(r -> {
                String bvid = (String) r.get("bvid");
                if (bvid != null && !bvid.startsWith("LOCAL_")) {
                    r.put("url", "https://www.bilibili.com/video/" + bvid);
                } else {
                    r.put("url", "#");
                }
            });
            return Map.of("results", results, "total", results.size());
        } catch (Exception e) {
            return Map.of("error", e.getMessage(), "results", List.of(), "total", 0);
        }
    }
}

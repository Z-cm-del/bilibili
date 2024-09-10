package com.bilibili.rag.controller;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.bilibili.rag.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    private final ElasticsearchClient esClient;
    private final AppConfig appConfig;

    @Autowired
    public HealthController(ElasticsearchClient esClient, AppConfig appConfig) {
        this.esClient = esClient;
        this.appConfig = appConfig;
    }

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("app", "BiliMind");

        // 检查 ES 连通性
        try {
            boolean esOk = esClient.ping().value();
            result.put("elasticsearch", esOk ? "UP" : "DOWN");
        } catch (Exception e) {
            result.put("elasticsearch", "DOWN: " + e.getMessage());
        }

        // 检查 LLM 配置
        String apiKey = appConfig.getOpenaiApiKey();
        result.put("llm_configured", apiKey != null && !apiKey.isBlank() && !apiKey.startsWith("sk-your"));
        result.put("llm_model", appConfig.getLlmModel());
        result.put("embedding_model", appConfig.getEmbeddingModel());

        return result;
    }
}

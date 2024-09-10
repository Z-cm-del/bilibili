package com.bilibili.rag.service;

import com.bilibili.rag.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * 调用 OpenAI Embedding API，将文本转为向量
 */
@Service
public class EmbeddingService {

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Autowired
    public EmbeddingService(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * 将文本转为 float 向量
     */
    public List<Float> embed(String text) throws Exception {
        // 截断过长文本（embedding 模型有 token 限制）
        if (text.length() > 8000) {
            text = text.substring(0, 8000);
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", appConfig.getEmbeddingModel());
        body.put("input", text);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(appConfig.getOpenaiBaseUrl() + "/embeddings"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + appConfig.getOpenaiApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Embedding API error: " + response.statusCode() + " " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode embeddingArray = root.path("data").get(0).path("embedding");

        List<Float> embedding = new ArrayList<>();
        for (JsonNode val : embeddingArray) {
            embedding.add((float) val.asDouble());
        }
        return embedding;
    }
}

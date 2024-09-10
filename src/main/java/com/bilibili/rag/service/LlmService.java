package com.bilibili.rag.service;

import com.bilibili.rag.config.AppConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class LlmService {

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

    @Autowired
    public LlmService(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * 简单单轮对话
     */
    public String chat(String systemPrompt, String userMessage) throws Exception {
        return chatWithHistory(systemPrompt, null, userMessage, Collections.emptyList());
    }

    /**
     * 多轮对话，支持传入历史消息和检索到的上下文
     *
     * @param systemPrompt 系统提示词
     * @param context      RAG 检索到的上下文（可为 null）
     * @param question     当前用户问题
     * @param history      历史消息列表，每条格式 {"role": "user"/"assistant", "content": "..."}
     */
    public String chatWithHistory(String systemPrompt, String context,
                                   String question, List<Map<String, String>> history) throws Exception {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", appConfig.getLlmModel());
        body.put("temperature", 0.7);
        body.put("max_tokens", 2000);

        ArrayNode messages = body.putArray("messages");

        // 系统消息
        addMessage(messages, "system", systemPrompt);

        // 注入检索上下文（作为 system 补充，避免污染对话流）
        if (context != null && !context.isBlank()) {
            addMessage(messages, "system",
                    "以下是从知识库中检索到的相关视频内容，请基于此内容回答用户问题：\n\n" + context);
        }

        // 历史对话（最多保留最近 6 轮，避免 token 超限）
        int start = Math.max(0, history.size() - 12);
        for (int i = start; i < history.size(); i++) {
            Map<String, String> msg = history.get(i);
            addMessage(messages, msg.get("role"), msg.get("content"));
        }

        // 当前问题
        addMessage(messages, "user", question);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(appConfig.getOpenaiBaseUrl() + "/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + appConfig.getOpenaiApiKey())
                .timeout(Duration.ofSeconds(90))
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("LLM API error [" + response.statusCode() + "]: " + response.body());
        }

        JsonNode root = objectMapper.readTree(response.body());
        return root.path("choices").get(0).path("message").path("content").asText();
    }

    private void addMessage(ArrayNode messages, String role, String content) {
        ObjectNode msg = messages.addObject();
        msg.put("role", role);
        msg.put("content", content);
    }
}

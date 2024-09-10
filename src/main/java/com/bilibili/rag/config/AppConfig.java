package com.bilibili.rag.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.io.File;

@Configuration
@EnableAsync
public class AppConfig {

    // OpenAI / LLM 配置
    @Value("${openai.api.key:}")
    private String openaiApiKey;

    @Value("${openai.base.url:https://api.openai.com/v1}")
    private String openaiBaseUrl;

    @Value("${llm.model:gpt-4-turbo}")
    private String llmModel;

    @Value("${embedding.model:text-embedding-3-small}")
    private String embeddingModel;

    // DashScope ASR
    @Value("${dashscope.base.url:https://dashscope.aliyuncs.com/api/v1}")
    private String dashscopeBaseUrl;

    @Value("${asr.model:paraformer-v2}")
    private String asrModel;

    @Value("${asr.timeout:600}")
    private int asrTimeout;

    @Value("${asr.model.local:paraformer-realtime-v2}")
    private String asrModelLocal;

    @Value("${asr.input.format:pcm}")
    private String asrInputFormat;

    // 应用配置
    @Value("${app.host:0.0.0.0}")
    private String appHost;

    @Value("${app.port:8000}")
    private int appPort;

    @Value("${debug:true}")
    private boolean debug;

    // 数据库
    @Value("${database.url:sqlite:./data/bilibili_rag.db}")
    private String databaseUrl;

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUri;

    @Value("${spring.elasticsearch.username:}")
    private String elasticsearchUsername;

    @Value("${spring.elasticsearch.password:}")
    private String elasticsearchPassword;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        String host = elasticsearchUri.replace("http://", "").replace("https://", "");
        String[] parts = host.split(":");
        String hostname = parts[0];
        int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;

        RestClientBuilder builder = RestClient.builder(new HttpHost(hostname, port));

        // 如果配置了用户名密码，加上 Basic Auth
        if (elasticsearchUsername != null && !elasticsearchUsername.isBlank()) {
            BasicCredentialsProvider credsProv = new BasicCredentialsProvider();
            credsProv.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(elasticsearchUsername, elasticsearchPassword));
            builder.setHttpClientConfigCallback(
                    httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credsProv));
        }

        RestClient restClient = builder.build();
        ElasticsearchTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }

    public void ensureDirectories() {
        String[] dirs = {
            "data",
            "logs"
        };
        
        for (String dir : dirs) {
            File directory = new File(dir);
            if (!directory.exists()) {
                directory.mkdirs();
            }
        }
    }

    // Getters
    public String getOpenaiApiKey() {
        return openaiApiKey;
    }

    public String getOpenaiBaseUrl() {
        return openaiBaseUrl;
    }

    public String getLlmModel() {
        return llmModel;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public String getDashscopeBaseUrl() {
        return dashscopeBaseUrl;
    }

    public String getAsrModel() {
        return asrModel;
    }

    public int getAsrTimeout() {
        return asrTimeout;
    }

    public String getAsrModelLocal() {
        return asrModelLocal;
    }

    public String getAsrInputFormat() {
        return asrInputFormat;
    }

    public String getAppHost() {
        return appHost;
    }

    public int getAppPort() {
        return appPort;
    }

    public boolean isDebug() {
        return debug;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }



}
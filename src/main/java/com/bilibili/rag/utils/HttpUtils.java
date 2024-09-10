package com.bilibili.rag.utils;

import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpUtils {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36";

    public static String get(String url) throws IOException {
        return get(url, null);
    }

    public static String get(String url, Map<String, String> cookies) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            request.addHeader("User-Agent", USER_AGENT);
            if (cookies != null && !cookies.isEmpty()) {
                StringBuilder cookieStr = new StringBuilder();
                cookies.forEach((k, v) -> cookieStr.append(k).append("=").append(v).append("; "));
                request.addHeader("Cookie", cookieStr.toString().trim());
            }
            try (CloseableHttpResponse response = client.execute(request)) {
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static String post(String url, String json) throws IOException {
        return post(url, json, null);
    }

    public static String post(String url, String json, Map<String, String> cookies) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(url);
            request.addHeader("User-Agent", USER_AGENT);
            request.addHeader("Content-Type", "application/json");
            if (cookies != null && !cookies.isEmpty()) {
                StringBuilder cookieStr = new StringBuilder();
                cookies.forEach((k, v) -> cookieStr.append(k).append("=").append(v).append("; "));
                request.addHeader("Cookie", cookieStr.toString().trim());
            }
            request.setEntity(new StringEntity(json, StandardCharsets.UTF_8));
            try (CloseableHttpResponse response = client.execute(request)) {
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

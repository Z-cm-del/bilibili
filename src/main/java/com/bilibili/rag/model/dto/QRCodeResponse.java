package com.bilibili.rag.model.dto;

public class QRCodeResponse {
    private String qrcodeKey;
    private String qrcodeUrl;
    private String qrcodeImageBase64;

    // Getters and Setters
    public String getQrcodeKey() {
        return qrcodeKey;
    }

    public void setQrcodeKey(String qrcodeKey) {
        this.qrcodeKey = qrcodeKey;
    }

    public String getQrcodeUrl() {
        return qrcodeUrl;
    }

    public void setQrcodeUrl(String qrcodeUrl) {
        this.qrcodeUrl = qrcodeUrl;
    }

    public String getQrcodeImageBase64() {
        return qrcodeImageBase64;
    }

    public void setQrcodeImageBase64(String qrcodeImageBase64) {
        this.qrcodeImageBase64 = qrcodeImageBase64;
    }
}
package com.bilibili.rag.model;

import java.util.List;

/**
 * ES 文档结构（仅用于类型参考，实际通过 Map 写入 ES）
 */
public class VideoEmbedding {

    private String bvid;
    private String title;
    private String content;
    private String ownerName;
    private String picUrl;
    private Integer duration;
    private List<Float> embedding;

    public String getBvid() { return bvid; }
    public void setBvid(String bvid) { this.bvid = bvid; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }

    public String getPicUrl() { return picUrl; }
    public void setPicUrl(String picUrl) { this.picUrl = picUrl; }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public List<Float> getEmbedding() { return embedding; }
    public void setEmbedding(List<Float> embedding) { this.embedding = embedding; }
}

package com.bilibili.rag.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", unique = true, nullable = false, length = 64)
    private String sessionId;

    @Column(name = "bili_mid")
    private Integer biliMid;

    @Column(name = "bili_uname", length = 100)
    private String biliUname;

    @Column(name = "bili_face", length = 500)
    private String biliFace;

    @Column(name = "sessdata", columnDefinition = "TEXT")
    private String sessdata;

    @Column(name = "bili_jct", columnDefinition = "TEXT")
    private String biliJct;

    @Column(name = "dedeuserid", length = 50)
    private String dedeuserId;

    @Column(name = "is_valid", columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean isValid;

    @Column(name = "last_active_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime lastActiveAt;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getBiliMid() {
        return biliMid;
    }

    public void setBiliMid(Integer biliMid) {
        this.biliMid = biliMid;
    }

    public String getBiliUname() {
        return biliUname;
    }

    public void setBiliUname(String biliUname) {
        this.biliUname = biliUname;
    }

    public String getBiliFace() {
        return biliFace;
    }

    public void setBiliFace(String biliFace) {
        this.biliFace = biliFace;
    }

    public String getSessdata() {
        return sessdata;
    }

    public void setSessdata(String sessdata) {
        this.sessdata = sessdata;
    }

    public String getBiliJct() {
        return biliJct;
    }

    public void setBiliJct(String biliJct) {
        this.biliJct = biliJct;
    }

    public String getDedeuserId() {
        return dedeuserId;
    }

    public void setDedeuserId(String dedeuserId) {
        this.dedeuserId = dedeuserId;
    }

    public boolean isValid() {
        return isValid;
    }

    public void setValid(boolean valid) {
        isValid = valid;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
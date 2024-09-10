package com.bilibili.rag.service;

import com.bilibili.rag.model.VideoCache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 独立的异步索引 Bean，避免 @Async + @Transactional 在同一 Bean 内的代理失效问题
 */
@Service
public class VideoIndexAsyncService {

    @PersistenceContext
    private EntityManager entityManager;

    private final RagService ragService;

    @Autowired
    public VideoIndexAsyncService(RagService ragService) {
        this.ragService = ragService;
    }

    @Async
    @Transactional
    public void indexAsync(Long videoCacheId) {
        VideoCache video = entityManager.find(VideoCache.class, videoCacheId);
        if (video == null) return;

        try {
            ragService.indexVideo(video);
            video.setProcessed(true);
            video.setProcessError(null);
            video.setUpdatedAt(LocalDateTime.now());
            entityManager.merge(video);
        } catch (Exception e) {
            video.setProcessed(false);
            video.setProcessError(e.getMessage());
            video.setUpdatedAt(LocalDateTime.now());
            entityManager.merge(video);
        }
    }
}

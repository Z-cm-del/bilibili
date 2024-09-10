package com.bilibili.rag.service;

import com.bilibili.rag.model.VideoCache;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VideoProcessService {

    @PersistenceContext
    private EntityManager entityManager;

    private final RagService ragService;
    private final VideoIndexAsyncService videoIndexAsyncService;

    @Autowired
    public VideoProcessService(RagService ragService, VideoIndexAsyncService videoIndexAsyncService) {
        this.ragService = ragService;
        this.videoIndexAsyncService = videoIndexAsyncService;
    }

    @Transactional
    public VideoCache saveVideoWithContent(String bvid, String title, String description,
                                            String ownerName, String content,
                                            String contentSource, String picUrl,
                                            Integer duration) {
        List<VideoCache> existing = entityManager.createQuery(
                "SELECT v FROM VideoCache v WHERE v.bvid = :bvid", VideoCache.class)
                .setParameter("bvid", bvid)
                .getResultList();

        VideoCache video;
        if (!existing.isEmpty()) {
            video = existing.get(0);
        } else {
            video = new VideoCache();
            video.setBvid(bvid);
            video.setCreatedAt(LocalDateTime.now());
        }

        video.setTitle(title);
        video.setDescription(description);
        video.setOwnerName(ownerName);
        video.setContent(content);
        video.setContentSource(contentSource);
        video.setPicUrl(picUrl);
        video.setDuration(duration);
        video.setProcessed(false);
        video.setProcessError(null);
        video.setUpdatedAt(LocalDateTime.now());

        if (existing.isEmpty()) {
            entityManager.persist(video);
        } else {
            video = entityManager.merge(video);
        }
        entityManager.flush(); // 确保 ID 已生成再返回
        return video;
    }

    /**
     * 提交异步向量化（通过独立 Bean 调用，保证 @Async 代理生效）
     */
    public void processAndIndex(Long videoCacheId) {
        videoIndexAsyncService.indexAsync(videoCacheId);
    }

    public List<VideoCache> getAllVideos() {
        return entityManager.createQuery(
                "SELECT v FROM VideoCache v ORDER BY v.createdAt DESC", VideoCache.class)
                .getResultList();
    }

    @Transactional
    public void deleteVideo(String bvid) throws Exception {
        List<VideoCache> list = entityManager.createQuery(
                "SELECT v FROM VideoCache v WHERE v.bvid = :bvid", VideoCache.class)
                .setParameter("bvid", bvid)
                .getResultList();
        if (!list.isEmpty()) {
            entityManager.remove(list.get(0));
        }
        try {
            ragService.deleteVideo(bvid);
        } catch (Exception ignored) {}
    }

    @Transactional
    public VideoCache getByBvid(String bvid) {
        List<VideoCache> list = entityManager.createQuery(
                "SELECT v FROM VideoCache v WHERE v.bvid = :bvid", VideoCache.class)
                .setParameter("bvid", bvid)
                .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }
}

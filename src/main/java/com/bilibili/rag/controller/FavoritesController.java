package com.bilibili.rag.controller;

import com.bilibili.rag.model.FavoriteFolder;
import com.bilibili.rag.model.FavoriteVideo;
import com.bilibili.rag.service.BilibiliService;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/favorites")
public class FavoritesController {

    @PersistenceContext
    private EntityManager entityManager;

    @GetMapping("/list/{sessionId}")
    public List<Map<String, Object>> getFavorites(@PathVariable String sessionId) throws Exception {
        // 从数据库查询收藏夹
        List<FavoriteFolder> folders = entityManager.createQuery(
            "SELECT ff FROM FavoriteFolder ff WHERE ff.sessionId = :sessionId",
            FavoriteFolder.class
        ).setParameter("sessionId", sessionId).getResultList();

        return folders.stream().map(folder -> {
            java.util.Map<String, Object> map = new java.util.HashMap<>();
            map.put("media_id", folder.getMediaId());
            map.put("title", folder.getTitle());
            map.put("media_count", folder.getMediaCount());
            map.put("is_selected", folder.isSelected());
            return map;
        }).collect(Collectors.toList());
    }

    @PostMapping("/sync/{sessionId}")
    @Transactional
    public Map<String, Object> syncFavorites(@PathVariable String sessionId, @RequestParam String sessdata, @RequestParam String biliJct, @RequestParam String dedeuserId) throws Exception {
        BilibiliService bili = new BilibiliService(sessdata, biliJct, dedeuserId);
        try {
            Map<String, Object> result = bili.getFavorites();
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            List<Map<String, Object>> folders = (List<Map<String, Object>>) data.get("list");

            // 保存到数据库
            for (Map<String, Object> folderData : folders) {
                Integer mediaId = (Integer) folderData.get("id");
                String title = (String) folderData.get("title");
                Integer mediaCount = (Integer) folderData.get("media_count");

                // 检查是否已存在
                FavoriteFolder existingFolder = entityManager.createQuery(
                    "SELECT ff FROM FavoriteFolder ff WHERE ff.sessionId = :sessionId AND ff.mediaId = :mediaId",
                    FavoriteFolder.class
                ).setParameter("sessionId", sessionId)
                 .setParameter("mediaId", mediaId)
                 .getResultList().stream().findFirst().orElse(null);

                if (existingFolder != null) {
                    // 更新
                    existingFolder.setTitle(title);
                    existingFolder.setMediaCount(mediaCount);
                    existingFolder.setLastSyncAt(LocalDateTime.now());
                    entityManager.merge(existingFolder);
                } else {
                    // 新建
                    FavoriteFolder folder = new FavoriteFolder();
                    folder.setSessionId(sessionId);
                    folder.setMediaId(mediaId);
                    folder.setTitle(title);
                    folder.setMediaCount(mediaCount);
                    folder.setSelected(true);
                    folder.setLastSyncAt(LocalDateTime.now());
                    entityManager.persist(folder);
                }
            }

            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("message", "同步成功");
            response.put("count", folders.size());
            return response;
        } finally {
            bili.close();
        }
    }

    @PostMapping("/select/{folderId}")
    @Transactional
    public Map<String, Object> selectFolder(@PathVariable Long folderId, @RequestParam boolean selected) {
        FavoriteFolder folder = entityManager.find(FavoriteFolder.class, folderId);
        if (folder != null) {
            folder.setSelected(selected);
            entityManager.merge(folder);
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("message", "操作成功");
            return result;
        } else {
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("message", "收藏夹不存在");
            return result;
        }
    }

}
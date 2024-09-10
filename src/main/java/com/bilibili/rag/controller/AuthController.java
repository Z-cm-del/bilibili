package com.bilibili.rag.controller;

import com.bilibili.rag.model.UserSession;
import com.bilibili.rag.model.dto.QRCodeResponse;
import com.bilibili.rag.model.dto.LoginStatusResponse;
import com.bilibili.rag.service.BilibiliService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @PersistenceContext
    private EntityManager entityManager;

    // 临时存储登录会话
    private final Map<String, Map<String, Object>> loginSessions = new ConcurrentHashMap<>();

    @GetMapping("/qrcode")
    public QRCodeResponse generateQrcode() throws Exception {
        BilibiliService bili = new BilibiliService();
        try {
            Map<String, Object> result = bili.generateQrcode();
            
            // 存储会话
            java.util.Map<String, Object> sessionInfo = new java.util.HashMap<>();
            sessionInfo.put("status", "waiting");
            loginSessions.put((String) result.get("qrcode_key"), sessionInfo);
            
            QRCodeResponse response = new QRCodeResponse();
            response.setQrcodeKey((String) result.get("qrcode_key"));
            response.setQrcodeUrl((String) result.get("qrcode_url"));
            response.setQrcodeImageBase64((String) result.get("qrcode_image_base64"));
            
            return response;
        } finally {
            bili.close();
        }
    }

    @GetMapping("/qrcode/poll/{qrcodeKey}")
    @Transactional
    public LoginStatusResponse pollQrcodeStatus(@PathVariable String qrcodeKey) throws Exception {
        BilibiliService bili = new BilibiliService();
        try {
            Map<String, Object> result = bili.pollQrcodeStatus(qrcodeKey);
            
            LoginStatusResponse response = new LoginStatusResponse();
            response.setStatus((String) result.get("status"));
            response.setMessage((String) result.get("message"));
            
            // 登录成功
            if ("Confirmed".equals(result.get("status"))) {
                Map<String, String> cookies = (Map<String, String>) result.get("cookies");
                
                // 创建会话
                String sessionId = UUID.randomUUID().toString();
                
                // 获取用户信息
                BilibiliService biliAuth = new BilibiliService(
                    cookies.get("SESSDATA"),
                    cookies.get("bili_jct"),
                    cookies.get("DedeUserID")
                );
                
                Map<String, Object> userInfoDict = new ConcurrentHashMap<>();
                try {
                    Map<String, Object> userInfo = biliAuth.getUserInfo();
                    int mid = (int) userInfo.get("mid");
                    
                    userInfoDict.put("mid", mid);
                    userInfoDict.put("uname", userInfo.get("uname"));
                    userInfoDict.put("face", userInfo.get("face"));
                    userInfoDict.put("level", ((Map<String, Object>) userInfo.get("level_info")).get("current_level"));
                    
                    // 持久化到数据库
                    UserSession userSession = new UserSession();
                    userSession.setSessionId(sessionId);
                    userSession.setBiliMid(mid);
                    userSession.setBiliUname((String) userInfo.get("uname"));
                    userSession.setBiliFace((String) userInfo.get("face"));
                    userSession.setSessdata(cookies.get("SESSDATA"));
                    userSession.setBiliJct(cookies.get("bili_jct"));
                    userSession.setDedeuserId(cookies.get("DedeUserID"));
                    userSession.setValid(true);
                    
                    entityManager.persist(userSession);
                    
                    response.setUserInfo(userInfoDict);
                } catch (Exception e) {
                    e.printStackTrace();
                    userInfoDict.put("mid", cookies.get("DedeUserID"));
                    userInfoDict.put("uname", "未知用户");
                    response.setUserInfo(userInfoDict);
                } finally {
                    biliAuth.close();
                }
                
                // 内存缓存
                java.util.Map<String, Object> sessionInfo = new java.util.HashMap<>();
                sessionInfo.put("cookies", cookies);
                sessionInfo.put("user_info", userInfoDict);
                loginSessions.put(sessionId, sessionInfo);
                
                response.setSessionId(sessionId);
                
                // 清理旧的 qrcode_key
                loginSessions.remove(qrcodeKey);
            }
            
            return response;
        } finally {
            bili.close();
        }
    }

    @GetMapping("/session/{sessionId}")
    @Transactional
    public Map<String, Object> getSessionInfo(@PathVariable String sessionId) {
        Map<String, Object> session = loginSessions.get(sessionId);
        if (session != null) {
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("valid", true);
            response.put("user_info", session.get("user_info"));
            return response;
        }
        
        // 从数据库查询
        UserSession userSession = entityManager.find(UserSession.class, sessionId);
        if (userSession != null && userSession.isValid()) {
            java.util.Map<String, Object> userInfo = new java.util.HashMap<>();
            userInfo.put("mid", userSession.getBiliMid());
            userInfo.put("uname", userSession.getBiliUname());
            userInfo.put("face", userSession.getBiliFace());
            
            java.util.Map<String, Object> cookiesMap = new java.util.HashMap<>();
            cookiesMap.put("SESSDATA", userSession.getSessdata());
            cookiesMap.put("bili_jct", userSession.getBiliJct());
            cookiesMap.put("DedeUserID", userSession.getDedeuserId());
            
            java.util.Map<String, Object> sessionInfo = new java.util.HashMap<>();
            sessionInfo.put("cookies", cookiesMap);
            sessionInfo.put("user_info", userInfo);
            
            loginSessions.put(sessionId, sessionInfo);
            
            java.util.Map<String, Object> response = new java.util.HashMap<>();
            response.put("valid", true);
            response.put("user_info", userInfo);
            return response;
        }
        
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("valid", false);
        response.put("message", "会话不存在或已过期");
        return response;
    }

    @DeleteMapping("/session/{sessionId}")
    public Map<String, Object> logout(@PathVariable String sessionId) {
        loginSessions.remove(sessionId);
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        response.put("message", "已退出登录");
        return response;
    }

}
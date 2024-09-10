# BiliMind — 哔哩哔哩收藏夹知识库助手

> 将 B 站收藏视频转化为可对话的个人知识库，基于 RAG（检索增强生成）架构。

## 项目亮点

- **RAG 全链路**：文本向量化 → Elasticsearch KNN 语义检索 → LLM 生成回答，完整实现检索增强生成
- **双入口**：支持 B 站扫码登录同步收藏夹，也支持直接上传字幕/文本文件（.srt / .vtt / .txt）
- **前后端分离**：Vue 3 + TypeScript 前端，Spring Boot 3 后端，RESTful API 设计
- **向量数据库**：Elasticsearch 8.x dense_vector + KNN 索引，支持余弦相似度语义搜索
- **本地持久化**：SQLite 存储视频元数据和用户会话，零运维成本

## 技术栈

| 层次 | 技术 |
|------|------|
| 前端 | Vue 3 + TypeScript + Vite + Axios + Marked |
| 后端 | Java 17 + Spring Boot 3.2 + Spring Security |
| ORM | Spring Data JPA + Hibernate + SQLite |
| 向量库 | Elasticsearch 8.x（KNN dense_vector） |
| LLM | OpenAI API（gpt-4o-mini / 兼容 DeepSeek 等） |
| Embedding | text-embedding-3-small（1536 维） |

## 架构图

```
用户 → Vue3 前端
         ↓
    Spring Boot API
    ├── /auth      扫码登录 / 会话管理
    ├── /favorites 收藏夹同步
    ├── /videos    视频上传 / 管理 / 向量化
    └── /chat      RAG 问答 / 语义搜索
         ↓
    ┌─────────────────────────────┐
    │  EmbeddingService           │  → OpenAI Embedding API
    │  RagService (KNN Search)    │  → Elasticsearch 8.x
    │  LlmService                 │  → OpenAI Chat API
    └─────────────────────────────┘
         ↓
    SQLite (元数据)  +  Elasticsearch (向量索引)
```

## 快速启动

### 前置依赖

- Java 17+
- Node.js 18+
- Elasticsearch 8.x（本地或 Docker）
- OpenAI API Key（或兼容接口）

### 启动 Elasticsearch

```bash
docker run -d --name es8 \
  -e "discovery.type=single-node" \
  -e "xpack.security.enabled=false" \
  -p 9200:9200 \
  elasticsearch:8.13.0
```

### 配置

编辑 `src/main/resources/application.properties`：

```properties
openai.api.key=sk-your-key-here
openai.base.url=https://api.openai.com/v1   # 可替换为 DeepSeek 等兼容接口
llm.model=gpt-4o-mini
embedding.model=text-embedding-3-small
```

### 启动后端

```bash
mvn spring-boot:run
# 服务运行在 http://localhost:8000
```

### 启动前端

```bash
cd frontend
npm install
npm run dev
# 访问 http://localhost:3000
```

## 核心功能

### 1. 视频内容入库

- **上传字幕文件**：支持 `.srt` / `.vtt` / `.txt`，自动解析纯文本
- **手动填写**：直接粘贴视频笔记或内容摘要
- **B 站同步**：扫码登录后同步收藏夹（需配置 B 站 API）
- 入库后异步向量化，失败可一键重试

### 2. 语义搜索

`GET /chat/search?query=Vue响应式原理&k=5`

基于 Elasticsearch KNN 向量搜索，返回语义最相关的视频片段。

### 3. RAG 问答

`POST /chat/ask` `{ "question": "Vue3 和 Vue2 的核心区别是什么？" }`

1. 将问题向量化
2. KNN 检索 Top-K 相关视频内容
3. 构建 Prompt + 上下文
4. 调用 LLM 生成带引用来源的回答

## API 文档

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /auth/qrcode | 获取 B 站登录二维码 |
| GET | /auth/qrcode/poll/{key} | 轮询登录状态 |
| GET | /favorites/list/{sessionId} | 获取收藏夹列表 |
| GET | /videos/list | 获取已入库视频 |
| POST | /videos/add | 手动添加视频内容 |
| POST | /videos/upload | 上传字幕/文本文件 |
| DELETE | /videos/{bvid} | 删除视频 |
| POST | /videos/{bvid}/reindex | 重新向量化 |
| POST | /chat/ask | RAG 问答 |
| GET | /chat/search | 语义搜索 |

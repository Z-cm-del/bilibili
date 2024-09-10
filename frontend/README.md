# Bilibili RAG 知识库 - 前端

基于 Vue 3 + TypeScript + Vite 构建的 Bilibili RAG 知识库前端应用。

## 功能特性

- 🔐 **B站扫码登录** - 安全便捷
- 📁 **收藏夹管理** - 查看和选择收藏夹
- 💬 **智能问答** - 基于收藏内容回答问题
- 🔍 **语义搜索** - 快速找到相关视频

## 技术栈

- Vue 3
- TypeScript
- Vite
- Axios
- Marked (Markdown渲染)

## 快速开始

### 环境要求

- Node.js 16+  
- npm 7+

### 安装依赖

```bash
npm install
```

### 配置

在 `.env` 文件中配置 API 地址（可选）：

```env
VITE_API_URL=http://localhost:8000
```

### 运行项目

```bash
# 开发模式
npm run dev

# 构建生产版本
npm run build

# 预览生产构建
npm run preview
```

## 项目结构

```
frontend/
├── public/              # 静态资源
├── src/
│   ├── components/      # 组件
│   │   ├── LoginModal.vue       # 登录模态框
│   │   ├── SourcesPanel.vue     # 收藏夹管理
│   │   └── ChatPanel.vue        # 对话面板
│   ├── services/        # 服务
│   │   └── api.ts       # API服务
│   ├── views/           # 页面
│   │   └── Home.vue     # 主页面
│   ├── router/          # 路由
│   │   └── index.ts     # 路由配置
│   ├── App.vue          # 根组件
│   └── main.ts          # 入口文件
├── .env                 # 环境变量
├── index.html           # HTML入口
├── package.json         # 项目配置
├── tsconfig.json        # TypeScript配置
└── vite.config.ts       # Vite配置
```

## API 接口

### 认证相关

- `GET /auth/qrcode` - 生成登录二维码
- `GET /auth/qrcode/poll/{qrcodeKey}` - 轮询登录状态
- `GET /auth/session/{sessionId}` - 获取会话信息
- `DELETE /auth/session/{sessionId}` - 退出登录

### 收藏夹相关

- `GET /favorites/list/{sessionId}` - 获取收藏夹列表
- `POST /favorites/sync/{sessionId}` - 同步收藏夹
- `POST /favorites/select/{folderId}` - 选择/取消选择收藏夹

### 聊天相关

- `POST /chat/ask` - 智能问答
- `POST /chat/search` - 搜索相关视频

## 注意事项

1. 前端默认使用 `/api` 代理到 `http://localhost:8000`，请确保后端服务在该地址运行
2. 首次登录需要使用 B 站 APP 扫描二维码
3. 登录成功后，会话信息会存储在本地存储中
4. 选择收藏夹后点击「入库」按钮，将收藏夹内容添加到知识库
5. 入库完成后，可以在对话面板中进行智能问答

## 许可证

MIT License
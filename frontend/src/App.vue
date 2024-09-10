<template>
  <div class="app">
    <router-view />
  </div>
</template>

<style>
:root {
  --primary: #00a1d6;
  --secondary: #f0f0f0;
  --text: #333;
  --text-light: #666;
  --bg: #fff;
  --border: #e0e0e0;
  --success: #52c41a;
  --warning: #faad14;
  --error: #f5222d;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
  font-size: 14px;
  line-height: 1.5;
  color: var(--text);
  background-color: var(--bg);
}

.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 16px;
  border: 1px solid var(--border);
  border-radius: 4px;
  background-color: var(--bg);
  color: var(--text);
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
}

.btn:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.btn-primary {
  background-color: var(--primary);
  border-color: var(--primary);
  color: white;
}

.btn-primary:hover {
  background-color: #0086b3;
  border-color: #0086b3;
  color: white;
}

.btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.input {
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 4px;
  font-size: 14px;
  width: 100%;
  transition: border-color 0.2s;
}

.input:focus {
  outline: none;
  border-color: var(--primary);
}

.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-card {
  background-color: white;
  border-radius: 8px;
  padding: 24px;
  width: 90%;
  max-width: 400px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--text);
}

.modal-subtitle {
  font-size: 14px;
  color: var(--text-light);
  margin-bottom: 24px;
}

.status-pill {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status-pill.ok {
  background-color: #f6ffed;
  color: var(--success);
}

.status-pill.partial {
  background-color: #fffbe6;
  color: var(--warning);
}

.status-pill.empty {
  background-color: #fff2f0;
  color: var(--error);
}

.panel {
  border: 1px solid var(--border);
  border-radius: 8px;
  overflow: hidden;
}

.panel-header {
  padding: 16px;
  border-bottom: 1px solid var(--border);
  background-color: #fafafa;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text);
}

.panel-subtitle {
  font-size: 12px;
  color: var(--text-light);
  margin-top: 4px;
}

.panel-body {
  padding: 16px;
  min-height: 300px;
}

.panel-footer {
  padding: 16px;
  border-top: 1px solid var(--border);
  background-color: #fafafa;
}

.folder-card {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 12px;
  transition: all 0.2s;
}

.folder-card:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.folder-card.selected {
  border-color: var(--primary);
  background-color: #e6f7ff;
}

.folder-head {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.folder-title {
  font-weight: 500;
  margin-bottom: 4px;
}

.folder-count {
  font-size: 12px;
  color: var(--text-light);
}

.folder-toggle {
  margin-left: auto;
  transition: transform 0.2s;
}

.folder-toggle.rotated {
  transform: rotate(90deg);
}

.folder-list {
  margin-top: 12px;
  padding-left: 24px;
}

.video-item {
  display: flex;
  align-items: center;
  padding: 8px 0;
  font-size: 14px;
}

.video-item span:first-child {
  margin-right: 8px;
  color: var(--primary);
}

.chat-window {
  min-height: 400px;
  max-height: 600px;
  overflow-y: auto;
  margin-bottom: 16px;
}

.message {
  margin-bottom: 16px;
  display: flex;
}

.message.user {
  justify-content: flex-end;
}

.message.assistant {
  justify-content: flex-start;
}

.message-bubble {
  max-width: 70%;
  padding: 12px;
  border-radius: 8px;
  line-height: 1.5;
}

.message.user .message-bubble {
  background-color: var(--primary);
  color: white;
  border-bottom-right-radius: 2px;
}

.message.assistant .message-bubble {
  background-color: var(--secondary);
  color: var(--text);
  border-bottom-left-radius: 2px;
}

.source-list {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
}

.source-link {
  display: block;
  font-size: 12px;
  color: var(--primary);
  text-decoration: none;
  margin-bottom: 4px;
}

.source-link:hover {
  text-decoration: underline;
}

.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid var(--border);
  border-top: 2px solid var(--primary);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.hero {
  text-align: center;
  padding: 64px 24px;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  min-height: 80vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.hero-title {
  font-size: 36px;
  font-weight: 700;
  margin-bottom: 16px;
  color: var(--text);
}

.hero-desc {
  font-size: 18px;
  color: var(--text-light);
  margin-bottom: 32px;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}

.hero-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-bottom: 48px;
}

.btn-lg {
  padding: 12px 24px;
  font-size: 16px;
}

.pipeline-row {
  display: flex;
  justify-content: center;
  gap: 32px;
  flex-wrap: wrap;
}

.pipeline-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  min-width: 200px;
}

.pipeline-icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background-color: var(--primary);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.pipeline-text {
  text-align: left;
}

.pipeline-text strong {
  display: block;
  margin-bottom: 4px;
  color: var(--text);
}

.pipeline-text span {
  font-size: 12px;
  color: var(--text-light);
}

.app-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background-color: white;
  border-bottom: 1px solid var(--border);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--text);
}

.brand-subtitle {
  font-size: 12px;
  color: var(--text-light);
  margin-top: 2px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background-color: var(--secondary);
  border-radius: 16px;
  font-size: 14px;
}

.user-chip strong {
  color: var(--primary);
}

.workspace {
  display: flex;
  flex: 1;
  padding: 24px;
  gap: 24px;
  min-height: 80vh;
}

.panel-sources {
  width: 320px;
  flex-shrink: 0;
}

.panel-chat {
  flex: 1;
}

.sources-scroll {
  max-height: 600px;
  overflow-y: auto;
}

.chat-scroll {
  max-height: 500px;
  overflow-y: auto;
  margin-bottom: 16px;
}

.empty-state {
  text-align: center;
  padding: 48px 24px;
  color: var(--text-light);
}

.prompt-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 12px;
  margin-top: 24px;
}

.prompt-chip {
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background-color: white;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
  font-size: 14px;
}

.prompt-chip:hover {
  border-color: var(--primary);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.app-footer {
  padding: 16px 24px;
  background-color: white;
  border-top: 1px solid var(--border);
  text-align: center;
  font-size: 12px;
  color: var(--text-light);
  margin-top: auto;
}

@media (max-width: 768px) {
  .workspace {
    flex-direction: column;
  }
  
  .panel-sources {
    width: 100%;
  }
  
  .hero-actions {
    flex-direction: column;
    align-items: center;
  }
  
  .pipeline-row {
    flex-direction: column;
    align-items: center;
  }
  
  .pipeline-card {
    width: 100%;
    max-width: 300px;
  }
}
</style>
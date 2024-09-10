<template>
  <div class="modal-backdrop" @click="$emit('close')">
    <div class="modal-card" @click.stop>
      <h2 class="modal-title">扫码登录</h2>
      <p class="modal-subtitle">使用哔哩哔哩 APP 扫描</p>

      <div class="mt-4 flex justify-center">
        <!-- 模拟登录：显示一个按钮 -->
        <div class="w-48 h-48 flex flex-col items-center justify-center">
          <p class="text-sm text-gray-600 mb-3">开发环境，模拟登录</p>
          <button @click="mockLogin" class="btn btn-primary">点击模拟登录</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { defineEmits } from 'vue'

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'success', sessionId: string, user: any): void
}>()

const mockLogin = () => {
  // 模拟用户信息
  const mockUser = {
    uid: 123456,
    name: '开发测试',
    avatar: 'https://via.placeholder.com/48'
  }
  emit('success', 'mock-session-id', mockUser)
}
</script>

<style scoped>
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}
.modal-card {
  background: white;
  border-radius: 12px;
  padding: 2rem;
  width: 320px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}
.modal-title {
  margin: 0 0 0.5rem;
  font-size: 1.5rem;
}
.modal-subtitle {
  margin: 0 0 1rem;
  color: #666;
}
.flex {
  display: flex;
}
.justify-center {
  justify-content: center;
}
.items-center {
  align-items: center;
}
.flex-col {
  flex-direction: column;
}
.w-48 {
  width: 12rem;
}
.h-48 {
  height: 12rem;
}
.mb-3 {
  margin-bottom: 0.75rem;
}
.mt-4 {
  margin-top: 1rem;
}
.text-sm {
  font-size: 0.875rem;
}
.text-gray-600 {
  color: #666;
}
.btn {
  padding: 0.5rem 1rem;
  border-radius: 0.5rem;
  cursor: pointer;
  border: none;
  background-color: #3b82f6;
  color: white;
}
.btn-primary:hover {
  background-color: #2563eb;
}
</style>
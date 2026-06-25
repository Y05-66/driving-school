<!--
  @description 主题选择器组件
  以弹出气泡（Popover）的形式展示可选主题列表
  用户点击任一主题色块即可实时切换系统主题

  组件结构：
  - 触发按钮：画笔图标，显示在顶栏右侧
  - 弹出面板：3列网格展示所有预设主题，每个主题以缩略预览图 + 名称展示
  - 当前选中的主题会有高亮边框和加粗名称
-->

<template>
  <!-- 使用 el-popover 实现点击弹出效果，定位在右下角 -->
  <el-popover placement="bottom-end" :width="280" trigger="click">
    <!-- 弹出层触发器：画笔图标按钮 -->
    <template #reference>
      <div class="theme-btn">
        <el-icon size="18"><Brush /></el-icon>
      </div>
    </template>
    <!-- 弹出面板内容 -->
    <div class="theme-panel">
      <div class="theme-title">主题风格</div>
      <!-- 主题网格：3列布局，遍历所有预设主题 -->
      <div class="theme-grid">
        <div
          v-for="(theme, key) in themeStore.themes"
          :key="key"
          class="theme-item"
          :class="{ active: themeStore.currentTheme === key }"
          @click="themeStore.setTheme(key)"
        >
          <!-- 主题缩略预览图：模拟侧边栏 + 内容区 + 卡片的布局 -->
          <div class="theme-preview">
            <!-- 左侧色条：模拟侧边栏 -->
            <div class="preview-sidebar" :style="{ background: theme.sidebar }"></div>
            <!-- 右侧区域：模拟主内容区 -->
            <div class="preview-main" :style="{ background: theme.mainBg }">
              <!-- 内嵌小卡片：模拟业务卡片 -->
              <div class="preview-card" :style="{ background: theme.cardBg, borderColor: theme.borderColor }">
                <!-- 小色块：模拟主色按钮 -->
                <div class="preview-btn" :style="{ background: theme.primary }"></div>
              </div>
            </div>
          </div>
          <!-- 主题名称 -->
          <div class="theme-name">{{ theme.name }}</div>
        </div>
      </div>
    </div>
  </el-popover>
</template>

<script setup>
import { useThemeStore } from '../store/theme'
/** @description 主题状态 Store，包含所有主题配置和切换方法 */
const themeStore = useThemeStore()
</script>

<style scoped>
/* 触发按钮样式：与顶栏其他操作按钮保持一致 */
.theme-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
  color: var(--text-secondary, #64748b);
}
.theme-btn:hover {
  background: #f1f5f9;
  color: var(--primary, #6366f1);
}

/* 弹出面板容器 */
.theme-panel { padding: 4px 0; }
.theme-title { font-size: 14px; font-weight: 600; color: #1e293b; margin-bottom: 14px; }

/* 主题网格：3列等宽布局 */
.theme-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; }

/* 单个主题项 */
.theme-item {
  cursor: pointer;
  text-align: center;
  transition: transform 0.2s;
}
.theme-item:hover { transform: translateY(-2px); }
/* 选中状态：名称加粗变色，预览图高亮边框 */
.theme-item.active .theme-name { color: var(--primary, #6366f1); font-weight: 600; }
.theme-item.active .theme-preview { box-shadow: 0 0 0 2px var(--primary, #6366f1); }

/* 主题预览缩略图：模拟完整页面布局 */
.theme-preview {
  width: 72px;
  height: 52px;
  border-radius: 8px;
  overflow: hidden;
  display: flex;
  border: 1px solid #e2e8f0;
  margin: 0 auto 6px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

/* 预览图左侧：模拟侧边栏 */
.preview-sidebar { width: 22px; flex-shrink: 0; }
/* 预览图右侧：模拟主内容区 */
.preview-main { flex: 1; padding: 6px; display: flex; flex-direction: column; gap: 4px; }
/* 预览图内嵌卡片 */
.preview-card {
  flex: 1;
  border-radius: 3px;
  border: 1px solid;
  padding: 4px;
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
}
/* 预览图内嵌按钮色块 */
.preview-btn { width: 14px; height: 6px; border-radius: 2px; }

/* 主题名称 */
.theme-name { font-size: 11px; color: #64748b; }
</style>

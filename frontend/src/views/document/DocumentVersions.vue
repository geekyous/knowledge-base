<!--
================================================================================
文档版本对比页 - views/document/DocumentVersions.vue
================================================================================

【文件说明】
文档版本对比页面，展示同一文档的不同版本之间的差异。
功能包括：
1. 页面标题区（返回按钮 + 标题）
2. 版本选择器（两个下拉框 + 恢复按钮）
3. 变更说明（灰色背景描述框）
4. 分栏对比面板（左侧旧版本 / 右侧新版本，差异高亮）
5. 版本历史卡片（版本号 + 作者 + 日期）

【原型对照】
- 原型位置：prototype-pc.html「文档版本对比」屏幕（2063-2130 行）
- 标题区：返回按钮 + "文档版本对比"
- 对比版本选择器：两个 el-select + 箭头图标 + 恢复按钮
- 变更说明：灰色背景框显示版本变更描述
- 分栏对比：左侧 v1.1（旧版本）+ 右侧 v2.0（新版本）
- 版本历史：3 张卡片（v2.0 当前、v1.1、v1.0）
================================================================================
-->

<template>
  <div class="document-versions-page">
    <!-- ================================================================ -->
    <!-- 页面标题区（匹配原型：返回按钮 + 标题） -->
    <!-- ================================================================ -->
    <div class="page-header">
      <el-button text @click="goBack" class="back-btn">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <h1 class="page-title">文档版本对比</h1>
    </div>

    <!-- ================================================================ -->
    <!-- 版本选择器（匹配原型：两个下拉框 + 箭头 + 恢复按钮） -->
    <!-- ================================================================ -->
    <div class="version-selector">
      <span class="selector-label">对比版本：</span>
      <el-select
        v-model="oldVersion"
        placeholder="选择旧版本"
        class="version-select"
        @change="onVersionChange"
      >
        <el-option
          v-for="v in versionOptions"
          :key="v.value"
          :label="v.label"
          :value="v.value"
        />
      </el-select>
      <el-icon class="arrow-icon"><Right /></el-icon>
      <el-select
        v-model="newVersion"
        placeholder="选择新版本"
        class="version-select"
        @change="onVersionChange"
      >
        <el-option
          v-for="v in versionOptions"
          :key="v.value"
          :label="v.label"
          :value="v.value"
        />
      </el-select>
      <el-button type="primary" plain class="restore-btn">
        恢复到 {{ oldVersion }}
      </el-button>
    </div>

    <!-- ================================================================ -->
    <!-- 变更说明（匹配原型：灰色背景框） -->
    <!-- ================================================================ -->
    <div class="change-description">
      <span class="version-label">{{ newVersion }}</span> 变更说明：{{ changeDescription }}
    </div>

    <!-- ================================================================ -->
    <!-- 分栏对比面板（匹配原型：左侧旧版本 / 右侧新版本） -->
    <!-- ================================================================ -->
    <div class="diff-panel">
      <!-- 左侧：旧版本 -->
      <div class="diff-column diff-old">
        <div class="diff-column-header">
          <span class="column-version">{{ oldVersion }}（旧版本）</span>
        </div>
        <div class="diff-column-content">
          <div
            v-for="(line, index) in oldContent"
            :key="index"
            :class="['diff-line', { 'diff-del': line.type === 'del' }]"
          >
            <span class="line-prefix" v-if="line.type === 'del'">-</span>
            <span class="line-prefix" v-else>&nbsp;</span>
            <span class="line-text">{{ line.text }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧：新版本 -->
      <div class="diff-column diff-new">
        <div class="diff-column-header">
          <span class="column-version">{{ newVersion }}（新版本）</span>
        </div>
        <div class="diff-column-content">
          <div
            v-for="(line, index) in newContent"
            :key="index"
            :class="['diff-line', { 'diff-add': line.type === 'add' }]"
          >
            <span class="line-prefix" v-if="line.type === 'add'">+</span>
            <span class="line-prefix" v-else>&nbsp;</span>
            <span class="line-text">{{ line.text }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ================================================================ -->
    <!-- 版本历史卡片（匹配原型：3 张卡片 v2.0 / v1.1 / v1.0） -->
    <!-- ================================================================ -->
    <div class="version-history">
      <h3 class="section-title">版本历史</h3>
      <div class="history-cards">
        <div
          v-for="v in versionHistory"
          :key="v.version"
          class="history-card"
          :class="{ active: v.version === newVersion }"
        >
          <div class="card-header-row">
            <span class="card-version">{{ v.version }}</span>
            <el-tag
              v-if="v.isCurrent"
              type="success"
              size="small"
              effect="dark"
              class="current-badge"
            >
              当前
            </el-tag>
          </div>
          <div class="card-meta">
            <span class="card-author">{{ v.author }}</span>
            <span class="card-date">{{ v.date }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// ============================================================================
// 导入
// ============================================================================
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ArrowLeft, Right } from '@element-plus/icons-vue'

// ============================================================================
// 路由
// ============================================================================
const router = useRouter()

// ============================================================================
// 版本数据（Mock）
// ============================================================================
interface VersionInfo {
  version: string
  date: string
  author: string
  isCurrent: boolean
}

const versionHistory = ref<VersionInfo[]>([
  { version: 'v2.0', date: '2025-12-15', author: 'Geekyous Guo', isCurrent: true },
  { version: 'v1.1', date: '2025-12-01', author: 'Geekyous Guo', isCurrent: false },
  { version: 'v1.0', date: '2025-11-01', author: 'Geekyous Guo', isCurrent: false }
])

const versionOptions = computed(() =>
  versionHistory.value.map(v => ({
    value: v.version,
    label: `${v.version}（${v.date}）`
  }))
)

// ============================================================================
// 选中状态
// ============================================================================
const oldVersion = ref('v1.1')
const newVersion = ref('v2.0')

// ============================================================================
// 变更说明
// ============================================================================
const changeDescription = ref(
  '新增灰度发布流程、回滚方案；更新发布前准备清单'
)

// ============================================================================
// Diff 内容行
// ============================================================================
interface DiffLine {
  text: string
  type: 'normal' | 'del' | 'add'
}

const oldContent = ref<DiffLine[]>([
  { text: '# 发布流程文档', type: 'normal' },
  { text: '', type: 'normal' },
  { text: '## 1. 发布前准备', type: 'normal' },
  { text: '- 确认代码审查完成', type: 'del' },
  { text: '- 运行全部测试用例', type: 'normal' },
  { text: '- 通知相关人员', type: 'normal' },
  { text: '', type: 'normal' },
  { text: '## 2. 发布步骤', type: 'normal' },
  { text: '- 合并代码到主分支', type: 'normal' },
  { text: '- 执行构建打包', type: 'normal' },
  { text: '- 直接部署到生产环境', type: 'del' },
  { text: '- 验证功能正常', type: 'normal' },
  { text: '', type: 'normal' },
  { text: '## 3. 异常处理', type: 'normal' },
  { text: '- 联系运维人员手动回退', type: 'del' },
  { text: '- 记录异常日志', type: 'normal' }
])

const newContent = ref<DiffLine[]>([
  { text: '# 发布流程文档', type: 'normal' },
  { text: '', type: 'normal' },
  { text: '## 1. 发布前准备', type: 'normal' },
  { text: '- 确认代码审查完成并通过', type: 'add' },
  { text: '- 运行全部测试用例', type: 'normal' },
  { text: '- 检查配置文件和环境变量', type: 'add' },
  { text: '- 通知相关人员', type: 'normal' },
  { text: '', type: 'normal' },
  { text: '## 2. 发布步骤', type: 'normal' },
  { text: '- 合并代码到主分支', type: 'normal' },
  { text: '- 执行构建打包', type: 'normal' },
  { text: '- 灰度发布：先发布到 10% 流量', type: 'add' },
  { text: '- 监控指标正常后逐步扩大流量', type: 'add' },
  { text: '- 全量发布到生产环境', type: 'add' },
  { text: '- 验证功能正常', type: 'normal' },
  { text: '', type: 'normal' },
  { text: '## 3. 回滚方案', type: 'add' },
  { text: '- 自动回滚：监控告警触发自动回退', type: 'add' },
  { text: '- 手动回滚：一键回退到上一稳定版本', type: 'add' },
  { text: '', type: 'normal' },
  { text: '## 4. 异常处理', type: 'normal' },
  { text: '- 记录异常日志', type: 'normal' }
])

// ============================================================================
// 事件处理
// ============================================================================
const onVersionChange = () => {
  // 后续对接后端 API 时，根据选中的版本号加载对应的 diff 数据
}

const goBack = () => {
  router.back()
}
</script>

<style scoped lang="scss">
// ============================================================================
// 页面容器
// ============================================================================
.document-versions-page {
  max-width: 1100px;
  margin: 0 auto;
  padding: 24px 20px;
}

// ============================================================================
// 页面标题区
// ============================================================================
.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;

  .back-btn {
    font-size: 14px;
    color: #606266;
    padding: 4px 8px;

    .el-icon {
      margin-right: 4px;
    }

    &:hover {
      color: #409eff;
    }
  }

  .page-title {
    font-size: 20px;
    font-weight: 600;
    color: #303133;
    margin: 0;
  }
}

// ============================================================================
// 版本选择器
// ============================================================================
.version-selector {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;

  .selector-label {
    font-size: 14px;
    color: #303133;
    font-weight: 500;
    white-space: nowrap;
  }

  .version-select {
    width: 200px;
  }

  .arrow-icon {
    font-size: 18px;
    color: #909399;
  }

  .restore-btn {
    margin-left: auto;
  }
}

// ============================================================================
// 变更说明
// ============================================================================
.change-description {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 14px 20px;
  margin-bottom: 20px;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;

  .version-label {
    font-weight: 600;
    color: #303133;
  }
}

// ============================================================================
// 分栏对比面板
// ============================================================================
.diff-panel {
  display: flex;
  gap: 0;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 32px;
}

.diff-column {
  flex: 1;
  min-width: 0;

  &.diff-old {
    border-right: 1px solid #e4e7ed;
  }
}

.diff-column-header {
  background: #f5f7fa;
  padding: 12px 20px;
  border-bottom: 1px solid #e4e7ed;

  .column-version {
    font-size: 14px;
    font-weight: 600;
    color: #303133;
  }
}

.diff-column-content {
  padding: 16px 0;
  font-family: 'Menlo', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
  line-height: 1.8;
  background: #fafafa;
}

.diff-line {
  padding: 0 20px;
  display: flex;
  align-items: flex-start;

  &.diff-del {
    background: #fef0f0;
  }

  &.diff-add {
    background: #f0f9eb;
  }

  .line-prefix {
    display: inline-block;
    width: 16px;
    flex-shrink: 0;
    font-weight: 700;
    text-align: center;
  }

  .diff-del .line-prefix {
    color: #f56c6c;
  }

  .diff-add .line-prefix {
    color: #67c23a;
  }

  .line-text {
    color: #303133;
    white-space: pre-wrap;
    word-break: break-all;
  }
}

// ============================================================================
// 版本历史卡片
// ============================================================================
.version-history {
  .section-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    margin: 0 0 16px 0;
  }
}

.history-cards {
  display: flex;
  gap: 16px;
}

.history-card {
  flex: 1;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 16px 20px;
  transition: all 0.3s;

  &:hover {
    border-color: #409eff;
    box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
  }

  &.active {
    border-color: #409eff;
    box-shadow: 0 2px 12px rgba(64, 158, 255, 0.1);
  }

  .card-header-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
  }

  .card-version {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
  }

  .current-badge {
    font-size: 12px;
  }

  .card-meta {
    display: flex;
    align-items: center;
    gap: 16px;
    font-size: 13px;
    color: #909399;

    .card-author {
      color: #606266;
    }
  }
}
</style>

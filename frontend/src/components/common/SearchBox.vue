<!--
================================================================================
搜索框组件 - components/common/SearchBox.vue
================================================================================

【文件说明】
通用搜索框组件，提供搜索输入和搜索触发功能。
支持回车搜索和按钮搜索两种方式。

【Vue 概念】
- defineProps: 定义组件的输入属性
- defineEmits: 定义组件发出的事件
- v-model: 双向绑定输入值
================================================================================
-->

<template>
  <div class="search-box">
    <el-input
      v-model="searchText"
      :placeholder="placeholder"
      size="large"
      clearable
      @keyup.enter="handleSearch"
    >
      <template #prefix>
        <el-icon><Search /></el-icon>
      </template>
      <template #append>
        <el-button :icon="Search" @click="handleSearch">搜索</el-button>
      </template>
    </el-input>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Search } from '@element-plus/icons-vue'

// Props
interface Props {
  placeholder?: string
  modelValue?: string
}

const props = withDefaults(defineProps<Props>(), {
  placeholder: '搜索...',
  modelValue: ''
})

// Emits
const emit = defineEmits<{
  (e: 'search', keyword: string): void
  (e: 'update:modelValue', value: string): void
}>()

const searchText = ref(props.modelValue)

const handleSearch = () => {
  if (searchText.value.trim()) {
    emit('search', searchText.value.trim())
  }
}
</script>

<style scoped lang="scss">
.search-box {
  max-width: 640px;
  margin: 0 auto;
}
</style>

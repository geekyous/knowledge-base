/**
 * ============================================================================
 * 管理后台 API 模块 - api/admin.ts
 * ============================================================================
 *
 * 【文件说明】
 * 封装所有管理后台相关的后端 API 调用。
 * 包含 6 个 API 对象：用户管理、文档审核、分类管理、标签管理、系统设置、仪表盘。
 * 所有接口前缀 /v1/admin/，后端 SecurityConfig 已配置 hasRole("ADMIN") 保护。
 *
 * 【API 模块】
 * - adminUserApi: 用户 CRUD + 禁用/解锁/重置密码
 * - adminReviewApi: 文档审核（通过/拒绝/批量）
 * - adminCategoryApi: 分类管理（树 + CRUD）
 * - adminTagApi: 标签 CRUD
 * - adminSettingsApi: 系统设置读写
 * - adminDashboardApi: 仪表盘统计
 * ============================================================================
 */

import { http } from '@/utils/request'
import type {
  AdminUser,
  CreateUserRequest,
  UpdateUserRequest,
  Tag,
  CategoryTreeNode,
  DashboardStats,
  SystemSettings,
  PageResponse,
  Document
} from '@/types'

/** 用户管理 API */
export const adminUserApi = {
  list: (params: { page: number; size: number; keyword?: string; role?: string; status?: string }) =>
    http.get<PageResponse<AdminUser>>('/v1/admin/users', { params }),
  get: (id: number) =>
    http.get<AdminUser>(`/v1/admin/users/${id}`),
  create: (data: CreateUserRequest) =>
    http.post<AdminUser>('/v1/admin/users', data),
  update: (id: number, data: UpdateUserRequest) =>
    http.put<AdminUser>(`/v1/admin/users/${id}`, data),
  resetPassword: (id: number, data: { newPassword: string }) =>
    http.put<void>(`/v1/admin/users/${id}/reset-password`, data),
  disable: (id: number) =>
    http.put<void>(`/v1/admin/users/${id}/disable`),
  unlock: (id: number) =>
    http.put<void>(`/v1/admin/users/${id}/unlock`),
  delete: (id: number) =>
    http.delete<void>(`/v1/admin/users/${id}`)
}

/** 文档审核 API */
export const adminReviewApi = {
  listPending: (params: { page: number; size: number }) =>
    http.get<PageResponse<Document>>('/v1/admin/reviews/pending', { params }),
  listReviewed: (params: { status: string; page: number; size: number }) =>
    http.get<PageResponse<Document>>('/v1/admin/reviews', { params }),
  approve: (id: number) =>
    http.put<void>(`/v1/admin/reviews/${id}/approve`),
  reject: (id: number, reason?: string) =>
    http.put<void>(`/v1/admin/reviews/${id}/reject`, { reason }),
  batchApprove: (ids: number[]) =>
    http.put<void>('/v1/admin/reviews/batch-approve', { ids })
}

/** 分类管理 API */
export const adminCategoryApi = {
  getTree: () =>
    http.get<CategoryTreeNode[]>('/v1/admin/categories/tree'),
  create: (data: { name: string; parentId?: number; icon?: string; sortOrder?: number }) =>
    http.post<CategoryTreeNode>('/v1/admin/categories', data),
  update: (id: number, data: { name?: string; parentId?: number; icon?: string; sortOrder?: number }) =>
    http.put<CategoryTreeNode>(`/v1/admin/categories/${id}`, data),
  delete: (id: number) =>
    http.delete<void>(`/v1/admin/categories/${id}`)
}

/** 标签管理 API */
export const adminTagApi = {
  list: () =>
    http.get<Tag[]>('/v1/admin/tags'),
  create: (data: { name: string; color?: string }) =>
    http.post<Tag>('/v1/admin/tags', data),
  update: (id: number, data: { name?: string; color?: string }) =>
    http.put<Tag>(`/v1/admin/tags/${id}`, data),
  delete: (id: number) =>
    http.delete<void>(`/v1/admin/tags/${id}`)
}

/** 系统设置 API */
export const adminSettingsApi = {
  getAll: () =>
    http.get<SystemSettings>('/v1/admin/settings'),
  update: (settings: Record<string, string>) =>
    http.put<void>('/v1/admin/settings', settings)
}

/** 仪表盘 API */
export const adminDashboardApi = {
  getStats: () =>
    http.get<DashboardStats>('/v1/admin/dashboard/stats'),
  getRecentDocs: (limit?: number) =>
    http.get<Document[]>('/v1/admin/dashboard/recent-docs', { params: { limit } }),
  getPendingReviews: (params?: { page?: number; size?: number }) =>
    http.get<PageResponse<Document>>('/v1/admin/dashboard/pending-reviews', { params })
}

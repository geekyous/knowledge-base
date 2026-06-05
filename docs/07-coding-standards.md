# 企业知识库问答系统 - 编码规范

## 📋 通用规范

### 代码风格原则
- **可读性优先** - 代码应该易于阅读和理解
- **一致性** - 遵循统一的代码风格
- **简洁性** - 避免过度设计和冗余代码
- **可维护性** - 便于后续维护和扩展

### 命名规范

#### 变量命名
```javascript
// ✅ 好的命名
const userAge = 25;
const documentList = [];
const isActive = true;

// ❌ 不好的命名
const a = 25;
const list = [];
const flag = true;
```

#### 函数命名
```javascript
// ✅ 好的命名
function getUserInfo() {}
function validateEmail() {}
function calculateTotal() {}

// ❌ 不好的命名
function getData() {}
function check() {}
function calc() {}
```

#### 类命名
```javascript
// ✅ 好的命名
class UserService {}
class DocumentController {}
class EmailValidator {}

// ❌ 不好的命名
class user {}
class document {}
class validator {}
```

## 🎨 前端编码规范 (Vue 3 + TypeScript)

### 组件规范

#### 组件文件命名
```
✅ PascalCase: UserProfile.vue
✅ kebab-case: user-profile.vue
❌ camelCase: userProfile.vue
```

#### 组件结构
```vue
<template>
  <!-- 模板内容 -->
</template>

<script setup lang="ts">
// 1. 导入
import { ref, computed } from 'vue'
import type { User } from './types'

// 2. Props 定义
interface Props {
  userId: number
}
const props = defineProps<Props>()

// 3. Emits 定义
interface Emits {
  (e: 'update', value: number): void
}
const emit = defineEmits<Emits>()

// 4. 响应式数据
const loading = ref(false)
const user = ref<User | null>(null)

// 5. 计算属性
const displayName = computed(() => user.value?.name ?? '')

// 6. 方法定义
const fetchData = async () => {
  loading.value = true
  try {
    // ...
  } finally {
    loading.value = false
  }
}

// 7. 生命周期
onMounted(() => {
  fetchData()
})
</script>

<style scoped>
/* 样式代码 */
</style>
```

### TypeScript 类型定义

#### 类型文件组织
```typescript
// types/user.ts
export interface User {
  id: number
  username: string
  email: string
  role: 'USER' | 'EDITOR' | 'ADMIN'
}

export interface CreateUserRequest {
  username: string
  password: string
  email: string
}

export interface UserResponse {
  id: number
  username: string
  email: string
  role: string
  createdAt: string
}
```

#### API 类型定义
```typescript
// api/types.ts
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

export interface PageResponse<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}
```

### API 调用规范

#### 请求封装
```typescript
// api/request.ts
import axios from 'axios'
import type { ApiResponse } from './types'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器
request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    // 统一错误处理
    return Promise.reject(error)
  }
)

export default request
```

#### API 模块
```typescript
// api/document.ts
import request from './request'
import type { ApiResponse, PageResponse } from './types'
import type { Document, DocumentDTO } from '@/types/document'

export const documentApi = {
  // 获取文档列表
  getList: (params: {
    page: number
    pageSize: number
    categoryId?: number
  }) =>
    request.get<ApiResponse<PageResponse<Document>>>(
      '/api/v1/documents',
      { params }
    ),

  // 获取文档详情
  getDetail: (id: number) =>
    request.get<ApiResponse<Document>>(`/api/v1/documents/${id}`),

  // 创建文档
  create: (data: DocumentDTO) =>
    request.post<ApiResponse<Document>>('/api/v1/documents', data),

  // 更新文档
  update: (id: number, data: DocumentDTO) =>
    request.put<ApiResponse<Document>>(`/api/v1/documents/${id}`, data),

  // 删除文档
  delete: (id: number) =>
    request.delete<ApiResponse<void>>(`/api/v1/documents/${id}`)
}
```

### 状态管理规范

#### Pinia Store
```typescript
// stores/user.ts
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types/user'

export const useUserStore = defineStore('user', () => {
  // State
  const currentUser = ref<User | null>(null)
  const token = ref<string | null>(null)

  // Getters
  const isLoggedIn = computed(() => !!token.value)
  const userRole = computed(() => currentUser.value?.role)

  // Actions
  const setUser = (user: User) => {
    currentUser.value = user
  }

  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const logout = () => {
    currentUser.value = null
    token.value = null
    localStorage.removeItem('token')
  }

  return {
    currentUser,
    token,
    isLoggedIn,
    userRole,
    setUser,
    setToken,
    logout
  }
})
```

### 路由规范

#### 路由配置
```typescript
// router/index.ts
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/home/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'documents',
        name: 'DocumentList',
        component: () => import('@/views/document/DocumentList.vue'),
        meta: { title: '文档列表', requiresAuth: true }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'Login' })
  } else {
    next()
  }
})

export default router
```

## 🔧 后端编码规范 (Java + Spring Boot)

### 包结构规范

```
com.company.kb
├── config/          # 配置类
├── controller/      # 控制器
├── service/         # 服务接口
│   └── impl/        # 服务实现
├── repository/      # 数据访问
├── entity/          # 实体类
├── dto/             # 数据传输对象
│   ├── request/     # 请求DTO
│   └── response/    # 响应DTO
├── security/        # 安全相关
├── exception/       # 异常处理
├── utils/           # 工具类
└── annotation/      # 自定义注解
```

### 类命名规范

```java
// ✅ 正确命名
public class UserController {}
public class DocumentService {}
public class UserRepository {}
public class UserDTO {}

// ❌ 错误命名
public class userController {}
public class document_service {}
public class USER_REPOSITORY {}
```

### 控制器规范

```java
@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "文档管理", description = "文档管理接口")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    @Operation(summary = "获取文档列表")
    public ResponseEntity<ApiResponse<PageResponse<DocumentDTO>>> getDocuments(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        PageResponse<DocumentDTO> response = documentService.getDocuments(page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @Operation(summary = "创建文档")
    public ResponseEntity<ApiResponse<DocumentDTO>> createDocument(
            @Valid @RequestBody DocumentCreateRequest request) {

        DocumentDTO document = documentService.createDocument(request);
        return ResponseEntity.ok(ApiResponse.success(document));
    }
}
```

### 服务层规范

```java
public interface DocumentService {
    PageResponse<DocumentDTO> getDocuments(int page, int pageSize);
    DocumentDTO getDocument(Long id);
    DocumentDTO createDocument(DocumentCreateRequest request);
    DocumentDTO updateDocument(Long id, DocumentUpdateRequest request);
    void deleteDocument(Long id);
}

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;

    @Override
    public PageResponse<DocumentDTO> getDocuments(int page, int pageSize) {
        Page<Document> documents = documentRepository.findAll(
            PageRequest.of(page - 1, pageSize)
        );
        return new PageResponse<>(documents);
    }

    @Override
    @Transactional
    public DocumentDTO createDocument(DocumentCreateRequest request) {
        Document document = new Document();
        document.setTitle(request.getTitle());
        document.setContent(request.getContent());
        document.setAuthor(userRepository.getCurrentUser());
        document = documentRepository.save(document);
        return DocumentDTO.fromEntity(document);
    }
}
```

### 实体类规范

```java
@Entity
@Table(name = "documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DocumentStatus status;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Timestamp createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Timestamp updatedAt;
}
```

### DTO 规范

```java
// Request DTO
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentCreateRequest {
    @NotBlank(message = "标题不能为空")
    @Size(max = 255, message = "标题长度不能超过255个字符")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    private Long categoryId;

    private List<String> tags;
}

// Response DTO
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentDTO {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private CategoryDTO category;
    private UserDTO author;
    private String status;
    private Integer viewCount;
    private LocalDateTime createdAt;

    public static DocumentDTO fromEntity(Document document) {
        return DocumentDTO.builder()
            .id(document.getId())
            .title(document.getTitle())
            .summary(document.getSummary())
            .content(document.getContent())
            .category(CategoryDTO.fromEntity(document.getCategory()))
            .author(UserDTO.fromEntity(document.getAuthor()))
            .status(document.getStatus().name())
            .viewCount(document.getViewCount())
            .createdAt(document.getCreatedAt().toLocalDateTime())
            .build();
    }
}
```

### 异常处理规范

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        return ResponseEntity
            .status(e.getErrorCode().getHttpStatus())
            .body(ApiResponse.error(e.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException e) {

        List<String> errors = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .collect(Collectors.toList());

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ErrorCode.VALIDATION_FAILED, errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(ErrorCode.SYSTEM_ERROR));
    }
}
```

## 🤖 AI 服务编码规范 (Python + FastAPI)

### 项目结构规范

```python
app/
├── api/              # 路由
├── services/         # 业务逻辑
├── models/           # 数据模型
├── core/             # 核心配置
├── utils/            # 工具函数
└── main.py           # 应用入口
```

### 路由规范

```python
from fastapi import APIRouter, Depends
from app.services.rag_service import RAGService
from app.models.request import QuestionRequest
from app.models.response import AnswerResponse

router = APIRouter(prefix="/chat", tags=["问答"])

@router.post("/ask", response_model=AnswerResponse)
async def ask_question(
    request: QuestionRequest,
    rag_service: RAGService = Depends()
) -> AnswerResponse:
    """
    智能问答接口
    """
    answer = await rag_service.ask_question(request.question)
    return AnswerResponse(answer=answer)
```

### 服务层规范

```python
from langchain.chains import RetrievalQA
from langchain.llms import OpenAI
from app.core.vector_store import get_vector_store
from app.core.llm import get_llm

class RAGService:
    def __init__(self):
        self.vector_store = get_vector_store()
        self.llm = get_llm()
        self.qa_chain = RetrievalQA.from_chain_type(
            llm=self.llm,
            chain_type="stuff",
            retriever=self.vector_store.as_retriever()
        )

    async def ask_question(self, question: str) -> str:
        """提问并返回答案"""
        result = await self.qa_chain.ainvoke({"query": question})
        return result["result"]
```

### 数据模型规范

```python
from pydantic import BaseModel, Field
from typing import List, Optional

class QuestionRequest(BaseModel):
    """问题请求"""
    question: str = Field(..., description="用户问题", min_length=1, max_length=500)
    conversation_id: Optional[str] = Field(None, description="对话ID")

    class Config:
        json_schema_extra = {
            "example": {
                "question": "如何申请年假？",
                "conversation_id": "conv-123"
            }
        }

class SourceDocument(BaseModel):
    """来源文档"""
    document_id: int
    title: str
    snippet: str
    relevance: float

class AnswerResponse(BaseModel):
    """答案响应"""
    answer: str
    sources: List[SourceDocument]
    follow_up_questions: List[str]
```

## 🧪 测试规范

### 单元测试

```java
@SpringBootTest
class DocumentServiceTest {

    @Autowired
    private DocumentService documentService;

    @MockBean
    private DocumentRepository documentRepository;

    @Test
    void shouldGetDocumentList() {
        // Given
        when(documentRepository.findAll(any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        // When
        PageResponse<DocumentDTO> result = documentService.getDocuments(1, 20);

        // Then
        assertThat(result.getItems()).isEmpty();
    }
}
```

### API 测试

```python
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_ask_question():
    response = client.post("/api/v1/chat/ask", json={
        "question": "如何申请年假？"
    })

    assert response.status_code == 200
    data = response.json()
    assert "answer" in data
```

## 📝 注释规范

### Java 注释

```java
/**
 * 文档服务接口
 *
 * @author Geekyous Guo
 * @since 1.0.0
 */
public interface DocumentService {

    /**
     * 获取文档列表
     *
     * @param page 页码，从1开始
     * @param pageSize 每页大小
     * @return 分页响应
     * @throws BusinessException 业务异常
     */
    PageResponse<DocumentDTO> getDocuments(int page, int pageSize);
}
```

### Python 注释

```python
def ask_question(self, question: str) -> str:
    """
    提问并返回答案

    Args:
        question: 用户问题

    Returns:
        答案文本

    Raises:
        ValueError: 当问题为空时
    """
    if not question:
        raise ValueError("问题不能为空")

    # 实现逻辑...
```

---

**文档版本：** v1.0
**最后更新：** 2026-05-31

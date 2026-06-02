# F01 - 用户认证与权限管理 - 开发指南

## 🛠️ 开发环境准备

### 1. 前端开发环境

#### 依赖安装
```bash
cd frontend
npm install
```

#### 新增依赖
```json
{
  "dependencies": {
    "pinia": "^2.1.7",
    "axios": "^1.6.8"
  }
}
```

### 2. 后端开发环境

#### Maven依赖
```xml
<!-- Spring Security -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.12.3</version>
</dependency>

<!-- Validation -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

## 📝 开发步骤

### 第一步：后端 - 实体类和Repository

#### 1. 创建用户实体

**文件位置：** `backend/src/main/java/com/company/kb/entity/User.java`

```java
package com.company.kb.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 用户实体
 */
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column
    private String avatar;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private Role role = Role.USER;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "login_attempts")
    @Builder.Default
    private Integer loginAttempts = 0;

    @Column(name = "locked_until")
    private Timestamp lockedUntil;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_login_ip")
    private String lastLoginIp;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public enum Role {
        USER, EDITOR, ADMIN
    }

    public enum UserStatus {
        ACTIVE, INACTIVE, LOCKED
    }

    /**
     * 检查账号是否被锁定
     */
    public boolean isLocked() {
        if (status != UserStatus.ACTIVE) {
            return true;
        }
        if (lockedUntil != null && lockedUntil.toInstant().isAfter(java.time.Instant.now())) {
            return true;
        }
        return false;
    }

    /**
     * 增加登录失败次数
     */
    public void incrementLoginAttempts() {
        this.loginAttempts++;
        if (this.loginAttempts >= 5) {
            this.status = UserStatus.LOCKED;
            this.lockedUntil = Timestamp.from(java.time.Instant.now().plusSeconds(30 * 60));
        }
    }

    /**
     * 重置登录失败次数
     */
    public void resetLoginAttempts() {
        this.loginAttempts = 0;
        this.status = UserStatus.ACTIVE;
        this.lockedUntil = null;
    }
}
```

#### 2. 创建用户Repository

**文件位置：** `backend/src/main/java/com/company/kb/repository/UserRepository.java`

```java
package com.company.kb.repository;

import com.company.kb.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);
}
```

### 第二步：后端 - 安全配置

#### 1. JWT工具类

**文件位置：** `backend/src/main/java/com/company/kb/security/JwtProvider.java`

```java
package com.company.kb.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT Token 提供者
 */
@Slf4j
@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    /**
     * 生成Token
     */
    public String generateToken(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setSubject(Long.toString(userPrincipal.getId()))
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .claim("username", userPrincipal.getUsername())
                .claim("role", userPrincipal.getRole().name())
                .signWith(getSigningKey(), SignatureAlgorithm.HS256);
    }

    /**
     * 从Token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return Long.parseLong(claims.getSubject());
    }

    /**
     * 验证Token
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (JwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * 获取Token过期时间
     */
    public long getExpirationTime() {
        return jwtExpirationMs;
    }
}
```

#### 2. 用户详情服务

**文件位置：** `backend/src/main/java/com/company/kb/security/UserDetailsServiceImpl.java`

```java
package com.company.kb.security;

import com.company.kb.entity.User;
import com.company.kb.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 用户详情服务实现
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));

        if (user.isLocked()) {
            throw new LockedAccountException("账号已被锁定");
        }

        return new UserPrincipal(user);
    }
}
```

#### 3. 用户Principal

**文件位置：** `backend/src/main/java/com/company/kb/security/UserPrincipal.java`

```java
package com.company.kb.security;

import com.company.kb.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

/**
 * 用户认证信息
 */
@Data
@Builder
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private Long id;
    private String username;
    private String password;
    private User.Role role;
    private Collection<? extends GrantedAuthority> authorities;

    public static UserPrincipal create(User user) {
        return UserPrincipal.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .role(user.getRole())
                .authorities(Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
                ))
                .build();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
```

#### 4. JWT认证过滤器

**文件位置：** `backend/src/main/java/com/company/kb/security/JwtAuthenticationFilter.java`

```java
package com.company.kb.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 认证过滤器
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String jwt = getJwtFromRequest(request);

        if (StringUtils.hasText(jwt) && jwtProvider.validateToken(jwt)) {
            Long userId = jwtProvider.getUserIdFromToken(jwt);

            // 这里可以根据userId从缓存或数据库获取用户信息
            // 简化版本：
            UserDetails userDetails = userDetailsService.loadUserByUsername(
                    jwtProvider.getUserIdFromToken(jwt).toString());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());

            authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
```

#### 5. 安全配置

**文件位置：** `backend/src/main/java/com/company/kb/config/SecurityConfig.java`

```java
package com.company.kb.config;

import com.company.kb.security.JwtAuthenticationFilter;
import com.company.kb.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 安全配置
 */
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/health").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationManager authenticationManager(org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
        return http.getSharedObject(AuthenticationManager.class);
    }
}
```

### 第三步：后端 - 业务逻辑

#### 1. 认证服务

**文件位置：** `backend/src/main/java/com/company/kb/service/AuthService.java`

```java
package com.company.kb.service;

import com.company.kb.dto.LoginRequest;
import com.company.kb.dto.LoginResponse;
import com.company.kb.dto.RegisterRequest;
import com.company.kb.entity.User;
import com.company.kb.exception.*;
import com.company.kb.repository.UserRepository;
import com.company.kb.security.JwtProvider;
import com.company.kb.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    /**
     * 用户登录
     */
    public LoginResponse login(LoginRequest request) {
        // 检查账号是否被锁定
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("用户名或密码错误"));

        if (user.isLocked()) {
            throw new AccountLockedException("账号已被锁定，请联系管理员");
        }

        // 验证用户名和密码
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        // 生成Token
        String token = jwtProvider.generateToken(authentication);

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        user.resetLoginAttempts();
        userRepository.save(user);

        // 构建响应
        return LoginResponse.builder()
                .token(token)
                .user(mapToUserResponse(user))
                .build();
    }

    /**
     * 用户注册
     */
    public LoginResponse register(RegisterRequest request) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("用户名已被使用", "USERNAME_EXISTS");
        }

        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("邮箱已被注册", "EMAIL_EXISTS");
        }

        // 创建新用户
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .role(User.Role.USER)
                .status(User.UserStatus.ACTIVE)
                .build();

        user = userRepository.save(user);

        // 自动登录
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        String token = jwtProvider.generateToken(authentication);

        return LoginResponse.builder()
                .token(token)
                .user(mapToUserResponse(user))
                .build();
    }

    /**
     * 获取当前用户信息
     */
    public com.company.kb.dto.UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        return mapToUserResponse(user);
    }

    /**
     * 更新用户信息
     */
    public com.company.kb.dto.UserResponse updateCurrentUser(com.company.kb.dto.UserUpdateRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        // 更新字段
        if (request.getEmail() != null) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BusinessException("邮箱已被使用", "EMAIL_EXISTS");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        user = userRepository.save(user);

        return mapToUserResponse(user);
    }

    /**
     * 修改密码
     */
    public void changePassword(com.company.kb.dto.ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        // 验证旧密码
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new UnauthorizedException("旧密码错误");
        }

        // 更新密码
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    /**
     * 刷新Token
     */
    public String refreshToken(String oldToken) {
        Long userId = jwtProvider.getUserIdFromToken(oldToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("用户不存在"));

        // 生成新Token
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );

        return jwtProvider.generateToken(authentication);
    }

    /**
     * 登出
     */
    public void logout() {
        // 可以将Token加入黑名单
        // 清理本地存储由前端处理
    }

    /**
     * 映射User到UserResponse
     */
    private com.company.kb.dto.UserResponse mapToUserResponse(User user) {
        return com.company.kb.dto.UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .avatar(user.getAvatar())
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
```

#### 2. 认证控制器

**文件位置：** `backend/src/main/java/com/company/kb/controller/AuthController.java`

```java
package com.company.kb.controller;

import com.company.kb.dto.*;
import com.company.kb.service.AuthService;
import com.company.kb.utils.Response;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Response<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return Response.success("登录成功", response);
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Response<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        LoginResponse response = authService.register(request);
        return Response.success("注册成功", response);
    }

    /**
     * 刷新Token
     */
    @PostMapping("/refresh")
    public Response<RefreshTokenResponse> refreshToken(@RequestHeader("Authorization") String token) {
        String newToken = authService.refreshToken(token.replace("Bearer ", ""));
        return Response.success("Token刷新成功", new RefreshTokenResponse(newToken));
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public Response<Void> logout() {
        authService.logout();
        return Response.success("登出成功");
    }
}
```

### 第四步：前端 - 登录页面

#### 1. 登录组件

**文件位置：** `frontend/src/views/auth/Login.vue`

```vue
<template>
  <div class="login-page">
    <div class="login-container">
      <!-- Logo 和标题 -->
      <div class="login-header">
        <el-icon :size="48" color="#409eff"><Reading /></el-icon>
        <h1 class="login-title">企业知识库</h1>
        <p class="login-subtitle">智能问答系统</p>
      </div>

      <!-- 登录表单 -->
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        size="large"
        class="login-form"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="formData.username"
            placeholder="用户名"
            :prefix-icon="User"
            clearable
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="密码"
            :prefix-icon="Lock"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="formData.remember">记住我</el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="login-button"
            @click="handleLogin"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Reading, User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const formData = reactive({
  username: '',
  password: '',
  remember: false
})

const formRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为 3 到 20 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 32, message: '密码长度为 6 到 32 个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!formRef.value) return

  try {
    const valid = await formRef.value.validate()
    if (!valid) return

    loading.value = true

    await authStore.login({
      username: formData.username,
      password: formData.password
    })

    ElMessage.success('登录成功')

    // 跳转到重定向页面或首页
    const redirect = (route.query.redirect as string) || '/home'
    router.push(redirect)
  } catch (error: any) {
    console.error('登录失败:', error)
    ElMessage.error(error.message || '登录失败，请检查用户名和密码')
  } finally {
    loading.value = false
  }
}
</script>
```

#### 2. API接口

**文件位置：** `frontend/src/api/auth.ts`

```typescript
import { http } from '@/utils/request'
import type { LoginRequest, LoginResponse, RegisterRequest } from '@/types'

export const authApi = {
  /**
   * 用户登录
   */
  login: (data: LoginRequest) => {
    return http.post<LoginResponse>('/v1/auth/login', data)
  },

  /**
   * 用户注册
   */
  register: (data: RegisterRequest) => {
    return http.post<LoginResponse>('/v1/auth/register', data)
  },

  /**
   * 刷新Token
   */
  refreshToken: () => {
    return http.post<{ token: string }>('/v1/auth/refresh')
  },

  /**
   * 登出
   */
  logout: () => {
    return http.post<void>('/v1/auth/logout')
  },

  /**
   * 获取当前用户信息
   */
  getCurrentUser: () => {
    return http.get<User>('/v1/users/me')
  }
}
```

## 🧪 测试指南

### 1. 后端单元测试

```java
@SpringBootTest
@Transactional
class AuthServiceTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Test
    @Rollback
    void shouldLoginSuccessfully() {
        // Given
        LoginRequest request = new LoginRequest("admin", "password123");

        // When
        LoginResponse response = authService.login(request);

        // Then
        assertNotNull(response.getToken());
        assertEquals("admin", response.getUser().getUsername());
    }

    @Test
    @Rollback
    void shouldThrowExceptionWhenInvalidPassword() {
        // Given
        LoginRequest request = new LoginRequest("admin", "wrongpassword");

        // When & Then
        assertThrows(UnauthorizedException.class, () -> {
            authService.login(request);
        });
    }
}
```

### 2. 前端组件测试

```typescript
import { describe, it, expect, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import Login from '@/views/auth/Login.vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'

describe('Login Component', () => {
  it('renders login form', () => {
    const wrapper = mount(Login, {
      global: {
        plugins: [ElementPlus],
        stubs: {
          'router-link': true
        }
      }
    })

    expect(wrapper.find('.login-title').text()).toBe('企业知识库')
    expect(wrapper.find('input[type="text"]').exists()).toBe(true)
    expect(wrapper.find('input[type="password"]').exists()).toBe(true)
  })

  it('validates username input', async () => {
    const wrapper = mount(Login, {
      global: {
        plugins: [ElementPlus]
      }
    })

    const usernameInput = wrapper.find('input[type="text"]')
    await usernameInput.setValue('ab') // 少于3个字符

    // 触发验证
    // expect error message to be shown
  })
})
```

### 3. API集成测试

```typescript
import { describe, it, expect, beforeAll } from 'vitest'
import { authApi } from '@/api/auth'

describe('Auth API', () => {
  beforeAll(() => {
    // 启动测试服务器
  })

  it('should login successfully', async () => {
    const response = await authApi.login({
      username: 'admin',
      password: 'password123'
    })

    expect(response.code).toBe(200)
    expect(response.data.token).toBeDefined()
    expect(response.data.user.username).toBe('admin')
  })
})
```

## 📚 相关文件清单

### 后端文件
```
backend/src/main/java/com/company/kb/
├── config/
│   └── SecurityConfig.java
├── controller/
│   └── AuthController.java
├── service/
│   └── AuthService.java
├── repository/
│   └── UserRepository.java
├── entity/
│   └── User.java
├── dto/
│   ├── request/
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   └── ChangePasswordRequest.java
│   └── response/
│       ├── LoginResponse.java
│       └── UserResponse.java
└── security/
    ├── JwtProvider.java
    ├── UserDetailsServiceImpl.java
    ├── UserPrincipal.java
    └── JwtAuthenticationFilter.java
```

### 前端文件
```
frontend/src/
├── views/
│   └── auth/
│       └── Login.vue
├── stores/
│   └── user.ts
├── api/
│   └── auth.ts
├── types/
│   └── user.ts
└── router/
    └── index.ts
```

## 🚀 部署检查清单

### 开发环境
- [ ] 数据库初始化脚本执行
- [ ] Redis服务启动
- [ ] 环境变量配置完成
- [ ] 前端开发服务器启动
- [ ] 后端服务启动

### 功能测试
- [ ] 用户登录功能正常
- [ ] 用户注册功能正常
- [ ] Token刷新机制正常
- [ ] 权限控制正常
- [ ] 错误处理正常

### 安全检查
- [ ] 密码加密存储
- [ ] Token使用HTTPS传输
- [ ] 登录失败限制生效
- [ ] SQL注入防护
- [ ] XSS防护

---

**文档版本：** v1.0  
**创建日期：** 2026-05-31  
**最后更新：** 2026-05-31  
**状态：** 待审核

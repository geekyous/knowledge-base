# Flyway 数据库迁移指南

> 本文档介绍 Flyway 在本项目中的集成方式、使用方法和常见问题处理。

---

## 一、Flyway 是什么？

Flyway 是一款开源的**数据库版本迁移工具**，核心思想是：

```
应用的代码有 Git 管理 → 数据库的结构谁来管？→ Flyway
```

它能做到：

| 能力 | 说明 |
|------|------|
| **版本追踪** | 自动记录哪些迁移脚本已执行（`flyway_schema_history` 表） |
| **增量迁移** | 只执行新增的迁移脚本，已执行过的不会重复 |
| **多环境一致** | 开发、测试、生产共用同一套脚本，应用启动时自动迁移 |
| **团队协作** | 新成员拉取代码后，启动应用即可获得最新表结构 |

### 为什么不用 Docker initdb？

| 维度 | Docker initdb | Flyway |
|------|--------------|--------|
| 触发时机 | 仅首次创建容器时 | **每次应用启动时检查** |
| 增量迁移 | ❌ 无法加新脚本 | ✅ 新增 `V3__xxx.sql` 自动执行 |
| 版本记录 | ❌ 无 | ✅ `flyway_schema_history` 表 |
| 本地开发 | ❌ 必须用 Docker | ✅ 直接连接 MySQL 即可 |
| 生产部署 | ❌ 需手动建表 | ✅ 应用启动自动迁移 |

---

## 二、命名规则

迁移脚本放在 `backend/src/main/resources/db/migration/` 目录下。

### 版本迁移（Versioned Migration）

```
V<版本号>__<描述>.sql
```

| 规则 | 示例 |
|------|------|
| `V` 大写 | `V1__init_schema.sql` |
| 版本号用数字，可含小数点 | `V1`、`V2`、`V2.1` |
| **两个下划线** 分隔版本和描述 | `V3__add_user_phone.sql` |
| 描述用小写 + 下划线 | `V4__create_index_on_email.sql` |
| 只执行一次，不可修改已执行的脚本 | — |

### 可重复迁移（Repeatable Migration）

```
R__<描述>.sql
```

- 每次脚本内容变化时重新执行
- 适合视图、存储过程等
- 本项目暂未使用

---

## 三、项目迁移目录结构

```
backend/src/main/resources/db/migration/
├── V1__init_schema.sql      ← 建表（12 张表 + 初始角色）
└── V2__seed_data.sql        ← 种子数据（分类、标签、文档、对话等）
```

**注意**：初始用户数据由 `DataInitializer`（Java 代码）创建，不在 SQL 中硬编码密码。

---

## 四、常用操作

### 4.1 新建迁移脚本

当需要修改数据库结构时（加字段、建索引、加表等），创建新的迁移文件：

```bash
# 示例：给用户表添加手机号字段
# 在 backend/src/main/resources/db/migration/ 下创建：
touch backend/src/main/resources/db/migration/V3__add_user_nickname.sql
```

文件内容：

```sql
-- V3: 用户表添加昵称字段
ALTER TABLE kb_users ADD COLUMN nickname VARCHAR(50) COMMENT '昵称' AFTER username;

-- 添加索引
ALTER TABLE kb_users ADD INDEX idx_nickname (nickname);
```

下次启动应用时，Flyway 会自动执行 V3。

### 4.2 查看迁移历史

连接 MySQL 后查询：

```sql
SELECT installed_rank, version, description, type, script, installed_on, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

输出示例：

| installed_rank | version | description | success | installed_on |
|---------------|---------|-------------|---------|-------------|
| 1 | 1 | init schema | 1 | 2026-06-08 10:00:00 |
| 2 | 2 | seed data | 1 | 2026-06-08 10:00:01 |

### 4.3 手动修复失败的迁移

如果迁移脚本执行失败（比如 SQL 语法错误），Flyway 会锁住，后续迁移无法执行。

**修复步骤**：

```sql
-- 1. 查看失败的迁移
SELECT * FROM flyway_schema_history WHERE success = 0;

-- 2. 手动修复问题（如删除已创建的半成品表）
-- 根据具体错误处理...

-- 3. 删除失败记录（让 Flyway 重新执行）
DELETE FROM flyway_schema_history WHERE success = 0;

-- 4. 修正 SQL 脚本后重启应用
```

### 4.4 已有数据库的 baseline

如果数据库已经存在（比如从 Docker initdb 方案迁移过来），Flyway 需要 "打基线"：

```yaml
# application.yml 中已配置：
spring:
  flyway:
    baseline-on-migrate: true    # 已有数据库时自动打基线
    baseline-version: 0          # 基线版本号
```

这样 Flyway 会跳过 V1、V2（因为表已存在），后续新增的 V3+ 会正常执行。

---

## 五、Spring Boot 集成配置

本项目配置位于 `application.yml`：

```yaml
spring:
  flyway:
    enabled: true                  # 启用 Flyway
    locations: classpath:db/migration  # 迁移脚本位置
    baseline-on-migrate: true      # 已有数据库时自动 baseline
    baseline-version: 0            # baseline 版本号
    encoding: UTF-8                # 脚本编码
    validate-on-migrate: true      # 迁移前校验已执行脚本是否被篡改
```

### 执行顺序

```
Spring Boot 启动
    ↓
Flyway 扫描 db/migration/ 目录
    ↓
对比 flyway_schema_history 表
    ↓
执行未运行的迁移脚本（按版本号顺序）
    ↓
DataInitializer.run() 检查用户表是否为空，为空则创建初始用户
    ↓
应用就绪
```

---

## 六、开发流程

### 新增数据库变更的标准流程

```
1. 创建迁移脚本 V{N}__{描述}.sql
2. 本地启动应用，验证 Flyway 执行成功
3. 提交代码（迁移脚本 + 关联代码变更）
4. CI/CD 部署时自动执行迁移
```

### ⚠️ 注意事项

| 规则 | 原因 |
|------|------|
| **已执行的脚本不可修改** | Flyway 会校验 checksum，修改后启动报错 |
| **迁移脚本只增不删** | 生产数据安全，不 DROP 表/列 |
| **不写业务逻辑** | 迁移脚本只管 DDL 和必要的初始化数据 |
| **脚本要幂等友好** | 避免依赖特定数据状态 |

---

## 七、常见问题

### Q1: 启动报 "Migration checksum mismatch"

**原因**：已执行的迁移脚本被修改了。

**解决**：
- 恢复脚本原始内容，或
- 使用 `flyway repair` 修复 checksum：在开发环境可删除 `flyway_schema_history` 中对应记录

### Q2: 已有数据库，首次接入 Flyway 怎么办？

配置中已设置 `baseline-on-migrate: true`，Flyway 会：
1. 检测到数据库已存在
2. 自动插入一条 baseline 记录（version=0）
3. 执行 version > 0 的所有未运行迁移

### Q3: 如何在本地开发环境重置数据库？

```bash
# 方式1：删除 MySQL volume 后重启（Docker 环境）
docker compose down -v    # 删除所有 named volumes
docker compose up -d       # 重新启动，Flyway 从头执行所有迁移

# 方式2：只清空 Flyway 历史（保留容器）
docker compose exec mysql mysql -ukb_user -pkb_password knowledge_base \
  -e "DROP DATABASE knowledge_base; CREATE DATABASE knowledge_base;"
# 重启 backend 服务，Flyway 重新执行所有迁移
```

### Q4: 多个开发者同时提交迁移脚本怎么办？

Flyway 按版本号顺序执行。如果两个人都创建了 `V3__xxx.sql`：
- Git 会提示冲突，需要手动合并
- 解决方式：将后来的改为 `V4__xxx.sql`

建议：迁移脚本版本号使用整数递增（V3, V4, V5...），避免小数点版本。

---

## 八、Maven 依赖

本项目使用的 Flyway 依赖（版本由 Spring Boot BOM 管理，无需手动指定）：

```xml
<!-- Flyway 核心 -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
<!-- Flyway MySQL 支持（Spring Boot 3.x 需要） -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-mysql</artifactId>
</dependency>
```

---

**文档版本：** v1.0
**最后更新：** 2026-06-08
**相关文档：** [数据库设计](../06-database-design.md)、[Java 概念](java-concepts.md)、[项目宪法](../constitution.md)

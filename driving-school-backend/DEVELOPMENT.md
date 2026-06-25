# 驾校管理系统后端 — 开发文档

本文档面向参与 **driving-school-backend** 开发与部署的工程师，基于当前仓库代码整理。接口明细以 Swagger 为准。

---

## 项目简介

**驾校管理系统后端**（`com.drivingschool:driving-school-backend`）为单体 Spring Boot 应用，提供驾校业务 REST API，覆盖：

- 系统用户与认证（JWT）
- 学员、教练、车辆管理
- 课程配置与培训预约
- 考试安排与报名
- 收费与财务统计
- 运营数据统计与操作日志

前端项目未包含在本仓库中；本服务默认监听 `8080`，统一前缀为 `/api`。

---

## 技术栈

| 类别 | 技术 | 版本（pom 中声明） |
|------|------|-------------------|
| 语言 / 运行时 | Java | 21 |
| 框架 | Spring Boot | 3.2.5 |
| Web | spring-boot-starter-web | 随 Boot |
| 安全 | Spring Security + JWT (jjwt) | jjwt 0.12.5 |
| ORM | MyBatis-Plus | 3.5.6 |
| 数据库 | MySQL | mysql-connector-j（runtime） |
| 缓存 | Spring Data Redis | 随 Boot |
| 校验 | spring-boot-starter-validation | 随 Boot |
| AOP | spring-boot-starter-aop | 随 Boot |
| API 文档 | SpringDoc OpenAPI | 2.5.0 |
| 工具库 | Hutool、Lombok | 5.8.27 / 随 Boot |

---

## 项目结构

本仓库为 **单模块 Maven 项目**（非多模块），根目录仅一个 `pom.xml`。

```
driving-school-backend/
├── pom.xml                          # Maven 构建与依赖
├── DEVELOPMENT.md                   # 本文档
├── src/main/java/com/drivingschool/
│   ├── DrivingSchoolApplication.java # 启动类，@MapperScan("com.drivingschool.*.mapper")
│   ├── common/                      # 公共层
│   │   ├── config/                  # Security、JWT、CORS、MyBatis-Plus、Jackson、AOP 日志等
│   │   ├── constant/                # 业务常量（学时、及格分等）
│   │   ├── enums/                   # 角色、状态等枚举
│   │   ├── exception/               # BusinessException、全局异常处理
│   │   ├── result/                  # 统一响应 R、分页 PageResult
│   │   └── utils/                   # JwtUtils、RedisUtils
│   ├── system/                      # 认证、用户、操作日志
│   ├── student/                     # 学员
│   ├── coach/                       # 教练
│   ├── vehicle/                     # 车辆
│   ├── course/                      # 课程配置、培训预约（Lesson）
│   ├── exam/                        # 考试与报名
│   ├── finance/                     # 收费
│   └── statistics/                  # 数据统计
├── src/main/resources/
│   ├── application.yml              # 公共配置
│   ├── application-dev.yml          # 开发环境
│   ├── application-prod.yml         # 生产环境（支持环境变量）
│   ├── schema.sql                   # 库表 DDL + 初始数据
│   └── mapper/                      # 预留 MyBatis XML 目录（当前无自定义 XML）
└── src/test/java/                   # 测试目录（当前无测试类）
```

### 业务包分层约定

每个业务域通常包含：

| 包 | 职责 |
|----|------|
| `controller` | REST 接口，`@Tag` + `@Operation` 供 Swagger |
| `service` | 业务逻辑 |
| `mapper` | MyBatis-Plus `BaseMapper` |
| `entity` | 表实体，`@TableName` / `@TableLogic` |
| `dto` | 查询、创建等入参（部分模块有） |

---

## 环境要求

| 组件 | 要求 |
|------|------|
| JDK | **21**（与 `pom.xml` 中 `java.version` 一致） |
| Maven | 3.8+（推荐 3.9+） |
| MySQL | 5.7+ / 8.x，字符集 `utf8mb4` |
| Redis | 用于 Token 黑名单等；开发环境可本地启动 |
| IDE | IntelliJ IDEA / Eclipse 等（仓库含 `.idea`，可按团队规范忽略） |

---

## 快速开始

### 1. 克隆与进入目录

```bash
git clone <你的仓库地址>
cd driving-school-backend
```

### 2. 初始化数据库

在 MySQL 中执行初始化脚本（会创建库 `driving_school` 并插入演示用户与课程）：

```bash
mysql -u root -p < src/main/resources/schema.sql
```

或在客户端中打开并执行 `src/main/resources/schema.sql`。

### 3. 配置开发环境

编辑 `src/main/resources/application-dev.yml`，按本地环境修改数据源与 Redis（**勿将真实密码提交到版本库**）：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/driving_school?...
    username: root
    password: <你的密码>
  data:
    redis:
      host: localhost
      port: 6379
      password:        # 无密码可留空
      database: 0

jwt:
  secret: <足够长的 HS256 密钥>
```

默认激活 profile 为 `dev`（见 `application.yml` 中 `spring.profiles.active`）。

### 4. 编译与运行

```bash
# 编译
mvn clean package -DskipTests

# 运行（开发）
mvn spring-boot:run

# 或运行打好的 jar
java -jar target/driving-school-backend-1.0.0.jar
```

### 5. 验证

| 项 | 地址 |
|----|------|
| 服务根路径 | `http://localhost:8080/api` |
| Swagger UI | `http://localhost:8080/api/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/api/v3/api-docs` |

登录示例（需 `Content-Type: application/json`）：

```http
POST /api/auth/login
{
  "username": "your_username",
  "password": "your_password"
}
```

响应 `data` 中含 `accessToken`、`refreshToken`、`expiresIn`（秒）。

---

## 配置说明

### 公共配置（`application.yml`）

| 配置项 | 说明 |
|--------|------|
| `server.port` | 默认 `8080` |
| `server.servlet.context-path` | 统一前缀 `/api` |
| `spring.profiles.active` | 默认 `dev` |
| `spring.jackson.date-format` | `yyyy-MM-dd HH:mm:ss`，时区 `Asia/Shanghai` |
| `mybatis-plus.global-config.db-config` | 雪花 ID、`deleted` 逻辑删除 |
| `springdoc.*` | Swagger UI 与 api-docs 路径 |
| `avatar.upload-dir` | 头像本地目录，默认 `./avatars` |
| `avatar.base-url` | 头像访问前缀，默认 `/api/avatars` |

### 开发环境（`application-dev.yml`）

- MySQL、Redis、JWT 密钥与过期时间写在本文件。
- MyBatis SQL 日志：`StdOutImpl`（控制台打印 SQL）。

### 生产环境（`application-prod.yml`）

支持环境变量覆盖，示例：

| 环境变量 | 含义 | 默认值 |
|----------|------|--------|
| `DB_HOST` | 数据库主机 | `localhost` |
| `DB_PORT` | 端口 | `3306` |
| `DB_NAME` | 库名 | `driving_school` |
| `DB_USER` | 用户名 | `root` |
| `DB_PASS` | 密码 | `root` |
| `REDIS_HOST` | Redis 主机 | `localhost` |
| `REDIS_PORT` | Redis 端口 | `6379` |
| `REDIS_PASS` | Redis 密码 | 空 |
| `JWT_SECRET` | JWT 签名密钥 | 内置占位（**生产必须更换**） |

JWT 过期时间（毫秒，两环境一致）：

| 配置项 | 值 | 含义 |
|--------|-----|------|
| `jwt.access-token-expiration` | `7200000` | Access Token 约 2 小时 |
| `jwt.refresh-token-expiration` | `604800000` | Refresh Token 约 7 天 |

启动生产配置：

```bash
java -jar driving-school-backend-1.0.0.jar --spring.profiles.active=prod
```

---

## 数据库

### 库名

`driving_school`（`utf8mb4`）

### 表一览

| 表名 | 说明 |
|------|------|
| `sys_user` | 系统用户（角色、状态、逻辑删除） |
| `sys_log` | 操作日志（AOP 写入，无逻辑删除） |
| `student` | 学员档案 |
| `coach` | 教练 |
| `vehicle` | 车辆 |
| `course` | 科目/课程配置（学时、单价） |
| `lesson` | 培训预约 |
| `exam` | 考试场次 |
| `exam_enrollment` | 考试报名与成绩 |
| `payment` | 收费记录 |

### 初始化脚本

路径：`src/main/resources/schema.sql`

- 建库、建表、索引
- 插入 4 个演示账号（见 [附录：默认账号](#附录)）
- 插入 C1/C2 科目课程配置

### 迁移说明

项目 **未使用** Flyway/Liquibase；结构变更需手动维护 `schema.sql` 或在数据库执行 DDL。业务表普遍含 `deleted` 字段，由 MyBatis-Plus 全局逻辑删除处理。

---

## 分层架构说明

```
HTTP 请求
    ↓
CorsFilter → JwtAuthenticationFilter → Spring Security 授权
    ↓
Controller（@PreAuthorize 方法级权限）
    ↓
Service（业务规则、事务）
    ↓
Mapper（MyBatis-Plus BaseMapper / 条件构造）
    ↓
MySQL
```

**横切能力：**

- **统一响应**：`R<T>`，`code=200` 表示成功；业务失败常用 `BusinessException` 或 `R.fail(code, message)`。
- **分页**：MyBatis-Plus `Page` + 封装 `PageResult`（`records/total/pageNum/pageSize/pages`）。
- **主键**：`IdType.ASSIGN_ID`（雪花）。
- **时间字段**：`createTime` / `updateTime` 由 `MetaObjectHandler` 自动填充。
- **Long 序列化**：Jackson 将 `Long` 转为字符串，避免前端精度丢失。
- **操作日志**：带 `@Operation` 的接口由 `LogAspect` 记录到 `sys_log`（异步线程池 `async-log-`）。

---

## API 说明

> 以下路径均相对于 **context-path**：实际 URL = `http://host:port/api` + 路径。  
> 除白名单外，请求头需携带：`Authorization: Bearer <accessToken>`。

### 白名单（无需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/auth/login` | 登录 |
| POST | `/auth/refresh` | 刷新 Token |
| GET | `/swagger-ui/**`、`/v3/api-docs/**`、`/swagger-ui.html` | API 文档 |
| GET | `/avatars/**` | 头像静态资源 |

### 认证与用户（`system`）

| 前缀 | 主要接口 | 权限要点 |
|------|----------|----------|
| `/auth` | `login`、`refresh`、`logout`、`info`、`change-password`、`avatar` | 登录/刷新公开；其余需认证 |
| `/users` | 用户 CRUD、重置密码、启用/禁用 | `@PreAuthorize("hasRole('ADMIN')")` |
| `/logs` | 操作日志分页查询 | 仅 `ADMIN` |

### 学员（`/students`）

| 接口 | 说明 | 角色 |
|------|------|------|
| `GET /me` | 当前学员信息 | `STUDENT` |
| `PUT /me` | 学员改个人信息 | `STUDENT` |
| `GET`、`POST`、`PUT /{id}`、`PUT /{id}/status` | 列表、报名、修改、状态 | `ADMIN` / `STAFF`（写操作） |
| `GET /{id}`、`/{id}/coach`、`/{id}/progress`、`/{id}/timeline` | 详情、教练、进度、时间线 | 已认证（部分列表需前台） |

### 教练（`/coaches`）

| 接口 | 说明 | 角色 |
|------|------|------|
| `GET /me`、`GET /me/students` | 当前教练与名下学员 | `COACH` |
| `GET`、`GET /{id}`、`GET /{id}/schedule`、`GET /{id}/students` | 查询、课表、学员 | 已认证 |
| `POST`、`PUT /{id}`、`PUT /{id}/status` | 增删改、状态 | `ADMIN` |

### 车辆（`/vehicles`）

列表、详情、可用车辆查询；创建/修改/状态变更仅 `ADMIN`。

### 课程（`/courses`）

科目配置列表与详情（只读接口，无方法级 `@PreAuthorize`）。

### 培训预约（`/lessons`）

| 接口 | 说明 | 角色 |
|------|------|------|
| `GET` | 分页列表 | 已认证 |
| `POST` | 创建预约 | `STUDENT` / `STAFF` / `ADMIN` |
| `PUT /{id}/confirm`、`complete`、`confirm-complete` | 确认/完成课时 | `COACH` / `STAFF` / `ADMIN` |
| `PUT /{id}/cancel` | 取消 | 多角色 |
| `GET /calendar`、`GET /available-slots` | 日历、空闲时段 | 已认证 |

### 考试（`/exams`）

| 接口 | 说明 | 角色 |
|------|------|------|
| `GET` | 考试安排列表 | 已认证 |
| `POST` | 创建场次 | `STAFF` / `ADMIN` |
| `POST /{examId}/enroll` | 报名 | `STUDENT` / `STAFF` / `ADMIN` |
| `PUT /enrollments/{id}/result` | 录入成绩 | `STAFF` / `ADMIN` |
| `GET /enrollments`、`GET /students/{id}/exam-history` | 报名列表、学员历史 | 已认证 |

考试类型字段示例：`SUBJECT_1` ~ `SUBJECT_4`（见实体与业务代码）。

### 财务（`/finance`）

| 接口 | 说明 |
|------|------|
| `GET /payments` | 收费分页（多条件筛选） |
| `POST /payments` | 新增收费（操作人取自当前用户） |
| `GET /payments/student/{id}` | 学员缴费记录 |
| `GET /summary`、`GET /daily` | 汇总、日报 |

> 财务接口未标注 `@PreAuthorize`，仅需登录；若需收紧权限可在后续迭代中补充。

### 数据统计（`/statistics`）

| 接口 | 说明 |
|------|------|
| `GET /overview` | 概览 |
| `GET /revenue?period=month` | 收入趋势 |
| `GET /enrollment` | 报名趋势 |
| `GET /pass-rate?groupBy=subject` | 通过率 |
| `GET /coach-ranking` | 教练排行 |
| `GET /vehicle-usage` | 车辆使用率 |

完整参数与响应模型请访问 **Swagger UI**。

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { }
}
```

常见 `code`：`200` 成功，`400` 参数校验，`401` 未认证/凭证错误，`403` 无权限，`500` 业务或系统错误。

---

## 认证与权限

### 认证流程

1. `POST /auth/login`：Spring Security `AuthenticationManager` 校验用户名密码（BCrypt）。
2. 签发 **Access Token**（含 `userId`、`username`、`role`、`type=access`）与 **Refresh Token**（`type=refresh`）。
3. 请求携带 `Authorization: Bearer <accessToken>`，`JwtAuthenticationFilter` 解析并设置 `SecurityContext`。
4. `POST /auth/logout`：将 access token 写入 Redis 黑名单（key：`token:blacklist:<token>`，TTL 与 access 过期一致）。
5. `POST /auth/refresh`：校验 refresh token 后重新签发一对 token。

### 角色

定义于 `UserRole` 枚举，库表 `sys_user.role` 与 JWT 中 `role` 一致：

| 角色代码 | 说明 |
|----------|------|
| `ADMIN` | 管理员 |
| `STAFF` | 前台 |
| `COACH` | 教练 |
| `STUDENT` | 学员 |

Spring Security 使用 `ROLE_` 前缀，例如 `@PreAuthorize("hasRole('ADMIN')")` 对应库中 `ADMIN`。

### 安全相关配置

- **无 Session**：`SessionCreationPolicy.STATELESS`
- **CSRF**：关闭（前后端分离 + JWT）
- **CORS**：`CorsConfig` 允许凭证、任意源模式、常用方法与头
- **未认证 / 无权限**：返回 JSON 体 `R.fail(401/403, ...)`

### Redis 用途

- Token 黑名单（登出）
- `RedisUtils` 亦提供通用 KV；`Constants.RATE_LIMIT_PREFIX` 预留限流前缀（按代码检索实际使用情况）

---

## 开发规范与代码约定

根据现有代码归纳，新代码建议保持一致：

1. **包命名**：`com.drivingschool.<域>.<layer>`，域名与业务一致。
2. **Controller**：`@RestController` + `@RequestMapping` + `@RequiredArgsConstructor` 构造注入；Swagger `@Tag` / `@Operation`。
3. **权限**：敏感写操作使用 `@PreAuthorize`；`Authentication.getPrincipal()` 为 `Long` 类型用户 ID。
4. **入参校验**：DTO 使用 `@Valid` + Jakarta Validation 注解；校验失败由 `GlobalExceptionHandler` 返回 400。
5. **业务异常**：抛出 `BusinessException`，勿直接返回 500 堆栈给前端。
6. **实体**：`@TableName`、`@TableId(type = IdType.ASSIGN_ID)`、逻辑删除字段 `deleted` + `@TableLogic`。
7. **时间类型**：`LocalDateTime` / `LocalDate` / `LocalTime`；接口日期参数常用 `@DateTimeFormat(pattern = "yyyy-MM-dd")`。
8. **分页参数**：多数列表默认 `pageNum=1`、`pageSize=10`。
9. **常量**：跨模块业务规则放在 `Constants`（如每教练最大学员数、科目及格分等）。
10. **不要**在仓库中提交真实数据库密码、JWT 生产密钥。

---

## 构建与打包

```bash
# 完整构建（含测试，当前无测试用例）
mvn clean package

# 跳过测试
mvn clean package -DskipTests
```

产物：`target/driving-school-backend-1.0.0.jar`（Spring Boot 可执行 jar）。

生产部署建议：

- 使用 `prod` profile 并通过环境变量注入密钥与数据库配置
- 前置 Nginx 反向代理并配置 HTTPS
- 头像目录 `avatar.upload-dir` 需持久化或使用对象存储（当前为本地磁盘）

---

## 测试

- 依赖：`spring-boot-starter-test`、`spring-security-test`（test scope）
- 现状：`src/test/java` 下 **无** 单元/集成测试类
- 本地验证：Swagger UI、Postman，或 `mvn spring-boot:run` 后调用 `/api/auth/login` 与各业务接口

添加测试时建议从 `AuthController`、`StudentService` 等核心路径开始，使用 `@SpringBootTest` + Testcontainers（MySQL/Redis）或嵌入式替代方案。

---

## 常见问题

### 启动报数据库连接失败

- 确认 MySQL 已启动且已执行 `schema.sql`
- 检查 `application-dev.yml` 中 URL、用户名、密码
- URL 需带 `serverTimezone=Asia/Shanghai` 等参数（脚本与 dev 配置已示例）

### 启动报 Redis 连接失败

- 本地启动 Redis，或临时注释/排除 Redis 相关自动配置（**不推荐**长期这样做，登出黑名单依赖 Redis）
- `JwtAuthenticationFilter` 在 Redis 不可用时会跳过黑名单检查并打 warn 日志

### 401 未认证

- 确认请求 URL 包含 `/api` 前缀
- Header：`Authorization: Bearer <token>`（注意 `Bearer` 后有空格）
- Access Token 过期后用 `refreshToken` 调 `/auth/refresh`

### 403 无权限

- 核对用户 `sys_user.role` 与接口 `@PreAuthorize` 要求
- 账号 `status=0` 时登录会被拒绝

### Swagger 打不开

- 地址：`http://localhost:8080/api/swagger-ui.html`（不是根路径 `/swagger-ui.html`）
- Security 已放行相关路径

### 前端 Long 精度问题

- 后端已将 `Long` 序列化为字符串，前端按字符串处理 ID 即可

### SQL 日志过多

- dev 环境开启了 `StdOutImpl`；生产 profile 使用 `NoLoggingImpl`

---

## 附录

### 端口与路径

| 项 | 值 |
|----|-----|
| HTTP 端口 | `8080` |
| Context Path | `/api` |
| Swagger | `/api/swagger-ui.html` |

### 业务常量摘录（`Constants`）

| 常量 | 值 | 含义 |
|------|-----|------|
| `MAX_STUDENTS_PER_COACH` | 30 | 每教练学员上限 |
| `MAX_LESSONS_PER_DAY` | 4 | 每日预约上限 |
| `CANCEL_HOURS_BEFORE` | 2 | 取消需提前小时数 |
| `SUBJECT_1_PASS_SCORE` 等 | 90/80/90/90 | 各科目及格线 |

### 枚举与状态码

- 学员状态：`StudentStatus`（待审核、在学、毕业、退学等）
- 课时状态：`LessonStatus`（待确认、已确认、进行中、已完成、已取消）
- 考试报名状态：`ExamEnrollStatus`
- 支付类型/方式：`PaymentType`、`PayMethod`
- 车辆/教练状态：见对应 `enum` 包

具体数值以枚举类与数据库字段注释为准。

---

## 文档缺口说明

以下内容在仓库中 **尚未提供**，本文档无法进一步细化：

| 缺口 | 说明 |
|------|------|
| README | 根目录无 `README.md` |
| 自动化测试 | `src/test` 无测试代码 |
| CI/CD | 无 GitHub Actions、Jenkinsfile 等 |
| Docker / K8s | 无容器化与编排清单 |
| 数据库版本迁移 | 仅 `schema.sql`，无 Flyway/Liquibase |
| 自定义 MyBatis XML | `mapper/` 目录为空，全部为 Plus 注解/Wrapper |
| 前端联调文档 | 需结合前端项目单独维护 |
| 生产监控与日志规范 | 未在代码中约定 |

如有上述交付物，建议补充到 `docs/` 或根目录 README 并链接到本文档。

---

*文档生成依据仓库当前源码与配置；变更代码后请同步更新本节相关描述。*

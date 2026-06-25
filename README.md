# 驾校管理系统

基于 Spring Boot + Vue 3 + uni-app 的全栈驾校管理系统，包含 PC 管理后台和微信小程序两端。

## 📋 项目简介

本系统面向驾校日常运营管理，覆盖学员报名、培训预约、考试管理、财务收费、数据统计等核心业务流程。支持管理员、前台、教练、学员四种角色的差异化权限控制。

## 🛠️ 技术栈

### 后端
- **框架**: Spring Boot 3.2.5 + Java 21
- **ORM**: MyBatis-Plus 3.5.6
- **数据库**: MySQL 8.0
- **缓存**: Redis
- **认证**: JWT + Spring Security (RBAC)
- **文档**: SpringDoc OpenAPI (Swagger UI)

### 前端
- **框架**: Vue 3 + Vite
- **UI 组件**: Element Plus
- **图表**: ECharts
- **状态管理**: Pinia
- **HTTP**: Axios

### 小程序
- **框架**: uni-app (Vue 3 + Vite)
- **平台**: 微信 / 支付宝 / 百度

## 🚀 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+
- MySQL 8.0
- Redis
- Node.js 18+

### 一键启动（Windows）

```bash
# 自动检测 IP、更新配置、启动后端和前端
start.bat

# 停止所有服务
stop.bat
```

### 手动启动

#### 1. 初始化数据库

```bash
mysql -u root -p < driving-school-backend/src/main/resources/schema.sql
```

#### 2. 启动后端

```bash
cd driving-school-backend
mvn spring-boot:run
```

后端启动后访问: http://localhost:8080/api

#### 3. 启动前端

```bash
cd driving-school-frontend
npm install
npm run dev
```

前端访问: http://localhost:3000

#### 4. 启动小程序

```bash
cd driving-school-miniapp
npm install
npm run dev:mp-weixin
```

用微信开发者工具导入 `dist/dev/mp-weixin` 目录。

### 默认账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | admin123 |
| 前台 | staff | staff123 |
| 教练 | coach | coach123 |
| 学员 | student | student123 |

## 📁 项目结构

```
driving-school/
├── driving-school-backend/     # Spring Boot 后端
│   └── src/main/java/com/drivingschool/
│       ├── system/             # 系统管理（用户、认证、日志）
│       ├── student/            # 学员管理
│       ├── coach/              # 教练管理
│       ├── course/             # 课程管理
│       ├── exam/               # 考试管理
│       ├── finance/            # 财务管理
│       ├── vehicle/            # 车辆管理
│       ├── contract/           # 合同管理
│       ├── notification/       # 通知管理
│       ├── statistics/         # 数据统计
│       └── common/             # 公共模块（配置、工具、异常）
├── driving-school-frontend/    # Vue 3 PC 管理后台
│   └── src/
│       ├── api/                # API 接口
│       ├── views/              # 页面组件
│       ├── store/              # 状态管理
│       └── router/             # 路由配置
├── driving-school-miniapp/     # uni-app 小程序
│   └── src/
│       ├── pages/              # 页面
│       ├── store/              # 状态管理
│       └── utils/              # 工具函数
├── start.bat                   # 一键启动
├── stop.bat                    # 一键停止
└── schema.sql                  # 数据库初始化脚本
```

## 🏗️ 系统架构

```
浏览器 → Vite Dev Proxy (:3000) → 后端 (:8080/api)
                                        ↓
微信小程序 ──────────────────→  CORS → 频率限制 → JWT 认证 → Spring Security → Controller → Service → Mapper → MySQL
```

## 📊 功能模块

| 模块 | 功能 |
|------|------|
| 学员管理 | 报名登记、状态变更、学习进度、附件管理 |
| 教练管理 | 排班管理、学员分配、绩效考核、评价管理 |
| 课程管理 | 课程配置、培训预约、学时签到 |
| 考试管理 | 考试安排、考试报名、成绩录入 |
| 财务管理 | 收费管理、退费处理、分期付款、财务统计 |
| 数据统计 | 学员统计、收入趋势、通过率分析、教练排名 |
| 题库练习 | 科目一/四在线刷题、错题本、收藏夹 |
| 系统管理 | 用户管理、角色权限、操作日志 |

## 🔐 安全特性

- JWT 无状态认证 + Refresh Token 自动续期
- RBAC 四级角色权限控制（@PreAuthorize）
- 登录频率限制（Redis 分布式限流）
- Token 黑名单（登出即失效）
- BCrypt 密码加密
- SQL 注入防护（MyBatis-Plus 参数化查询）
- IDOR 漏洞防护（学员只能访问自己的数据）

## 📄 API 文档

后端启动后访问 Swagger UI:

```
http://localhost:8080/api/swagger-ui.html
```

## 📝 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| DB_HOST | MySQL 主机 | localhost |
| DB_PORT | MySQL 端口 | 3306 |
| DB_NAME | 数据库名 | driving_school |
| DB_USER | MySQL 用户名 | root |
| DB_PASS | MySQL 密码 | 123456 |
| REDIS_HOST | Redis 主机 | localhost |
| REDIS_PORT | Redis 端口 | 6379 |
| JWT_SECRET | JWT 密钥 | 开发环境默认值 |

## License

MIT

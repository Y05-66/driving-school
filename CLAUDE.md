# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Monorepo for a driving school management system (驾校管理系统). Three independent projects:

- **driving-school-backend** — Spring Boot 3.2.5 monolith (Java 21, Maven). REST API at context path `/api`, port 8080. Detailed backend docs in `driving-school-backend/CLAUDE.md`.
- **driving-school-frontend** — Vue 3 + Vite SPA. Port 3000 in dev, proxies `/api` to backend.
- **driving-school-miniapp** — uni-app WeChat/Alipay/Baidu mini-program. Calls backend directly via HTTP (no proxy).

## Build & Run Commands

### One-click start/stop (Windows)

```bash
start.bat    # Auto-detects local IP, updates IPs in config files, builds miniapp, starts backend + frontend
stop.bat     # Kills processes on ports 8080 and 3000
update-ip.bat # Updates hardcoded IP addresses in miniapp config.js + frontend vite.config.js, rebuilds miniapp
```

`start.bat` detects the local LAN IP (prefers 192.168.x.x), patches it into `driving-school-miniapp/src/utils/config.js` and `driving-school-frontend/vite.config.js`, builds the miniapp, then launches backend and frontend in hidden windows. Logs go to `logs/backend.log` and `logs/frontend.log`.

### Backend

```bash
cd driving-school-backend
mvn clean package -DskipTests    # Build jar
mvn spring-boot:run              # Run with dev profile
```

Prerequisites: JDK 21, Maven 3.8+, MySQL, Redis. Initialize DB: `mysql -u root -p < src/main/resources/schema.sql`.

### Frontend

```bash
cd driving-school-frontend
npm install                      # Install dependencies
npm run dev                      # Dev server on port 3000
npm run build                    # Production build
```

No tests or linting configured in the frontend.

**Theme system:** 6 preset themes (default/blue/green/orange/rose/dark) applied via CSS custom properties on `document.documentElement`. Managed by `src/store/theme.js` Pinia store, persisted to localStorage.

### Miniapp

```bash
cd driving-school-miniapp
npm install                      # Install dependencies
npm run dev:mp-weixin            # Dev mode for WeChat mini-program
npm run build:mp-weixin          # Build for WeChat
```

## Architecture

```
Browser → Vite dev proxy (:3000) → Backend (:8080/api)
                                        ↓
WeChat Miniapp ──────────────────→  CorsFilter (global bean) → RateLimitFilter → JwtAuthenticationFilter → Spring Security → Controller → Service → Mapper → MySQL
```

Filter chain details: `RateLimitFilter` (login/registration rate limit, 10 req/min per IP) → `JwtAuthenticationFilter` (parses Bearer token) → Spring Security (role-based authorization). `CorsFilter` runs as a separate Servlet filter outside the Security chain. Registration endpoints (`POST /registrations`, `GET /registrations/status`) are public.

Database ER diagram: open `ER图预览.html` in a browser for a visual schema reference of all 28 tables and their relationships. Also useful: Swagger UI at `http://localhost:8080/api/swagger-ui.html` for interactive API exploration.

**Frontend build:** Vite splits chunks: `element-plus`, `echarts`, and `vendor` (vue/vue-router/pinia/axios) are separate bundles for faster loading.

### Backend (`driving-school-backend/`)

Single-module Spring Boot monolith. Package layout: `com.drivingschool.<domain>/{controller,service,mapper,entity,dto}`.

Domains and their API prefixes:

| Domain | Path prefix | Key entity |
|--------|-------------|------------|
| `system` | `/auth`, `/users`, `/logs` | `SysUser`, `SysLog` |
| `student` | `/students` | `Student`, `StudentProgress`, `StudentAttachment` |
| `coach` | `/coaches` | `Coach`, `CoachStudent`, `CoachEvaluation`, `CoachPerformance` |
| `vehicle` | `/vehicles` | `Vehicle`, `VehicleMaintenance` |
| `course` | `/courses`, `/lessons` | `Course`, `Lesson`, `TimeSlotTemplate` |
| `exam` | `/exams` | `Exam`, `ExamEnrollment` |
| `finance` | `/finance` | `Payment`, `InstallmentPlan`, `InstallmentDetail` |
| `statistics` | `/statistics` | (read-only aggregations) |
| `notification` | `/notifications` | `Notification` |
| `channel` | `/channels` | `Channel`, `StudentChannel` |
| `checkin` | `/checkins` | `CheckIn` |
| `contract` | `/contracts` | `Contract` |
| `practice` | `/practice` | `QuestionBank`, `PracticeRecord` |
| `registration` | `/registrations` | (public registration submission) |
| `reminder` | `/reminders` | `Reminder` |
| `survey` | `/surveys` | `SatisfactionSurvey` |
| `video` | `/videos` | (teaching video content) |

Cross-cutting in `common/`: `config/` (Security, JWT filter, RateLimit filter, CORS, Jackson, AOP, async, cache, scheduled tasks), `constant/Constants`, `enums/`, `exception/` (BusinessException + GlobalExceptionHandler), `result/` (R\<T\>, PageResult), `utils/` (JwtUtils, RedisUtils, ExcelUtils).

**Business constants** (`common/constant/Constants.java`): `MAX_STUDENTS_PER_COACH=30`, `MAX_LESSONS_PER_DAY=4`, `CANCEL_HOURS_BEFORE=2`, `MAX_RETAKE_COUNT=5`, `RETAKE_INTERVAL_DAYS=10`, pass scores: subject 1/3/4 = 90, subject 2 = 80.

**Database:** MySQL `driving_school`, 28 tables in `schema.sql`. All tables use BIGINT snowflake PKs, soft delete (`deleted` column), `createTime`/`updateTime` auto-filled. Default accounts: `admin`/`staff`/`coach`/`student` (BCrypt hashed passwords in seed data). MyBatis-Plus configured with `ASSIGN_ID` id-type, `deleted` as logic-delete field, underscore-to-camel-case mapping enabled. Schema file: `driving-school-backend/src/main/resources/schema.sql`. No migration tool (Flyway/Liquibase) — schema managed via this single file.

**IDs in DTOs/params:** Use `@DateTimeFormat(pattern = "yyyy-MM-dd")` for date params. Jackson `Long` → `String` conversion configured in `JacksonConfig`.

Full details in `driving-school-backend/CLAUDE.md`.

### Frontend (`driving-school-frontend/`)

Vue 3 Composition API (`<script setup>`) + Element Plus + Pinia + Vue Router 4 + ECharts + Axios.

**Key files:**
- `src/api/index.js` — All API calls in one file, grouped by domain
- `src/utils/request.js` — Axios instance with JWT interceptor and 401 auto-logout
- `src/store/user.js` — Pinia store: token/userInfo in localStorage, login/logout/fetchUserInfo
- `src/store/notification.js` — Unread notification count, polled every 60s
- `src/store/theme.js` — Theme state
- `src/router/index.js` — Routes with role-based guards (`meta.roles`)
- `src/components/Layout.vue` — Main shell: sidebar + header + router-view
- `src/components/ThemePicker.vue` — Theme switcher
- `src/views/` — Page components organized by domain

**Routing:** All business pages are children of `/` (Layout component). `/login` is public. Catch-all redirects to `/dashboard`. Role guards in `router.beforeEach` — if route has `meta.roles`, checks user role. On page refresh, re-fetches `userInfo` if store is empty (Pinia state lost, localStorage preserved). Routes with role restrictions: `channels` (ADMIN/STAFF), `contracts` (ADMIN/STAFF/STUDENT), `checkin` (ADMIN/STAFF/COACH), `registrations` (ADMIN/STAFF), `practice` (STUDENT), `survey` (STUDENT), `users`/`logs` (ADMIN only).

**API pattern:** Frontend calls `request.get/post/put/delete` which goes to `/api/...`. Backend returns `{ code: 200, data: ..., message: ... }`. Frontend response interceptor checks `code !== 200` as error. On 401, automatically attempts token refresh via `POST /api/auth/refresh` using stored `refreshToken`; if refresh fails, logs out and redirects to `/login`.

### Miniapp (`driving-school-miniapp/`)

uni-app (Vue 3 + Vite) targeting WeChat, Alipay, and Baidu mini-program platforms. Uses `uni.request` wrapper for HTTP.

**Key files:**
- `src/utils/config.js` — `BASE_URL` and `TIMEOUT` constants. **Hardcoded IP** — must be updated when backend IP changes.
- `src/utils/request.js` — HTTP client using `uni.request`, JWT auth, 401 auto-redirect to login.
- `src/utils/api.js` — API call definitions
- `src/store/user.js` — Pinia store with `uni.setStorageSync`/`getStorageSync`
- `src/store/notification.js` — Unread notification count
- `src/pages/` — Page components organized by domain
- `src/pages.json` — uni-app page routes and tab bar config

**Tab bar** (5 tabs): 首页 (`pages/index/index`), 学车 (`pages/learn/index`), 预约 (`pages/booking/index`), 消息 (`pages/message/index`), 我的 (`pages/profile/index`). Additional non-tab pages: login, student progress, lesson list/book, exam list, practice/wrong questions, registration, coach schedule/students/lessons, video list/play, finance/payment, survey, reminder, notification list.

**Build targets:** WeChat (`dev:mp-weixin` / `build:mp-weixin`), Alipay (`dev:mp-alipay` / `build:mp-alipay`), Baidu (`dev:mp-baidu` / `build:mp-baidu`). Output goes to `dist/dev/mp-weixin/` (or `build` variant).

## Key Conventions

- **Auth:** JWT stateless. Token stored in localStorage (frontend) or `uni.setStorageSync` (miniapp). `Authorization: Bearer <token>` header. Roles: `ADMIN`, `STAFF`, `COACH`, `STUDENT` (enum `UserRole`). Use `@PreAuthorize("hasRole('ADMIN')")` for method-level security. `Authentication.getPrincipal()` returns `Long` userId.
- **Pagination:** MyBatis-Plus `Page` → `PageResult{records, total, pageNum, pageSize, pages}`. Params: `pageNum`, `pageSize`.
- **Entities:** `@TableName`, `@TableId(type = IdType.ASSIGN_ID)` (snowflake IDs), `@TableLogic` on `deleted` field. `createTime`/`updateTime` auto-filled by `MetaObjectHandler`. Jackson converts `Long` → `String` to avoid JS precision loss.
- **Validation:** DTOs use Jakarta Validation (`@Valid`); failures return 400 via GlobalExceptionHandler.
- **Logging:** `@Operation`-annotated endpoints are logged to `sys_log` by `LogAspect` (async via `async-log-` thread pool prefix).
- **Response:** All endpoints return `R<T>` — `{ code: 200, message: "success", data: ... }`. Codes: 200=success, 400=validation error, 401=unauthenticated, 403=forbidden, 500=business/system error. Throw `BusinessException` for business errors (GlobalExceptionHandler maps to appropriate HTTP codes).
- **Constructor injection:** Use `@RequiredArgsConstructor` (Lombok).
- **Swagger:** `@Tag` on controllers, `@Operation` on methods. UI at `/api/swagger-ui.html`, OpenAPI JSON at `/api/v3/api-docs`.
- **Frontend API:** All calls go through `src/api/index.js`. Never import axios directly in views.
- **Frontend state:** Pinia stores in `src/store/`. User store persists token+userInfo to localStorage.

## Testing

**No tests exist** in any of the three projects. Backend `pom.xml` includes `spring-boot-starter-test` and `spring-security-test` as dependencies but no test classes have been written. Frontend has no test framework configured.

## Dev Environment Notes

- Frontend vite proxy targets `http://localhost:8080` — update `vite.config.js` if backend runs elsewhere.
- Miniapp `BASE_URL` in `src/utils/config.js` must be updated manually when backend IP changes. Use `update-ip.bat` or `start.bat` to auto-detect and patch IPs in both miniapp config and frontend `vite.config.js`.
- Backend dev config in `src/main/resources/application-dev.yml`. Environment variable overrides: `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS`, `REDIS_HOST`, `REDIS_PORT`, `REDIS_PASS`, `JWT_SECRET`. Defaults: MySQL on localhost:3306, Redis on localhost:6379, password `123456`.
- Redis is required for token blacklist; app warns but runs without it.
- Max upload size: 2MB (configured in `application.yml`).

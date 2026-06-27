# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Driving school management system backend (`com.drivingschool:driving-school-backend`). Single-module Spring Boot 3.2.5 monolith serving REST APIs for: auth/users, students, coaches, vehicles, courses, lessons, exams, finance, statistics, notifications, channels, check-in, contracts, practice (question bank), registration, reminders, surveys, videos, and AI chat (DashScope/阿里云百炼). Context path: `/api`, default port `8080`.

## Build & Run Commands

```bash
mvn clean package -DskipTests    # Build jar
mvn spring-boot:run              # Run in dev mode (activates dev profile)
java -jar target/driving-school-backend-1.0.0.jar  # Run packaged jar
java -jar target/driving-school-backend-1.0.0.jar --spring.profiles.active=prod  # Production
```

No tests exist yet. `pom.xml` includes `spring-boot-starter-test` and `spring-security-test` as dependencies but no test classes have been written. Dependencies: `mvn dependency:resolve`.

## Prerequisites

- **JDK 21**, **Maven 3.8+**
- **MySQL** — initialize with `mysql -u root -p < src/main/resources/schema.sql` (creates `driving_school` db, 30 tables, seed data with 4 default users)
- **Redis** — required for token blacklist; app warns but runs without it
- Dev config in `src/main/resources/application-dev.yml` (MySQL/Redis creds, JWT secret)

## Architecture

```
HTTP → CorsFilter (global bean) → RateLimitFilter → JwtAuthenticationFilter → Spring Security → Controller → Service → Mapper (MyBatis-Plus) → MySQL
```

**Package layout** — each business domain follows `com.drivingschool.<domain>/{controller,service,mapper,entity,dto}`:

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
| `ai` | `/ai` | `ChatHistory` (DashScope/阿里云百炼 AI chat, SSE streaming + sync) |

Cross-cutting in `common/`: `config/` (Security, JWT filter, RateLimit filter, CORS, Jackson, AOP, async, cache, scheduled tasks), `constant/Constants`, `enums/`, `exception/` (BusinessException + GlobalExceptionHandler), `result/` (R\<T\>, PageResult), `utils/` (JwtUtils, RedisUtils, ExcelUtils).

## Key Conventions

- **Auth**: JWT stateless. `JwtAuthenticationFilter` parses `Authorization: Bearer <token>`. `Authentication.getPrincipal()` returns `Long` userId. Roles: `ADMIN`, `STAFF`, `COACH`, `STUDENT` (enum `UserRole`). Use `@PreAuthorize("hasRole('ADMIN')")` for method-level security.
- **Entities**: `@TableName`, `@TableId(type = IdType.ASSIGN_ID)` (snowflake IDs), `deleted` field for `@TableLogic`. `createTime`/`updateTime` auto-filled by `MetaObjectHandler`.
- **Pagination**: MyBatis-Plus `Page` object; returns `PageResult` with `records/total/pageNum/pageSize/pages`. Defaults: `pageNum=1`, `pageSize=10`.
- **Response**: All endpoints return `R<T>` — `code=200` success, throw `BusinessException` for business errors (GlobalExceptionHandler maps to appropriate HTTP codes).
- **Validation**: DTOs use Jakarta Validation (`@Valid`); failures return 400 via GlobalExceptionHandler.
- **Long serialization**: Jackson converts `Long` → `String` (via `JacksonConfig`) to avoid JS precision loss.
- **Logging**: `@Operation`-annotated endpoints are logged to `sys_log` by `LogAspect` (async via `async-log-` thread pool prefix).
- **IDs in DTOs/params**: Use `@DateTimeFormat(pattern = "yyyy-MM-dd")` for date params. Page params: `pageNum`, `pageSize`.
- **Constructor injection**: Use `@RequiredArgsConstructor` (Lombok) on controllers/services.
- **Swagger**: `@Tag` on controllers, `@Operation` on methods. UI at `/api/swagger-ui.html`.

## Business Constants (Constants.java)

`MAX_STUDENTS_PER_COACH=30`, `MAX_LESSONS_PER_DAY=4`, `CANCEL_HOURS_BEFORE=2`, `MAX_RETAKE_COUNT=5`, `RETAKE_INTERVAL_DAYS=10`, pass scores: subject 1/3/4 = 90, subject 2 = 80.

## Notable Dependencies

- **DashScope SDK** (`com.alibaba:dashscope-sdk-java:2.20.0`) — Alibaba Cloud's AI platform for the `/ai` chat feature. Supports SSE streaming and sync responses.
- **Hutool** (`cn.hutool:hutool-all:5.8.27`) — utility library
- **Apache POI** (`poi-ooxml:5.2.5`) — Excel export

## API Docs

Swagger UI: `http://localhost:8080/api/swagger-ui.html`
OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`

## Database Schema

Full DDL in `src/main/resources/schema.sql`. 30 tables, all using BIGINT snowflake PKs, soft delete (`deleted` column), `create_time`/`update_time` auto-filled. No migration tool (Flyway/Liquibase) — schema is managed via this single file. ER diagram: open `ER图预览.html` (project root) in a browser.

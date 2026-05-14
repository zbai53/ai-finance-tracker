# AI Finance Tracker — Build Progress Log

> Daily log of what got done, what got stuck, what's next.
> The newest entry goes at the TOP. Older entries roll down.

---

## 2026-05-14 (Wednesday) · Day 4 — Auth endpoints and global exception handling

**Phase:** Week 1 (Spring Boot Backend Foundation)
**Time spent:** ~3 hrs
**Day:** 4 of 28

### Done
- Wrote `Result<T>` unified response wrapper with static factory methods
- Wrote `GlobalExceptionHandler` with `@RestControllerAdvice`
- Wrote `AuthController` with `POST /api/auth/register` and `POST /api/auth/login`
- Used Java `record` for request DTOs with `@NotBlank` validation
- Tested register and login in Postman — both return correct `Result` format
- Verified JWT filter: 403 without token, passes through with valid token

### Blockers / lessons
- `record` accessor methods don't use `get` prefix — `request.email()` not `request.getEmail()`
- `Bearer ` prefix required in Authorization header
- `/api/auth/**` is permitAll so won't return 403 — need non-auth endpoint to test security
- Understood Controller vs Service separation: Controller handles HTTP in/out, Service handles business logic

### Next session goal
- Day 5: Write `TransactionService` and `TransactionController` with dynamic SQL filtering and pagination

---

## 2026-05-13 (Tuesday) · Day 3 — JWT authentication with Spring Security

**Phase:** Week 1 (Spring Boot Backend Foundation)
**Time spent:** ~3 hrs
**Day:** 3 of 28

### Done
- Added jjwt dependencies to `pom.xml`
- Wrote `JwtUtil` with `generateToken()`, `getUserIdFromToken()`, `validateToken()`
- Wrote `UserService` with `register()` (BCrypt password hashing) and `login()`
- Wrote `JwtAuthenticationFilter extends OncePerRequestFilter`
- Wrote `SecurityConfig` with `SecurityFilterChain` — permitAll for `/api/auth/**`, authenticated for everything else
- Application starts and compiles successfully

### Blockers / lessons
- Class name case matters — `JWtUtil` vs `JwtUtil` broke the entire compile chain
- `OncePerRequestFilter` guarantees the filter runs exactly once per request
- JWT is stateless: server stores nothing, token carries the userId in the payload
- BCrypt `matches()` is needed for verification because BCrypt output is non-deterministic

### Next session goal
- Day 4: Write `Result<T>` wrapper, `GlobalExceptionHandler`, `AuthController`

---

## 2026-05-13 (Tuesday) · Day 2 — Entity classes and MyBatis mapper layer

**Phase:** Week 1 (Spring Boot Backend Foundation)
**Time spent:** ~2 hrs
**Day:** 2 of 28

### Done
- Added MyBatis-Plus dependency (`mybatis-plus-spring-boot3-starter`)
- Wrote four entity classes: `User`, `Category`, `Transaction`, `AiConversation`
- Wrote four Mapper interfaces extending `BaseMapper<T>`
- Wrote `TransactionMapper.xml` with `selectByUserIdAndDateRange` dynamic SQL
- Application starts with no MyBatis mapper warnings

### Blockers / lessons
- File name must match class name exactly — `user.java` vs `User.java` causes compile error
- `map-underscore-to-camel-case: true` in `application.yml` handles `created_at` → `createdAt` mapping
- `BaseMapper` provides free CRUD; XML is needed for dynamic queries, JOINs, and aggregates
- `classpath:` maps to `target/classes/` which is where `src/main/resources/` gets copied after compile

### Next session goal
- Day 3: Add JWT dependencies, write JwtUtil, UserService, JwtAuthenticationFilter

---

## 2026-05-12 (Monday) · Day 1 — Project init and database setup

**Phase:** Week 1 (Spring Boot Backend Foundation)
**Time spent:** ~2 hrs
**Day:** 1 of 28

### Done
- Verified environment: Java 21, Maven 3.9.15, Node 23
- Generated Spring Boot 3.5.14 project at start.spring.io with correct dependencies
- Fixed nested directory issue after unzipping (`backend/backend/` → `backend/`)
- Configured `application.yml` with MySQL datasource
- Created `finance_tracker` database and four tables: `user`, `category`, `transaction`, `ai_conversation`
- Application starts successfully (expected DataSource error resolved after yml config)

### Blockers / lessons
- `git commit` requires `-m` flag — `git commit "message"` treats the string as a file path
- GitHub credential conflict between `zhihao0112` and `zbai53` — cleared keychain cache to fix
- `application.yml` is read automatically because Spring Boot follows convention over configuration
- `classpath` = `target/classes/`, not the source directory

### Next session goal
- Day 2: Add MyBatis-Plus dependency, write four entity classes and mapper interfaces

---

## 2026-05-12 (Monday) · Day 0 — Project setup

**Phase:** Pre-work
**Time spent:** ~2 hrs
**Day:** 0 of 28

### Done
- Created GitHub repository `ai-finance-tracker`
- Created Claude Project with all five knowledge files
- Updated README with project description, tech stack, architecture diagram
- Switched AI provider from DeepSeek to Claude API
- Fixed GitHub credential conflict, first push successful

### Blockers / lessons
- None — setup day

### Next session goal
- Day 1: Verify environment, generate Spring Boot project, create database tables

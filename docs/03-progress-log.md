# AI Finance Tracker — Build Progress Log

> Daily log of what got done, what got stuck, what's next.
> The newest entry goes at the TOP. Older entries roll down.

---

## 2026-05-29 (Thursday) · Day 20 — AI conversation history with context injection

**Phase:** Week 3 (LLM Features)
**Time spent:** ~2 hrs
**Day:** 20 of 28

### Done
- Created `ConversationService.java` with save(), getRecent(), buildContextString()
- Updated `NlQueryService` to load last 5 conversations as context for answer prompt
- Saves user question before streaming, saves assistant answer after streaming completes
- Added `GET /api/ai/history` endpoint returning last 20 conversations as JSON
- Created `src/api/ai.ts` with `getAiHistory()` function
- Added collapsible conversation history panel to DashboardPage
- History refreshes automatically after each query completes
- Created `run.sh` startup script to pass ANTHROPIC_API_KEY via JVM argument

### Blockers / lessons
- `mvn spring-boot:run` doesn't reliably pass terminal environment variables to the Java subprocess — use `-Dspring-boot.run.jvmArguments` to inject directly
- `run.sh` with `source ~/.zshrc` is the permanent fix — one command to start backend
- Context is only added to the final answer prompt, NOT the intent extraction prompt — adding history to JSON extraction would confuse structured output
- `AiService.streamResponseAndCapture()` accumulates content in StringBuilder while streaming — captures the full response for persistence

### Next session goal
- Day 21: Add timeout, retry, and graceful error handling for all AI features


## 2026-05-29 (Friday) · Day 19 — Spending anomaly detection

**Phase:** Week 3 (LLM Features)
**Time spent:** ~2 hrs
**Day:** 19 of 28

### Done
- Created `AnomalyService.java` — fetches current month + previous 2 months category spending, builds comparison table, streams Claude's analysis
- Added `GET /api/ai/anomalies?year=&month=` endpoint to `AiController`
- Added `buildAnomalyUrl()` to `useAiStream.ts`
- Added "⚠ Check Anomalies" button (amber accent) to Dashboard
- Anomaly card renders markdown with `ReactMarkdown`
- Tested: correctly identified new spending categories vs $0 average from prior months

### Blockers / lessons
- API key committed to application.yml AGAIN — must use `${ANTHROPIC_API_KEY:fallback}` pattern and never hardcode
- Permanent fix: added key to `~/.zshrc` so it's always available without manual export
- After any key leak: revoke immediately and generate new one, even after git rebase
- Category comparison uses name-based matching (not ID) — correct because categoryId can change if user deletes and recreates

### Next session goal
- Day 20: AI conversation history — persist interactions and inject recent history as context


## 2026-05-27 (Tuesday) · Day 18 — Natural language transaction query

**Phase:** Week 3 (LLM Features)
**Time spent:** ~3 hrs
**Day:** 18 of 28

### Done
- Created `NlQueryService.java` with two-call pattern:
  - Call 1: Claude extracts structured intent JSON from natural language question
  - Call 2: Claude streams natural language answer using actual transaction data
- Added `AiService.extractText()` for non-streaming single-call responses
- Added `GET /api/ai/query?question=` endpoint to `AiController`
- Added search bar and AI Answer card to DashboardPage
- Added `buildQueryUrl()` to `useAiStream.ts`
- Tested: "how much did I spend this month?" → correct streaming answer

### Blockers / lessons
- API key nightmare: old key cached in `target/classes/application.yml` — `mvn clean` required to clear it
- Never use environment variables from a different terminal session — key must be in the same session as `mvn spring-boot:run`
- Anthropic keys may take a few minutes to fully activate after creation
- **Never commit API keys to git** — GitHub push protection catches it immediately; revoke and regenerate the key even after rebase
- LLM never writes SQL — it only extracts structured parameters (type, categoryName, startDate, endDate) that map to pre-written MyBatis queries

### Next session goal
- Day 19: Spending anomaly detection — compare this month's spending to previous months and surface unusual patterns


## 2026-05-26 (Monday) · Day 17 — Monthly financial report with streaming output

**Phase:** Week 3 (LLM Features)
**Time spent:** ~2 hrs
**Day:** 17 of 28

### Done
- Created `ReportService.java` — fetches monthly transactions and summary, builds financial context prompt, calls `aiService.streamResponse()`
- Added `GET /api/ai/report?year=&month=` endpoint to `AiController`
- Added `buildReportUrl()` helper to `useAiStream.ts`
- Added "✦ Generate AI Report" button to Dashboard — streams report in real time
- Report card shows typing cursor while streaming, Clear button to dismiss
- Fixed Markdown rendering: installed `react-markdown` + `@tailwindcss/typography`, added `prose` class
- Fixed Markdown heading issue: added "Do NOT use # headings" to prompt

### Blockers / lessons
- **Never hardcode API keys in application.yml** — GitHub push protection caught it immediately
- Fix: use `${ANTHROPIC_API_KEY:fallback}` in yml, set real key via environment variable only
- After a key appears in git history it must be revoked immediately even after rebase
- `react-markdown` requires dev server restart to pick up new Tailwind plugin (`@tailwindcss/typography`)
- `onerror` on EventSource fires both on real errors AND normal stream close — treat both as "done"

### Next session goal
- Day 18: Natural language transaction query — user types "how much did I spend on food last month?" and gets a direct answer

## 2026-05-26 (Monday) · Day 16 — SSE streaming setup

**Phase:** Week 3 (LLM Features)
**Time spent:** ~2 hrs
**Day:** 16 of 28

### Done
- Added `streamResponse()` method to `AiService` using `client.messages().createStreaming()` with `StreamResponse<RawMessageStreamEvent>`
- Created `AiController` with `GET /api/ai/stream` endpoint producing `TEXT_EVENT_STREAM_VALUE`
- Used `CompletableFuture.runAsync` to run blocking stream off the servlet thread — returns `SseEmitter` immediately
- Updated `JwtAuthenticationFilter` to accept token as query param fallback (needed for `EventSource` which can't set custom headers)
- Created `frontend/src/hooks/useAiStream.ts` with `start()`, `reset()`, `content`, `isStreaming`, `error` state
- Tested in browser: `?prompt=Say hello in 3 words&token=...` → real-time streaming chunks visible

### Blockers / lessons
- Wrong method name: `stream()` doesn't exist on blocking `MessageService` — correct method is `createStreaming()`
- `StreamResponse<RawMessageStreamEvent>` is `AutoCloseable` — must use try-with-resources
- Text extraction chain: `.contentBlockDelta().stream()` → `.delta().text().stream()` → `.text()`
- `EventSource` API in browsers cannot set custom headers — token must be passed as query param for SSE endpoints
- ANTHROPIC_API_KEY must be exported in the same terminal session as `mvn spring-boot:run`

### Next session goal
- Day 17: Build monthly financial report feature using SSE streaming — fetch transactions, build prompt with financial context, stream Claude's analysis to frontend

## 2026-05-25 (Sunday) · Day 15 — Claude API integration for transaction categorization

**Phase:** Week 3 (LLM Features)
**Time spent:** ~2 hrs
**Day:** 15 of 28

### Done
- Added `com.anthropic:anthropic-java:2.11.0` dependency to `pom.xml`
- Added `anthropic.api-key` config to `application.yml`
- Created `src/main/java/com/financetracker/backend/ai/AiService.java` with `categorizeTransaction()` method
- Prompt filters categories by same type (expense→expense only) to reduce noise
- Integrated AI categorization into `TransactionService.create()` — runs after insert, updates `aiCategory` field
- AI failure is caught and logged — core transaction creation never blocked
- Tested: "Grocery shopping at Walmart" → auto-categorized as "Food" ✅

### Blockers / lessons
- Wrong artifact ID: `com.anthropic:sdk` doesn't exist — correct is `com.anthropic:anthropic-java`
- `export ANTHROPIC_API_KEY` only applies to the current terminal session — must export in the same terminal that runs `mvn spring-boot:run`
- New Anthropic API keys need account credit to work — authentication_error 401 = invalid or inactive key
- `@Value` fields can't be `final` with `@RequiredArgsConstructor` — Lombok only injects `final` fields via constructor, `@Value` uses field injection after construction

### Next session goal
- Day 16: Implement SSE streaming setup — bridge Claude streaming response to frontend via Spring SseEmitter


## 2026-05-24 (Sunday) · Day 14 — Environment variables and production CORS config

**Phase:** Week 2 (React Frontend) — COMPLETE
**Time spent:** ~1.5 hrs
**Day:** 14 of 28

### Done
- Created `frontend/.env.development` and `frontend/.env.production` with `VITE_API_BASE_URL`
- Updated `frontend/src/api/http.ts` to use `import.meta.env.VITE_API_BASE_URL` instead of hardcoded URL
- Updated `CorsConfig.java` to read allowed origin from `@Value("${frontend.url:http://localhost:5173}")`
- Added `frontend.url` to `application.yml` for local dev
- Added `.env.production` to `frontend/.gitignore` — production URLs shouldn't be in source control
- Ran full E2E test: login → create transaction → dashboard stats → edit → delete — all pass

### Blockers / lessons
- Environment variables in Vite use `import.meta.env.VITE_*` prefix — `process.env` doesn't work in Vite
- Spring Boot maps `FRONTEND_URL` env var → `frontend.url` property automatically via relaxed binding
- `.env.development` is safe to commit (localhost only), `.env.production` should not be committed
- Same codebase runs in dev and prod by injecting different env vars — no code changes needed to deploy

### Week 2 deliverable
A complete full-stack app: React frontend with sidebar navigation, transaction CRUD, dashboard charts, JWT auth, and production-ready CORS + environment config. A real user can register, log in, manage transactions, and see a dashboard.

### Next session goal
- Day 15: Integrate Claude API for automatic transaction categorization

## 2026-05-24 (Sunday) · Day 13 — App layout with sidebar navigation and polish

**Phase:** Week 2 (React Frontend)
**Time spent:** ~2 hrs
**Day:** 13 of 28

### Done
- Created `AppLayout.tsx` with fixed sidebar (240px), active link detection via `useLocation`, logout button pinned to bottom
- Mobile responsive: sidebar collapses to hamburger menu on small screens
- Updated `App.tsx` to wrap all protected routes with `AppLayout`
- Removed header nav buttons from `DashboardPage` — layout now owns navigation
- Fixed Net Savings color: red when net < 0, blue when net >= 0
- Empty state on TransactionsPage now has "Add your first transaction" button that opens modal
- Both pages visually consistent with shared sidebar and background

### Blockers / lessons
- frontend-design skill deferred to Day 27 — applying it now would risk breaking working functionality mid-development
- `useLocation().pathname` is the correct way to detect active route in React Router v6 for styling nav links
- `NavLink` from react-router-dom also handles active state automatically — worth knowing as an alternative to manual pathname comparison

### Next session goal
- Day 14: Configure CORS properly for production, set up environment variables, run full E2E test

## 2026-05-24 (Sunday) · Day 12 — Dashboard with Recharts visualizations

**Phase:** Week 2 (React Frontend)
**Time spent:** ~2 hrs
**Day:** 12 of 28

### Done
- Created `src/api/statistics.ts` with `getMonthlySummary` and `getCategoryStatistics`
- Built complete `DashboardPage` with:
  - Month selector (left/right arrows, year rollover handled)
  - Three summary cards: Total Income, Total Expense, Net Savings with color coding
  - LineChart (Recharts): last 6 months income vs expense using Promise.all for parallel fetches
  - PieChart (Recharts): expense breakdown by category with custom legend
  - Loading states for all three data sources independently
- Dashboard displays real data from backend — charts update when month changes

### Blockers / lessons
- `Promise.all` is the right pattern for fetching 6 months of data in parallel — sequential would be 6× slower
- `ResponsiveContainer` is required for Recharts to fill its parent div — without it the chart has a fixed pixel width
- Pie chart showing two "Food" categories = duplicate test data in DB, not a code bug
- Net Savings color bug (should be gray when negative) — minor UI issue to fix in Day 13 polish

### Next session goal
- Day 13: Add app layout with sidebar navigation, polish all pages for visual consistency

## 2026-05-24 (Saturday) · Day 10 — Transaction list page with real data

**Phase:** Week 2 (React Frontend)
**Time spent:** ~3 hrs
**Day:** 10 of 28

### Done
- Wrote `src/api/transactions.ts` and `src/api/categories.ts` with typed Axios functions
- Added `TransactionsPage` with table, filter bar (type + date range), pagination, loading state, empty state
- Connected frontend to backend — transaction list displays real data from MySQL
- Fixed Spring Security circular dependency: replaced `@RequiredArgsConstructor` with manual constructor using `ApplicationContext` injection
- Fixed CORS preflight: added `OPTIONS /**` permitAll to SecurityConfig so browser preflight requests pass through to CorsFilter
- Seeded test data via Postman — categories and transactions visible in UI

### Blockers / lessons
- `Using generated security password` warning means SecurityConfig isn't loading — took multiple attempts to diagnose as circular dependency
- CORS error on GET/POST but not on login = Spring Security blocking OPTIONS preflight, not a real CORS config issue
- Preflight (OPTIONS) requests carry no auth token — they must be permitted unconditionally, before authentication checks
- `Provisional headers are shown` in DevTools = request was blocked before it left the browser, not a server-side issue

### Next session goal
- Day 11: Build transaction create/edit modal with form, API integration, and list refresh on success


## 2026-05-18 (Sunday) · Day 9 — Login and Register pages with API integration

**Phase:** Week 2 (React Frontend)
**Time spent:** ~2 hrs
**Day:** 9 of 28

### Done
- Wrote `src/api/auth.ts` with `login()` and `register()` functions using typed Axios calls
- Wrote complete `LoginPage` with controlled inputs, loading state, error display, and JWT save on success
- Wrote complete `RegisterPage` with client-side password length validation, auto-login after register
- Added `/register` route to `App.tsx`
- Fixed CORS: added `CorsConfig.java` to allow `localhost:5173` and `5174`
- Tested full flow: register new user → auto-login → redirect to Dashboard

### Blockers / lessons
- CORS blocked the first login attempt — browser blocks cross-origin requests unless the server explicitly allows it via `Access-Control-Allow-Origin` header
- `.then(r => r.data.data)` — two `.data` because Axios wraps response in its own `.data`, and the backend wraps business data in `Result.data`
- `e.preventDefault()` is required in form `onSubmit` — without it the browser refreshes the page
- Register flow calls `register()` then immediately calls `login()` to auto-inject token — one less step for the user

### Next session goal
- Day 10: Build Transaction list page with data fetching, loading state, filter bar, and pagination

## 2026-05-18 (Sunday) · Day 8 — React project init with routing and auth context

**Phase:** Week 2 (React Frontend)
**Time spent:** ~3 hrs
**Day:** 8 of 28

### Done
- Initialized Vite + React 18 + TypeScript project under `frontend/`
- Installed dependencies: axios, react-router-dom, recharts, tailwindcss v3
- Configured Tailwind: `tailwind.config.js` content paths + `@tailwind` directives in `index.css`
- Created folder structure: `api/`, `components/`, `context/`, `pages/`, `types/`
- Wrote `http.ts` with Axios instance, request interceptor (auto-inject JWT), response interceptor (401/403 redirect)
- Defined TypeScript interfaces in `types/index.ts` for Result, User, Transaction, Category, PageResult, statistics DTOs
- Wrote `AuthContext.tsx` with `login()`, `logout()`, `isAuthenticated` state
- Wrote `ProtectedRoute` component that redirects to `/login` if unauthenticated
- Wrote `App.tsx` with routes for `/login`, `/dashboard` (protected), and `/` redirect
- Tested routing: unauthenticated redirects to login, with token in localStorage shows dashboard

### Blockers / lessons
- `useState(null)` + `useEffect` for reading localStorage causes a first-render flicker — `ProtectedRoute` sees `token=null` and redirects before useEffect runs. Fix: use lazy initializer `useState(() => localStorage.getItem('token'))` so initial state is correct synchronously
- TypeScript with new compiler options requires `import type` for type-only imports like `ReactNode`
- Tailwind v3 vs v4 have different config — installed v3 explicitly to match the roadmap
- Frontend types are contracts between backend and frontend — they let the compiler catch typos and breaking changes at compile time

### Next session goal
- Day 9: Build complete Login and Register pages with form validation, API integration, and error handling


## 2026-05-17 (Saturday) · Day 7 — Postman collection and Week 1 wrap-up

**Phase:** Week 1 (Spring Boot Backend Foundation) — COMPLETE
**Time spent:** ~2 hrs
**Day:** 7 of 28

### Done
- Created Postman collection `AI Finance Tracker API` with 4 folders (Auth, Transactions, Categories, Statistics) and 11 requests
- Set up environment variables `baseUrl` and `token` for portable URLs
- Wrote post-response script on Login to auto-inject token into environment
- Ran full E2E flow: login → create category → create transaction → list → monthly summary → by-category
- Code review: verified every write endpoint passes userId to service layer, every service write method checks ownership
- Understood IDOR vulnerability and why ownership checks matter on every write operation
- Exported collection to `docs/api.postman_collection.json`

### Blockers / lessons
- Postman scripts moved from "Tests" tab to "Scripts → Post-response"
- Authorization (ownership check) is separate from Authentication (JWT) — both are required for safe writes
- Read operations are lower-risk than write operations — focus security review on create/update/delete first

### Week 1 deliverable
A complete backend REST API with JWT auth, multi-layer architecture, dynamic SQL filtering, pagination, and aggregate statistics. All 11 endpoints work end-to-end via Postman.

### Next session goal
- Day 8: Initialize React + Vite + TypeScript frontend project, set up routing and AuthContext


## 2026-05-16 (Friday) · Day 6 — Category CRUD and statistics aggregation

**Phase:** Week 1 (Spring Boot Backend Foundation)
**Time spent:** ~3 hrs
**Day:** 6 of 28

### Done
- Wrote `CategoryService` (create, list, delete) using `QueryWrapper` for simple queries
- Wrote `CategoryController` with 3 endpoints — deliberately skipped update and pagination per scope discipline
- Created two DTOs: `CategoryStatistics` (categoryId, categoryName, total, count) and `MonthlySummary` (totalIncome, totalExpense, net)
- Added `selectCategoryStatistics` and `selectMonthlySummary` to `TransactionMapper`
- Wrote aggregate SQL with `LEFT JOIN`, `GROUP BY`, `CASE WHEN`, `COALESCE`, and dynamic filters
- Used `ResultMap` to explicitly map JOIN aliases like `category_name` to `categoryName`
- Wrote `StatisticsController` calling Mapper directly (no service layer — pure query, no business logic)
- Tested in Postman: monthly summary returns correct totals, by-category groups correctly with NULL handling

### Blockers / lessons
- `BaseMapper` methods (`insert`, `selectById`, etc) only work if the Mapper actually extends `BaseMapper<T>` — missed it on CategoryMapper
- `ResultMap` is needed when columns come from JOIN aliases or aggregate functions — auto camelCase mapping isn't reliable there
- Skipping the service layer is a deliberate choice for thin query controllers — would refactor if business logic emerges
- `COALESCE(..., 0)` handles NULL gracefully when no records match the filter

### Next session goal
- Day 7: Build Postman collection, run full end-to-end test, fix any bugs found, export collection to `docs/`

## 2026-05-15 (Thursday) · Day 5 — Transaction CRUD with dynamic SQL

**Phase:** Week 1 (Spring Boot Backend Foundation)
**Time spent:** ~3 hrs
**Day:** 5 of 28

### Done
- Added PageHelper dependency for pagination
- Wrote `TransactionService` with create, list (filtered + paginated), update, delete
- Implemented `getOwnedTransaction()` helper for authorization checks
- Added `selectByFilter` method to `TransactionMapper` with `@Param` annotations
- Wrote dynamic SQL in `TransactionMapper.xml` using `<where>` + `<if>` for optional filters
- Wrote `TransactionController` with `@RequestParam`, `@PathVariable`, `@RequestBody`
- Tested full CRUD in Postman: create, filter by type, paginate, delete
- Verified dynamic SQL: `type=income` returns empty list, `type=expense` returns all 3 records

### Blockers / lessons
- `@PutMapping` not `@PutMapper` — annotation name matters
- `record` accessor methods drop the `get` prefix
- PageHelper requires `startPage()` to be called immediately before the query (ThreadLocal-based)
- JWT verifies identity (authentication), ownership check verifies access (authorization) — both needed

### Next session goal
- Day 6: Write `CategoryService`, `CategoryController`, and aggregate `StatisticsController` with `GROUP BY` queries

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

# AI Finance Tracker

> A full-stack personal finance platform powered by the Claude API — automatically categorizes transactions, streams real-time financial reports, and answers natural-language questions about your spending.

---

## Live Demo

**[https://ai-finance-tracker-lake.vercel.app](https://ai-finance-tracker-lake.vercel.app)**

| Field | Value |
|-------|-------|
| Email | `demo@example.com` |
| Password | `demo123456` |

The demo account is pre-loaded with 90 days of realistic transactions across 8 categories. No sign-up required.

---

## What It Does

- **JWT-authenticated REST API** — Spring Boot 3.5 backend with stateless JWT auth; every request is validated by a custom `JwtAuthenticationFilter` before reaching any controller
- **AI auto-categorization** — when a transaction is created, `AiService` calls `claude-sonnet-4-5` with the description, amount, and user's category list; the model returns the best-matching category name in a single token, stored immediately
- **Streaming AI reports** — monthly financial summaries are generated via `SseEmitter` + `EventSource`; tokens stream from the Claude API through Spring directly to the browser with no buffering
- **Natural-language query engine** — a two-call AI pipeline: first call extracts structured intent (date range, category, type) as JSON; second call streams a natural-language answer using the retrieved transactions as context
- **Spending anomaly detection** — compares current month category totals against the prior two months, formats a fixed-width comparison table, and streams Claude's analysis of unusual patterns

---

## Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| Frontend framework | React 18 + TypeScript + Vite | SPA with strict type safety |
| Styling | Tailwind CSS v3 | Utility-first responsive UI |
| Charts | Recharts | Line chart (6-month trend) + Pie chart (category breakdown) |
| HTTP client | Axios | JWT interceptor auto-attaches `Authorization` header |
| AI streaming | Browser `EventSource` | Consumes SSE stream from backend |
| Backend framework | Spring Boot 3.5 + Java 21 | REST API, security, SSE |
| ORM | MyBatis-Plus | Dynamic SQL with full query visibility |
| Auth | Spring Security + JJWT | Stateless JWT; token also accepted as query param for SSE |
| AI | Anthropic Java SDK 2.11 (`claude-sonnet-4-5`) | Categorization, reports, NL queries, anomaly detection |
| Database | MySQL 8 | Transactions, categories, users, conversation history |
| CI/CD | GitHub Actions | Build + type-check on every push to `main` |
| Backend hosting | Railway | Auto-deploy from `main`; env vars injected at runtime |
| Frontend hosting | Vercel | Auto-deploy; `VITE_API_BASE_URL` set per environment |

---

## Architecture

```
Browser (React + Vite)              Spring Boot 3.5 (Railway)          MySQL 8
┌──────────────────────────┐        ┌──────────────────────────┐       ┌─────────────────┐
│  DashboardPage           │        │  JwtAuthenticationFilter │       │  transaction    │
│  TransactionsPage        │─REST──▶│  AuthController          │──────▶│  category       │
│  useAiStream (hook)      │        │  TransactionController   │       │  user           │
│  EventSource (SSE)       │◀─SSE───│  AiController            │       │  ai_conversation│
└──────────────────────────┘        │  ┌────────────────────┐  │       └─────────────────┘
                                    │  │ AiService          │  │
                                    │  │ ReportService      │  │
                                    │  │ NlQueryService     │  │       Anthropic API
                                    │  │ AnomalyService     │──┼──────▶ claude-sonnet-4-5
                                    │  │ ConversationService│  │
                                    │  └────────────────────┘  │
                                    └──────────────────────────┘

AI pipeline (shared pattern):
  1. Fetch transactions / category stats from MySQL via MyBatis-Plus
  2. Serialize to a structured plain-text context block
  3. Build a role-specific prompt (categorizer / analyst / query engine)
  4. Call Claude — non-streaming for classification, streaming for reports and answers
  5. Return via JSON (categorization) or SSE stream (everything else)
```

---

## AI Features

**Auto-categorization**
When a transaction is saved, `AiService.categorizeTransaction()` builds a prompt listing only the categories that match the transaction's type (income or expense), then calls Claude with `maxTokens: 50`. The model returns a single category name; mismatches default to "Other" so the flow never blocks a save.

**Streaming Monthly Report**
`ReportService` fetches the month's `MonthlySummary` (total income, expense, net) and the top 10 transactions by amount, then calls `AiService.streamResponse()`. Spring's `SseEmitter` pushes each text delta to the browser as it arrives from the Anthropic streaming API, so the report types out in real time.

**Natural-Language Query**
`NlQueryService` uses a two-call pattern: a non-streaming call extracts structured intent (date range, category, transaction type) as JSON; the service queries MySQL with those parameters; a second streaming call answers the user's original question using the retrieved rows as context. The last five conversation turns are prepended to maintain continuity.

**Spending Anomaly Detection**
`AnomalyService` loads category-level expense totals for the current month and the two preceding months, formats them into a fixed-width Markdown table, and asks Claude to identify significant deviations. Results stream directly to the dashboard's anomaly card.

---

## Key Technical Decisions

**Why MyBatis over JPA**
MyBatis was chosen deliberately over Spring Data JPA because every SQL query is hand-written and visible — there are no generated queries to debug. The transaction list endpoint has six optional filters, a JOIN with the category table, and pagination; MyBatis dynamic SQL (`<if>` tags) handles this cleanly without the N+1 risks that come with lazy-loading JPA relationships. In an interview, being able to show the exact query for any feature matters.

**Why Claude API**
The Claude API was chosen because `claude-sonnet-4-5` produces well-structured plain-text financial analysis without extensive prompt engineering, and the Anthropic Java SDK 2.11 has first-class streaming support via `createStreaming()` that maps cleanly onto Spring's `SseEmitter`. Claude's long context window handles the full monthly transaction payload without truncation, which matters for the anomaly detection feature that sends three months of category data in one prompt.

**Why SSE over WebSocket**
Server-Sent Events are unidirectional and HTTP/1.1-compatible — no protocol upgrade, no extra infrastructure, and they work through Vercel and Railway's reverse proxies without configuration. Since all AI responses flow server-to-client only, SSE is the right tool; WebSocket would add handshake complexity for no gain.

---

## Local Setup

**Prerequisites:** Java 21, Node 20, MySQL 8, an Anthropic API key

```bash
# 1. Clone
git clone https://github.com/zbai53/ai-finance-tracker.git
cd ai-finance-tracker

# 2. Create database
mysql -u root -p -e "CREATE DATABASE finance_tracker;"

# 3. Configure backend
# Edit backend/src/main/resources/application.yml:
#   spring.datasource.password: <your MySQL password>

# 4. Export API key and start backend
export ANTHROPIC_API_KEY=sk-ant-...
cd backend
mvn spring-boot:run

# 5. Start frontend (new terminal)
cd frontend
cp .env.development.local.example .env.development.local   # or set VITE_API_BASE_URL=http://localhost:8080
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173). The `DataSeeder` will auto-create the demo account with 90 days of transactions on first startup.

---

## What I Learned

- **Streaming across the full stack** — wiring the Anthropic SDK's `createStreaming()` through Spring's `SseEmitter` to the browser's `EventSource` required understanding how each layer buffers (or doesn't): the SDK emits `contentBlockDelta` events, Spring flushes each `emitter.send()` immediately, and the browser appends tokens in `onmessage`. Getting `onerror` to distinguish a real network failure from a normal stream close required a `contentRef` mirror pattern in the React hook.

- **JWT in non-standard contexts** — the `EventSource` API does not support custom headers, so SSE authentication required extending `JwtAuthenticationFilter` to read the token from a query parameter (`?token=...`) as a fallback. This is a real-world constraint that JWTs-in-headers tutorials don't cover.

- **Prompt engineering for structured extraction** — the natural-language query feature required two separate Claude calls because a single call that both extracts intent and answers in prose is unpredictable. Separating "extract JSON" from "answer in natural language" made each call reliable and testable independently.

- **MyBatis dynamic SQL for complex filters** — building the transaction list query with six optional parameters (type, category, date range, pagination) in MyBatis XML taught me how to compose `WHERE` clauses conditionally without ORMs. The resulting SQL is fully predictable and easy to optimize with `EXPLAIN`.

- **Production configuration discipline** — maintaining `application.yml` for local dev and `application-prod.yml` for Railway with only `${ENV_VAR}` placeholders enforced a clean boundary between code and secrets. The same pattern on the frontend (`VITE_API_BASE_URL` per environment) meant zero changes were needed when the backend URL changed during Railway setup.

---

## Resume Bullet

> Built a full-stack AI finance platform (Spring Boot 3.5 / React 18 / Claude API) featuring real-time streaming reports via SSE, a two-call natural-language query engine, and automatic transaction categorization; deployed on Railway + Vercel with a GitHub Actions CI pipeline — [github.com/zbai53/ai-finance-tracker](https://github.com/zbai53/ai-finance-tracker)

# AI Finance Tracker

> An AI-powered personal finance platform. Not just a spreadsheet with charts — an intelligent system that categorizes transactions automatically, generates streaming financial reports, and answers natural language questions about your spending.

**Status: actively building — Day 1 of 28**

---

## Live Demo

🚧 Deploying in Week 4 — check back soon.

Demo account will be available at launch: `demo@example.com` / `demo123456`

---

## What it does

- **Auto-categorization** — transactions are categorized by an LLM the moment you add them
- **Streaming monthly reports** — AI-generated financial analysis that types out in real time
- **Natural language queries** — ask "how much did I spend on food last month?" and get a direct answer
- **Spending anomaly detection** — surfaces unusual patterns without you having to look for them
- **Interactive dashboard** — income vs expense trends, category breakdowns, month-over-month comparison

---

## Tech stack

| Layer | Technology |
|-------|-----------|
| Frontend | React 18 + TypeScript + Vite |
| Styling | Tailwind CSS |
| Charts | Recharts |
| HTTP | Axios (with JWT interceptors) |
| Backend | Spring Boot 3.2 + Java 17 |
| Persistence | MyBatis-Plus |
| Auth | Spring Security + JWT (stateless) |
| Database | MySQL 8 |
| AI | Claude API (claude-sonnet-4-5) |
| Streaming | Server-Sent Events (SSE) |
| Deploy — frontend | Vercel |
| Deploy — backend | Railway |
| Deploy — database | PlanetScale |

---

## Architecture

React (Vite)          Spring Boot 3.2         MySQL 8
┌─────────────┐       ┌─────────────────┐     ┌──────────────┐
│  Dashboard  │──────▶│  REST API + JWT │────▶│ Transactions │
│  Transactions│  SSE  │  MyBatis-Plus   │     │ Categories   │
│  AI Chat    │◀──────│  AiService      │     │ Users        │
└─────────────┘       └────────┬────────┘     └──────────────┘
│
▼
Claude API
(categorization, reports,
NL queries, anomaly detection)

The AI pipeline follows the same pattern for every feature:
1. Fetch relevant transactions from MySQL via MyBatis
2. Serialize to a structured JSON context string
3. Build a prompt with financial context + user query
4. Call Claude API (streaming for reports, non-streaming for classification)
5. Parse the result and return via REST or SSE stream

---

## Why MyBatis over JPA?

Deliberate choice. JPA generates SQL for you; MyBatis makes you write it. For a portfolio project where explaining technical decisions in interviews matters, full visibility into every query is more valuable than convenience. The transaction filtering feature — six optional filter parameters, a JOIN with the category table, pagination — is a good example of where MyBatis dynamic SQL shines.

---

## Why Claude over OpenAI/DeepSeek?

First-hand familiarity. Using a tool you actually work with daily means you can speak to its behaviour, quirks, and capabilities honestly in an interview — not just cite a name. Claude's long context window also handles the financial data payloads in the monthly report feature cleanly.

---

## Local setup

*Will be filled in once the project is running locally — Day 14.*

---

## What I learned

*Will be filled in at project completion — Day 28.*

---

## Build log

Daily progress tracked in [`docs/build-coach/03-progress-log.md`](docs/build-coach/03-progress-log.md).

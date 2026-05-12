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

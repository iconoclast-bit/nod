---
phase: 2
plan: "02"
type: auto
wave: 1
depends_on: ["1-01"]
---

# Plan: Phase 2 Extraction Pipeline

Build the Extraction Pipeline service that ephemerally retrieves emails from the Gmail API, parses them for chores using Gemini 1.5/3.5 Flash, formats the chores as JSON, and inserts them into PostgreSQL.

## Tasks

- [x] Add Google Client libraries and Gmail API dependencies to pom.xml
- [x] Implement NodCardRepository database save method using Spring's Simple JdbcClient
- [x] Implement GmailService to pull the last 50 emails in-memory (ephemeral)
- [x] Implement GeminiClient to format prompts and execute structured JSON queries using HttpClient
- [x] Implement ExtractionScheduler to define the scheduled hourly extraction job and expose trigger endpoints in NodCardController

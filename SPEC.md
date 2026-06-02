# Project Specification: Nod

Nod is a proactive digital chief of staff that scans user inboxes and calendars, identifies chores, and uses web automation to complete them. The user reviews and approves actions using a Tinder-style swipe interface.

## Tech Stack & Architecture

- **Frontend**: Flutter (targeting iOS and Android mobile platforms).
- **Backend API**: Java 21 and Spring Boot 3.5.
- **Database Persistence**: Spring Data JDBC connected to PostgreSQL. No JPA/Hibernate proxy/lazy-loading mechanisms are permitted. Optimized queries and updates must be executed using Spring's `JdbcClient`.
- **AI Integration**:
  - **Gemini 3.5 Flash**: Low-cost, fast extraction agent scanning input inboxes and calendars.
  - **Gemini 3.1 Pro**: High-reasoning model for planning and executing browser-based DOM actions.
- **Execution Engine**: Google Antigravity Multi-Agent Orchestration, Puppeteer, and Browserbase (for proxying and CAPTCHA bypass).

## Database Schema (PostgreSQL)

### Users Table
```sql
CREATE TABLE users (
    id VARCHAR(255) PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(255),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

### Nod Cards Table
```sql
CREATE TABLE nod_cards (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    chore_type VARCHAR(100) NOT NULL,
    summary_text TEXT NOT NULL,
    status VARCHAR(50) NOT NULL CHECK (status IN ('pending', 'approved', 'rejected', 'completed', 'failed')),
    action_payload JSONB NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
```

## Backend API Specification

### Status Webhook Update Endpoint
- **URL**: `PATCH /api/users/{userId}/nod-cards/{cardId}`
- **Body**:
  ```json
  {
    "status": "approved"
  }
  ```
- **Responses**:
  - `200 OK`: Status successfully updated.
  - `400 Bad Request`: Invalid status value.
  - `404 Not Found`: User or card not found.
  - `500 Internal Server Error`: Database or execution engine error.

### OAuth2 Google Workspace Authentication Boilerplate
- **GET `/api/auth/google`**: Generates and redirects user to Google Workspace Authorization URL with scopes:
  - `https://www.googleapis.com/auth/gmail.readonly`
  - `https://www.googleapis.com/auth/calendar.readonly`
- **GET `/api/auth/google/callback`**: Processes redirected authorization code, retrieves access/refresh tokens.

## Core Pipelines & Mechanics

### 1. Extraction Pipeline (Gmail/Calendar → Card Payload)
- Triggered by a scheduler (cron job).
- Fetches recent messages / events.
- **Ephemerality Constraint**: Raw email text/bodies must be processed completely in-memory and immediately discarded. Do not persist raw inbox content.
- Gemini 3.5 Flash processes in-memory email bodies, outputting a strict JSON array of extracted actions matching the database payload.
- Extracted entries are stored as `pending` cards in the `nod_cards` table.

### 2. Approval Gate Execution (Antigravity Orchestration)
- Execution agent listens or wakes up for `approved` status updates on `nod_cards`.
- Launches a headless Puppeteer session via Browserbase.
- Navigates through target checkout/trial cancel steps.
- **Approval Gate Requirement**: Pauses state just before the final execution click. Updates card status in the DB to `awaiting_user_nod` and serializes/saves the browser session.
- Once Flutter updates status to `approved`, the session is restored, the final click is executed, and card status is updated to `completed` or `failed`.

## Code & File Standards
- **Line Endings**: Unix Line Endings (LF) strictly required on all files.

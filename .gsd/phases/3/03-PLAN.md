---
phase: 3
plan: "03"
type: auto
wave: 1
depends_on: ["2-02"]
---

# Plan: Phase 3 The Execution Agent

Build the Execution Agent in TypeScript/Node.js to run Puppeteer headless automation via Browserbase, query PostgreSQL database for 'approved' cards, perform web automation, pause at the final click, store browser session state (the Approval Gate), and complete execution when the user nods.

## Tasks

- [ ] Initialize `execution-agent/` folder with `package.json` and `tsconfig.json`
- [ ] Implement database integration in TypeScript using `pg` to poll for 'approved' cards
- [ ] Implement Puppeteer browser automation logic mapped to Browserbase proxy structure
- [ ] Implement the Approval Gate: pause execution, update status to `awaiting_user_nod`, store session, and poll database until user approves
- [ ] Implement final confirmation action and status updates (`completed` / `failed`)
- [ ] Verify setup using TypeScript build checks and manual execution

Role: You are an elite AI Architect and Full-Stack Developer specializing in Agentic Workflows, Google Antigravity 2.0, Firebase, and Flutter.

Project Overview: We are building an app called "Nod." It is a proactive digital chief of staff. It scans user inboxes/calendars, identifies chores (e.g., canceling a trial, checking into a flight), and uses web automation to complete them. The user interface is a daily digest of simple swipeable cards (Swipe Right = Execute, Swipe Left = Dismiss).

Tech Stack:

Frontend: Flutter (Targeting iOS/Android).

Backend/Database: Firebase (Authentication, Firestore) and Google Cloud Run.

AI Models: Gemini 3.5 Flash (for fast, cheap data extraction), Gemini 3.1 Pro (for complex DOM manipulation and planning).

Execution: Google Antigravity Multi-Agent Orchestration, Puppeteer, and Browserbase.

Architecture Rules:

Strict separation of concerns between the Extraction Agent (scanning data), Planning Agent (creating steps), and Execution Agent (browser automation).

Ephemerality: User email data must be processed in memory and immediately discarded. Only the actionable task (The "Nod Card") is saved to Firestore.

The Approval Gate: Execution agents must prepare the solution, pause at the final confirmation step, generate a Firestore document representing the pending action, and wait for a frontend webhook to resume and finalize.

Write clean, highly modular, heavily commented Dart and Node.js/TypeScript code.

Phase 1: Backend & Database Schema
Use this prompt in your first coding session to establish the foundation.

Task: Initialize the Firebase backend and database schema for Nod.

Requirements:

Generate the TypeScript code to initialize Firebase Admin SDK.

Design a Firestore schema for a users collection and a sub-collection called nod_cards.

A nod_card document must include fields for: id, chore_type (string), summary_text (string - e.g., "Cancel Spotify Premium"), status (pending, approved, rejected, completed, failed), action_payload (JSON for the execution agent), and created_at (timestamp).

Create a basic Express.js REST API structure hosted on Google Cloud Run to receive webhooks from the Flutter frontend to update the status of a nod_card.

Implement secure OAuth2 boilerplate for Google Workspace (Gmail/Calendar) to request read-only access.

Phase 2: The Extraction Pipeline
Run this prompt to build your first AI agent.

Task: Build the Extraction Agent pipeline using Node.js and the Gemini SDK.

Requirements:

Create a service that fetches the last 50 emails from an authenticated user's inbox using the Gmail API.

Write a function that passes these email bodies to Gemini 3.5 Flash.

Provide a strict system prompt within the code for Gemini 3.5 Flash. It must act as a data extractor. It should identify subscription renewals, flight check-ins, or product return windows.

Force Gemini 3.5 Flash to output a strict JSON array matching the nod_card Firestore schema from Phase 1.

Write the logic to iterate over the JSON array and save these new pending tasks directly into the user's nod_cards Firestore sub-collection.

Phase 3: The Execution Agent (The Heavy Lifter)
This prompt sets up the complex headless browser automation. Ask the AI to start with a mock scenario so you can test the pipeline safely.

Task: Build the Execution Agent using Google Antigravity's Agent Manager, Puppeteer, and Gemini 3.1 Pro.

Requirements:

Create a TypeScript module that listens to the Firestore nod_cards collection for documents where status changes to 'approved'.

Implement a headless browser automation script using Puppeteer (routed through Browserbase for proxy/CAPTCHA management).

Write a specific flow for a mock scenario: "Canceling a dummy subscription on a test website."

Integrate Gemini 3.1 Pro to dynamically inspect the DOM structure of the current page and return the exact XPath or CSS selector needed to click the "Cancel Subscription" button.

Implement the "Approval Gate" logic: The script must pause execution just before clicking the final submit button, update the Firestore document status to 'awaiting_user_nod', and securely store the browser session state.

Implement the resume logic: When the frontend updates the status to 'approved', resume the session and click the button.

Phase 4: The Flutter Frontend
Once your backend is humming, use this prompt to generate your user interface.

Task: Build the "Nod" mobile app frontend using Flutter.

Requirements:

Integrate Firebase Authentication for Google Sign-In.

Create a sleek, minimalist main screen that streams the user's nod_cards from Firestore where the status is 'pending'.

Implement a Tinder-style swipeable card interface using a popular Flutter swipe package (like appinio_swiper or flutter_card_swiper).

Display the summary_text cleanly in the center of the card.

Implement the swipe logic: Swiping Right triggers a backend API call to update the Firestore document status to 'approved'. Swiping Left updates the status to 'rejected'.

Ensure the UI provides immediate haptic feedback and a smooth animation when a card is cleared, rewarding the user with a "Zero Nods Pending" success state when the queue is empty.
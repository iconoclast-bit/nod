---
phase: 1
plan: "01"
type: auto
wave: 1
depends_on: []
---

# Plan: Nod Backend Foundation & Database Schema

Initialize Spring Boot project, define PostgreSQL schemas, implement status webhook update controller, and establish OAuth2 Google Workspace boilerplate.

## Tasks

- [x] Create Maven pom.xml and configuration files
- [x] Create database schema.sql defining users and nod_cards
- [x] Implement NodCardRepository with JDBC simple client
- [x] Expose webhook and OAuth2 redirect/callback endpoints in NodCardController

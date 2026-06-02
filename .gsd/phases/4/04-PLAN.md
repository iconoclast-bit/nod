---
phase: 4
plan: "04"
type: auto
wave: 1
depends_on: ["3-03"]
---

# Plan: Phase 4 The Flutter Frontend

Build the Flutter mobile frontend for Nod. Implement Google Sign-In authentication, fetch pending cards from the Spring Boot API, render swipeable cards using a tinder-swiper package, wire swiping actions to status PATCH API requests, and provide haptic success feedback.

## Tasks

- [ ] Initialize `frontend/` directory using `flutter create`
- [ ] Configure `pubspec.yaml` dependencies (`google_sign_in`, `http`, `flutter_card_swiper`)
- [ ] Implement backend API service in Flutter to handle login authorization and status updates
- [ ] Implement Tinder-style card swiper screen (swiping right = approved, swiping left = rejected)
- [ ] Implement Empty Queue / Zero Nods success screen and configure device haptics
- [ ] Verify setup using `flutter analyze` or basic compile check

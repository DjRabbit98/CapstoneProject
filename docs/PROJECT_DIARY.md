# NIT3004 IT Capstone Project 2 — PROJECT DIARY / PROGRESS REPORT (DRAFT v0.3)

**Project 5: AI-Powered Personalised Wellness Assistant**
Team: Jacqueline Marroquin, Tiago Alves, Dylan Fenech, Ali El-Kardi

> Follows the NEF3002 sample diary: an introduction, a dated work log per subsystem, design constants, database schema, and meeting records. Entries up to 8 Nov are reconstructed from the GitHub commit history; `[TODO]` entries need the person who did the work to fill in. **Every team member adds a dated line here each time they work on the project** (branch + PR, like code).

## Introduction
This document records the team's progress during the build phase (NIT3004). Planning-phase work (requirements, use cases, UI/UX design, risk and resource management) is documented in the NIT3003 Planning Report.

- Code repository: https://github.com/Melonistt/CapstoneProject
- Backend README: `backend/README.md`; test log: `backend/TESTING.md`
- Planning Report: `NIT3003 Planning Report (Project 5).docx`
- Final deadline: 23 November 2026; feature freeze 18 November

## Android App — Work Log

### Project setup, navigation and Home screen
Files: `MainActivity.kt`, `HomeFragment.kt`, `activity_main.xml`, `nav_graph.xml`, `bottom_nav_menu.xml`, `strings.xml`

| Date | Member | Notes |
|---|---|---|
| 01/11/2026 | Tiago | Initial commit; full Android project scaffold ("The whole thing"), then restructured into `app/` module with Gradle Kotlin DSL ("Redo"). Created Login screen, Main activity, and Home/Meal/Train/Progress/Profile fragments with layouts and bottom navigation. |
| 08/11/2026 | Tiago | "de-hardcoding": moved hard-coded text in all five fragment layouts into `strings.xml`. |
| 08/11/2026 | Tiago | "overlay fix" on `activity_main.xml`. |
| 08/11/2026 | Tiago | "Big reform + partial button functionality": reworked MainActivity and HomeFragment, bottom nav menu and nav graph; Home buttons navigate to Training and Log Meal. |

### Authentication screens (Login / Sign Up)
Files: `LoginActivity.kt`, `SignupActivity.kt`, `ProfileFragment.kt` (logout), `activity_login.xml`, `activity_signup.xml`, `network/*`

| Date | Member | Notes |
|---|---|---|
| 04/11/2026 | Tiago | Renamed LoginScreen → LoginActivity; implemented logout in ProfileFragment with error handling. |
| 08/11/2026 | Tiago | Added SignupActivity and layout; login validation (email format, 8-char password), "remember me", forgot-password placeholder. Login/sign-up state stored locally in SharedPreferences pending backend integration. |
| 09/11/2026 | Dylan | Added Retrofit network layer (`ApiClient`, `WellnessApi`, `ApiModels`) and `network_security_config.xml`; wired Sign Up → `POST /auth/register` and Login → `POST /auth/login` with bearer-token storage; sign-up validation aligned to backend password policy. Branch `wire-auth` published; PR #2 opened. Auth endpoints tested (9 cases, `backend/TESTING.md`). |
| 11/11/2026 | Dylan | Installed Android Studio; first build blocked by Trend Micro Folder Shield killing AAPT2 (resolved by trusting `aapt2.exe`). |
| 14/11/2026 | Dylan | App builds and runs on the "Medium Phone API 37" emulator (BUILD SUCCESSFUL 2 m 53 s). |
| 15/11/2026 | Tiago | Merged PR #2 (`wire-auth`) into `main`. |
| 15/11/2026 | Dylan | Emulator could not reach the backend (`SocketTimeoutException` to 10.0.2.2; no website loaded in the emulator's Chrome either). Not fixed by binding the backend to 0.0.0.0, quitting NordVPN, disabling Windows Firewall or restarting the emulator; Android Studio's HTTP proxy was on Auto-detect. Resolved by pointing the app at `127.0.0.1` with `adb reverse tcp:8000 tcp:8000`. First successful sign-up from the app → `POST /auth/register 201`. Fixed logout (now clears the token) and reduced OkHttp logging from BODY to BASIC (it was printing the password to Logcat). Branch `food-log`, PR #3. |

### Profile, Meal, Train, Progress screens
| Date | Member | Notes |
|---|---|---|
| 01/11/2026 | Tiago | Placeholder fragments and layouts created. |
| 15/11/2026 | Dylan | Retrofit methods and models for `/log/*` and `/ai/estimate` added to `WellnessApi.kt` / `ApiModels.kt` so the screens are UI work only. |
| `[TODO]` | Tiago | Food log screen (add meal, "estimate with AI", today's list + total) and Dashboard numbers. |
| `[TODO]` | Dylan | Profile ↔ `GET/PUT /profile`; AI Coach chat screen; Train / Meal-plan tabs rendering `/ai/recommend`. |
| `[TODO]` | Tiago / Ali | Progress tab: weight line chart from `GET /log/weight`. |

## Backend API — Work Log

### FastAPI service, authentication, profile, AI coach, food log
Files: `backend/app/*`

| Date | Member | Notes |
|---|---|---|
| 05/11/2026 | Dylan | Built FastAPI backend on branch `backend-api`: `/auth/register`, `/auth/login`, `/auth/me` (bcrypt, JWT 7-day tokens, failed logins logged with IP, 15-min lockout after 5 failures); `/profile` with daily calorie target (Mifflin-St Jeor); `/ai/chat` and `/ai/recommend` via Gemini with a safety system prompt. SQLite default, PostgreSQL via `DATABASE_URL`. README and `.env.example`. |
| 08/11/2026 | Tiago | Merged PR #1 (`backend-api`) into main. Backend folder moved to `app/src/main/java/backend/`; Retrofit/OkHttp/Gson dependencies added to `app/build.gradle.kts`. |
| 09/11/2026 | Dylan | Backend folder moved back to repo root `backend/`. |
| 10/11/2026 | Dylan | Google AI Studio would not issue a Gemini key on three accounts (VU account: "no Cloud projects available"). Added Groq as an alternative provider (OpenAI-compatible REST, auto-selected when `GROQ_API_KEY` is set). Gemini key obtained from Tiago. |
| 10/11/2026 | Dylan | `google-genai` SDK failed on Windows on every call → Gemini called over plain REST; SDK removed. `gemini-2.5-flash` retired for new keys; `gemini-3.8-flash` times out (thinking model) → `gemini-3.5-flash-lite`. Added `GET /ai/models`. AI coach tested end-to-end: 5 tests passed, all ≤ 4.4 s. |
| 11/11/2026 | Dylan | Manual demo through Swagger UI: register → login → profile → chat → meal plan → workout plan. All passed. |
| 15/11/2026 | Dylan | Food log and progress: `meal_logs` and `weight_logs` tables; `POST/GET/DELETE /log/meals` with day totals, calorie target and calories remaining; `GET /log/summary` (kcal per day); `POST/GET/DELETE /log/weight` with start/latest/change, weigh-in updates the profile weight. `POST /ai/estimate` returns kcal/macros with a confidence flag for a described meal. Every meal row records its `source` (manual / ai_estimate / ai_plan / photo). Tests 16–28 in TESTING.md; one Gemini "high demand" 503 observed and handled as a clean 502. `run.py` binds 0.0.0.0. |

## Design and Style Constants
From Planning Report §UI/UX: primary green #2E7D5B, primary dark #1F5C42, accent amber #F4A340, text #1F2933, background #F4F7F5, error #D75452, info #4A90D9. Type family Inter (Roboto fallback). `[TODO: confirm what the app actually uses in colors.xml / themes.xml]`

## Database Schema
- **users** — id, email, password_hash, first_name, last_name, failed_logins, locked_until, created_at
- **profiles** — user_id, age, sex, height_cm, weight_kg, activity_level, goal, dietary_preferences, allergies, updated_at
- **meal_logs** — id, user_id, date, meal_type, name, kcal, protein_g, carbs_g, fat_g, source, created_at
- **weight_logs** — id, user_id, date, weight_kg, notes, created_at
`[TODO: ERD]`

## Meeting Records
`[TODO: add one block per team meeting held so far — reconstruct from the group chat. Template below, same as the sample. Minute the 18 Nov feature-freeze call properly.]`

**MEETING 1**
Project title: AI-Powered Personalised Wellness Assistant
Team Members: Jacqueline Marroquin, Tiago Alves, Dylan Fenech, Ali El-Kardi
Date: `[dd/mm/2026]`  Time: `[ ]`  Location: `[Discord / Teams / in person]`
Meeting Objective(s): `[ ]`
Attendance — Present: `[ ]`  Absent: `[ ]`
Agenda/Issues:
- Topic 1 `[ ]`
- Topic 2 `[ ]`
Next Meeting: Date `[ ]`  Objective(s) `[ ]`

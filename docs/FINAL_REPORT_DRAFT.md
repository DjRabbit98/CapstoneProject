# NIT3004 IT Capstone Project 2 — FINAL REPORT (DRAFT v0.2, 15 Nov 2026)

**Project 5: AI-Powered Personalised Wellness, Weight Loss, Nutrition and Home Training Assistant**

Team: Jacqueline Marroquin (s8079964), Tiago Alves (s8130002), Dylan Fenech (s8019633), Ali El-Kardi (s8002016)
Supervisor: *[name]*

> Structure follows the NEF3002 Smart Locker sample report. `[TODO]` marks content still to write; `[OWNER]` says who fills it. Screenshots live in `docs/screenshots/` and are referenced as Image x.y.z like the sample. **Edit this file on a branch and open a PR, same as code.**

---

## Table of Contents
1. Introduction — 1.1 Scope Review
2. System Requirements Review — 2.1 Functional, 2.2 Non-Functional
3. Database — 3.1 Database Review, 3.2 Layout and Functionality
4. System Architecture — 4.1 Android App, 4.2 Backend API & AI Coach
5. Testing
6. Challenges and Problems
7. Student Contributions
8. References

---

## 1 Introduction

### 1.1 Scope Review  `[OWNER: Jacqueline — reuse Planning Report intro]`
The project set out to build a mobile wellness assistant that unifies nutrition tracking, AI-generated meal and home-workout plans, and progress tracking in one app, as an affordable alternative to personal trainers and dietitians (Planning Report §1).

**What was delivered (Project 2):** an Android app (Kotlin) with registration, login, a five-tab home screen and profile, connected to a Python FastAPI backend that provides secure authentication, a wellness profile with a daily calorie target, a food log with daily totals, weigh-in tracking, and a Gemini-powered AI coach that answers questions, estimates the nutrition of a described meal, and generates meal plans and home-workout plans tailored to the user's profile, goal, dietary preferences and allergies.

**What was descoped and why:** trained computer-vision meal recognition, wearable integration, 2FA and data export were moved to future work given the 4-week build window. `[TODO: confirm final list at the 18 Nov feature freeze]`

## 2 System Requirements Review

### 2.1 Functional Requirements Review  `[OWNER: Dylan]`
| # | Requirement (Planning Report §2) | Status | Evidence |
|---|---|---|---|
| FR-1 | Generate AI recommendations & assist conversationally | **Implemented** (backend) / app screen `[TODO]` | `POST /ai/chat`, `POST /ai/recommend` (meal_plan / workout_plan, 1–7 days, optional extra request) — TESTING.md 11–15 |
| FR-2 | User registration & authentication (8+ chars, 1 number, 1 special) | **Implemented** (backend + app) | `POST /auth/register`, `/auth/login`; bcrypt; JWT 7-day token; 5 failed attempts → 15 min lock; app Sign Up / Login screens — TESTING.md 1–7, 29–31. Password recovery and 2FA: not implemented |
| FR-3 | Build/maintain a user profile | **Implemented** (backend) / app screen `[TODO]` | `GET/PUT /profile` with daily calorie target (Mifflin-St Jeor) — TESTING.md 8 |
| FR-4 | Log food intake | **Implemented** (backend) / app screen `[TODO: Tiago]` | `POST/GET/DELETE /log/meals`, `source` per entry (manual / ai_estimate / ai_plan / photo) — TESTING.md 16–19 |
| FR-5 | Analyse meals from photos (CV) | **Partial** | `POST /ai/estimate` gives kcal/macros from a text description (confidence flagged); image input is future work — TESTING.md 26–28 |
| FR-6 | Nutrition dashboard | **Implemented** (backend) / app screen `[TODO: Tiago]` | day totals vs calorie target, `GET /log/summary` per-day kcal for the week — TESTING.md 19, 23 |
| FR-7 | Generate & adapt home-training plans | **Implemented** (backend) / app screen `[TODO]` | `/ai/recommend kind=workout_plan` — TESTING.md 15 |
| FR-8 | Track progress towards goals | **Implemented** (backend) / app screen `[TODO]` | `POST/GET /log/weight` with start/latest/change; weigh-in updates the profile and calorie target — TESTING.md 20–22 |

*Summary paragraph in the style of the sample ("We achieved … The ones we didn't were on the edge of our scope …")* `[TODO]`

### 2.2 Non-Functional Requirements Review  `[OWNER: Dylan]`
| NFR | Status | Notes |
|---|---|---|
| AI response ≤ 5 s | **Met** | chat 1.5–2.6 s, plans 2.7–4.4 s, estimates 0.9–1.4 s (TESTING.md 11–15, 26–28) |
| API response ≤ 2 s | **Met** for non-AI endpoints | auth/profile/log calls return in well under 1 s locally `[TODO: record 3 timings]` |
| TLS 1.2+ in transit | **Not in prototype** | Dev build uses http on localhost / adb tunnel; production would sit behind HTTPS |
| bcrypt ≥ cost 12 | **Implemented** | `security.py` |
| MFA (TOTP) opt-in | **Not implemented** | Future work |
| Failed logins logged with timestamp + IP; lock after 5 | **Implemented** | `routers/auth.py`, TESTING.md 6–7 |
| Export / delete personal data | **Not implemented** | Future work |
| AI safety: no medical diagnosis, evidence-based, respects allergies | **Implemented** | System prompt enforced on every call; TESTING.md 13 (chest pain → GP/000), 14 (no peanuts) |
| Australian Privacy Act / APPs | **Partial** | Minimal data collected; no third-party sharing; passwords never logged (fixed 15 Nov, see §6) |

## 3 Database

### 3.1 Database Review  `[OWNER: Jacqueline]`
`[TODO: why SQLite via SQLAlchemy for the prototype and PostgreSQL via DATABASE_URL for production; Room declared in the app for on-device profile caching — decision rationale, like the sample's SQL→Firestore discussion]`

### 3.2 Database Layout and Functionality  `[OWNER: Jacqueline]`
Tables (from `backend/app/models.py`):
- **users** — id, email (unique), password_hash, first_name, last_name, failed_logins, locked_until, created_at
- **profiles** — user_id (PK/FK), age, sex, height_cm, weight_kg, activity_level, goal, dietary_preferences, allergies, updated_at
- **meal_logs** — id, user_id, date, meal_type, name, kcal, protein_g, carbs_g, fat_g, source, created_at
- **weight_logs** — id, user_id, date, weight_kg, notes, created_at
`[TODO: ERD image — Image 3.2.1]`

## 4 System Architecture

### 4.1 Android App  `[OWNER: Tiago]`
#### 4.1.1 Technologies Used
Kotlin, Android Studio, XML layouts with ViewBinding, Material Components, Navigation component (bottom nav), Retrofit 3 + OkHttp 5 + Gson, Kotlin coroutines, Room (declared), Hilt (declared), GitHub. `[TODO: one short paragraph each with a citation, as in the sample]`
#### 4.1.2 UI Descriptions
Screens: Login, Sign Up, Home, Meal, Train, Progress, Profile. `[TODO: one screenshot + 2–3 sentences each — Images 4.1.2.1 … 4.1.2.7, files in docs/screenshots/]`
#### 4.1.3 App Subsystems Explained
- Authentication flow (LoginActivity / SignupActivity → ApiClient → backend) `[TODO: code screenshots]`
- Network layer (`network/ApiClient.kt`, `WellnessApi.kt`, `ApiModels.kt`): single Retrofit instance, bearer-token interceptor, error-message mapping, BASIC-level HTTP logging
- Navigation (`nav_graph.xml`, `bottom_nav_menu.xml`)
- Dev connectivity: `adb reverse tcp:8000 tcp:8000` tunnels the emulator to the laptop's backend (see backend/README.md)

### 4.2 Backend API and AI Coach  `[OWNER: Dylan]`
#### 4.2.1 Technologies Used
Python 3.12, FastAPI, Uvicorn, SQLAlchemy 2, Pydantic 2, PyJWT, bcrypt, python-dotenv, SQLite. Gemini (`gemini-3.5-flash-lite`) and Groq called over plain REST with the standard library — no vendor SDK. `[TODO: short justification + citation each]`
#### 4.2.2 Endpoints
| Method | Path | Purpose |
|---|---|---|
| POST | /auth/register | Create account (password policy) |
| POST | /auth/login | Bearer token (7 days), failed-login logging, lockout |
| GET | /auth/me | Current user |
| GET / PUT | /profile | Wellness profile + calorie target (Mifflin-St Jeor) |
| POST | /ai/chat | Conversational coach with history |
| POST | /ai/recommend | meal_plan / workout_plan, 1–7 days, JSON |
| POST | /ai/estimate | kcal + macros for a described meal, with confidence |
| GET | /ai/models | Models the configured key can use |
| POST / GET / DELETE | /log/meals | Food log; day view with totals, target, remaining |
| GET | /log/summary | kcal per day for the last N days |
| POST / GET / DELETE | /log/weight | Weigh-ins; history with start/latest/change |
| GET | /health | Liveness |
#### 4.2.3 Backend Subsystems Explained
- `main.py` app wiring; `routers/*`; `security.py` (hashing, JWT, dependencies); `nutrition.py` (BMR/TDEE → calorie target); `ai_service.py` (provider selection, SAFETY_RULES system prompt, JSON mode for plans and estimates, vendor-error mapping to 502/503) `[TODO: walk through each with code screenshots — Images 4.2.3.x]`
- Where nutrition numbers come from: user entry, AI estimate (confirmed by the user), or an AI meal plan; every meal log row records its `source`. Calorie targets are computed deterministically, not by the AI.
- Profile can be sent in the request body (on-device profile) or resolved from the bearer token (server profile)

## 5 Testing  `[OWNER: Ali — convert backend/TESTING.md into report tables]`
31 tests to date in `backend/TESTING.md`: auth & profile (1–9), AI coach (10–15), food log & progress (16–25), AI estimate (26–28), app ↔ backend on the emulator (29–31). `[TODO: table per group + NFR timings + screenshots of Swagger responses and the PyCharm log]`

## 6 Challenges and Problems  `[OWNER: all — each adds 1–2]`
- Backend folder relocated into `app/src/main/java/` during a merge (8 Nov) → moved back to repo root; team moved to branch + PR workflow
- Google AI Studio would not issue a Gemini API key on three accounts (university Google Workspace blocks Cloud projects); key obtained from a teammate; Groq added as an alternative provider
- `google-genai` SDK failed on Windows on every call ("client has been closed") → replaced with direct REST calls; `gemini-2.5-flash` retired for new keys; `gemini-3.8-flash` timed out as a thinking model → `gemini-3.5-flash-lite`
- Gemini free tier occasionally returns 503 "high demand" → surfaced as a clean 502; app to offer Retry
- Trend Micro Folder Shield blocked Android's AAPT2 from writing build files under Documents → trusted the program; Claude desktop app's file writes also needed trusting
- Emulator had no network on a laptop with a VPN client and Android Studio's HTTP proxy on Auto-detect → app points at 127.0.0.1 via `adb reverse`, independent of guest networking
- OkHttp logging at BODY level wrote the sign-up password to Logcat → reduced to BASIC (security lesson for the report)
- `[TODO: Tiago — Android Studio / Gradle / Hilt issues]`
- Computer vision descoped: dataset + model training not feasible in timeframe; text-based AI estimate delivered instead

## 7 Student Contributions  `[OWNER: each member writes own, agree % on 18 Nov]`
| Student ID | Name | Role | % |
|---|---|---|---|
| s8130002 | Tiago Alves | Android app lead | `[TODO]` |
| s8019633 | Dylan Fenech | Backend & AI integration lead | `[TODO]` |
| s8079964 | Jacqueline Marroquin | `[TODO]` | `[TODO]` |
| s8002016 | Ali El-Kardi | `[TODO]` | `[TODO]` |

*Dylan (draft):* Designed and built the FastAPI backend — registration/login with bcrypt and JWT, failed-login logging and lockout per the planning NFRs, wellness profile with calorie target, food log with daily totals, weigh-in tracking, and the AI coach (chat, meal/workout plans, nutrition estimates) with a safety system prompt and a provider layer for Gemini and Groq. Wrote the backend README, `.env.example` and the test log (31 tests). Wired the Android login/registration screens to the API (Retrofit client, token storage, logout), set up the emulator and resolved the build and network blockers. Drafted the requirements review, final report skeleton and project diary. `[TODO: expand to ~1 page like the sample]`

Code repository: https://github.com/Melonistt/CapstoneProject

## 8 References  `[OWNER: Ali — carry over Planning Report bibliography + add tech references: FastAPI, SQLAlchemy, Pydantic, Retrofit, OkHttp, Kotlin, Android Developers, Gemini API, Groq, Mifflin-St Jeor (1990)]`

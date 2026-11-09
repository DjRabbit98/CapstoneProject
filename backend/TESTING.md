# Backend API — test log

## Auth & profile endpoint tests — 9 Nov 2026 (Dylan)
Backend run locally (`python run.py`), SQLite, no GEMINI_API_KEY set. Requests sent exactly as the Android Login/Sign Up screens send them.

| # | Test | Request | Result | Status |
|---|---|---|---|---|
| 1 | Register new user | POST /auth/register test@example.com / Passw0rd! | user id + names returned | 201 ✅ |
| 2 | Register duplicate email | same as 1 | "Email already registered" | 409 ✅ |
| 3 | Register weak password | password = "password" | "Password must contain at least one number" | 422 ✅ |
| 4 | Login correct password | POST /auth/login | JWT bearer token (165 chars) | 200 ✅ |
| 5 | Current user with token | GET /auth/me | same user as test 1 | 200 ✅ |
| 6 | Login wrong password | POST /auth/login | "Incorrect email or password"; failed attempt logged with timestamp + IP | 401 ✅ |
| 7 | 5th consecutive failure, then correct password | POST /auth/login ×5 | "Account locked. Try again in 15 minutes." | 423 ✅ (NFR: lock after 5) |
| 8 | Save profile | PUT /profile age 28, male, 180 cm, 90 kg, moderate, weight_loss, allergy peanuts | profile echoed with daily_calorie_target = 2430 kcal | 200 ✅ |
| 9 | AI chat with no API key | POST /ai/chat | "AI service not configured (GEMINI_API_KEY missing)" — clean failure, no crash | 503 ✅ |

Still to test: /ai/chat and /ai/recommend with a real GEMINI_API_KEY (response time for NFR ≤ 5 s; allergen never present in meal plan); end-to-end from the Android emulator.

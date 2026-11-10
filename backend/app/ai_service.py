"""AI provider wrapper. Routes never touch a vendor SDK directly, so swapping provider is a change here only.

Provider selection (env AI_PROVIDER): "groq", "gemini", or unset = auto (Groq if GROQ_API_KEY is set, else Gemini).
Groq is called through its OpenAI-compatible REST API with the standard library (no extra package).
"""
import json
import logging
import os
import urllib.error
import urllib.request

from fastapi import HTTPException, status

log = logging.getLogger("ai")

SAFETY_RULES = """You are the AI coach inside a wellness app for general fitness, nutrition and wellbeing.
Rules you must always follow:
- You do not give medical diagnoses, clinical advice or medication advice. If the user describes symptoms,
  an injury, pregnancy, an eating disorder or a medical condition, tell them kindly to see a GP or
  accredited practising dietitian, and keep any tips general.
- Base advice on mainstream, evidence-based guidance (e.g. Australian Dietary Guidelines, the
  Australian Physical Activity Guidelines). Never suggest crash diets, fasting for days, under-eating
  below the provided calorie target, dangerous supplements or unsafe exercise.
- Respect the user's dietary preferences and allergies at all times; never include a listed allergen.
- Be friendly, concise and practical. Use metric units and Australian spelling."""

GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"


# ---------------------------------------------------------------- provider selection
def provider() -> str:
    p = os.getenv("AI_PROVIDER", "").strip().lower()
    if p in ("groq", "gemini"):
        return p
    return "groq" if os.getenv("GROQ_API_KEY") else "gemini"


def model_name() -> str:
    if provider() == "groq":
        return os.getenv("GROQ_MODEL", "llama-3.3-70b-versatile")
    return os.getenv("GEMINI_MODEL", "gemini-2.5-flash")


def _require_key(var: str) -> str:
    key = os.getenv(var)
    if not key:
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, f"AI service not configured ({var} missing)")
    return key


# ---------------------------------------------------------------- shared helpers
def profile_context(profile, calorie_target) -> str:
    if profile is None:
        return "The user has not filled in their profile yet; ask for key details if needed."
    fields = {
        "age": profile.age,
        "sex": profile.sex,
        "height_cm": profile.height_cm,
        "weight_kg": profile.weight_kg,
        "activity_level": profile.activity_level,
        "goal": profile.goal,
        "dietary_preferences": profile.dietary_preferences,
        "allergies": profile.allergies,
        "daily_calorie_target_kcal": calorie_target,
    }
    known = {k: v for k, v in fields.items() if v not in (None, "")}
    return "User profile: " + json.dumps(known)


def _wrap_vendor_error(exc: Exception) -> HTTPException:
    # network, quota, bad key, etc. (planning report risk R4) -> clean 502, app keeps working
    log.exception("AI call failed (%s)", provider())
    detail = f"AI service error: {type(exc).__name__}"
    if isinstance(exc, urllib.error.HTTPError):
        detail = f"AI service error: HTTP {exc.code}"
        if exc.code == 401:
            detail += " (check GROQ_API_KEY)"
        elif exc.code == 429:
            detail += " (rate limit, try again shortly)"
    return HTTPException(status.HTTP_502_BAD_GATEWAY, detail)


# ---------------------------------------------------------------- Groq (OpenAI-compatible)
def _groq_generate(messages: list[dict], system: str, json_mode: bool) -> str:
    key = _require_key("GROQ_API_KEY")
    body = {
        "model": model_name(),
        "messages": [{"role": "system", "content": system}] + messages,
        "temperature": 0.7,
        "max_tokens": 4096,
    }
    if json_mode:
        body["response_format"] = {"type": "json_object"}
    req = urllib.request.Request(
        GROQ_URL,
        data=json.dumps(body).encode(),
        headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            data = json.loads(resp.read())
    except Exception as exc:
        raise _wrap_vendor_error(exc) from exc
    return (data["choices"][0]["message"]["content"] or "").strip()


# ---------------------------------------------------------------- Gemini
def _gemini_generate(messages: list[dict], system: str, json_mode: bool) -> str:
    key = _require_key("GEMINI_API_KEY")
    from google import genai
    from google.genai import types

    contents = [
        {"role": "model" if m["role"] == "assistant" else "user", "parts": [{"text": m["content"]}]} for m in messages
    ]
    config = types.GenerateContentConfig(
        system_instruction=system,
        temperature=0.7,
        response_mime_type="application/json" if json_mode else None,
    )
    try:
        resp = genai.Client(api_key=key).models.generate_content(model=model_name(), contents=contents, config=config)
    except Exception as exc:
        raise _wrap_vendor_error(exc) from exc
    return (resp.text or "").strip()


def _generate(messages: list[dict], system: str, json_mode: bool = False) -> str:
    """messages: OpenAI-style [{"role": "user"|"assistant", "content": str}, ...]"""
    if provider() == "groq":
        return _groq_generate(messages, system, json_mode)
    return _gemini_generate(messages, system, json_mode)


# ---------------------------------------------------------------- public API used by routers
def chat(message: str, history: list, context: str) -> str:
    messages = [{"role": t.role, "content": t.text} for t in history]
    messages.append({"role": "user", "content": message})
    return _generate(messages, SAFETY_RULES + "\n\n" + context)


MEAL_SCHEMA = """Return JSON only, shaped like:
{"days":[{"day":1,"total_kcal":0,"meals":[{"name":"","meal_type":"breakfast|lunch|dinner|snack",
"kcal":0,"protein_g":0,"carbs_g":0,"fat_g":0,"ingredients":[""],"instructions":""}]}],
"notes":"","disclaimer":""}"""

WORKOUT_SCHEMA = """Return JSON only, shaped like:
{"days":[{"day":1,"focus":"","duration_min":0,"exercises":[{"name":"","type":"walking|running|cardio|strength|stretching",
"sets":0,"reps":"","duration_min":0,"instructions":""}]}],"notes":"","disclaimer":""}
Exercises must be doable at home with little or no equipment."""


def recommend(kind: str, days: int, extra: str | None, context: str) -> dict:
    schema = MEAL_SCHEMA if kind == "meal_plan" else WORKOUT_SCHEMA
    what = "meal plan" if kind == "meal_plan" else "home training plan"
    prompt = f"Create a {days}-day personalised {what} for this user."
    if extra:
        prompt += f" Extra request from the user: {extra}"
    raw = _generate([{"role": "user", "content": prompt}], SAFETY_RULES + "\n\n" + context + "\n\n" + schema, json_mode=True)
    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        log.warning("AI returned non-JSON: %.200s", raw)
        raise HTTPException(status.HTTP_502_BAD_GATEWAY, "AI returned an unreadable plan, please try again")

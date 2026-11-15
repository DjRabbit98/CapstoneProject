import logging

from dotenv import load_dotenv

load_dotenv()

from fastapi import FastAPI  # noqa: E402

from .database import Base, engine  # noqa: E402
from .routers import ai, auth, log, profile  # noqa: E402

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")

Base.metadata.create_all(bind=engine)

app = FastAPI(title="AI Wellness Assistant API", version="0.2.0")
app.include_router(auth.router)
app.include_router(profile.router)
app.include_router(log.router)
app.include_router(ai.router)


@app.get("/health", tags=["system"])
def health():
    return {"status": "ok"}

"""Food log and weigh-ins: the data behind the Dashboard, Food Log and Progress screens.

Nutrition numbers are whatever the user saved: typed by hand, taken from an AI estimate (/ai/estimate),
or copied from an AI meal plan. Every entry carries `source` so the app can show where a number came from.
"""
from datetime import date, timedelta

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from ..database import get_db
from ..models import MealLog, Profile, User, WeightLog
from ..nutrition import daily_calorie_target
from ..schemas import (
    DayLogOut,
    DaySummary,
    DayTotals,
    MealLogIn,
    MealLogOut,
    WeightHistoryOut,
    WeightLogIn,
    WeightLogOut,
)
from ..security import get_current_user

router = APIRouter(prefix="/log", tags=["log"])


def _today() -> str:
    return date.today().isoformat()


# ------------------------------------------------------------------ meals
@router.post("/meals", response_model=MealLogOut, status_code=status.HTTP_201_CREATED)
def add_meal(body: MealLogIn, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    entry = MealLog(user_id=user.id, **body.model_dump(exclude_unset=True))
    entry.date = body.date or _today()
    db.add(entry)
    db.commit()
    db.refresh(entry)
    return entry


@router.get("/meals", response_model=DayLogOut)
def day_log(
    day: str | None = Query(None, alias="date", pattern=r"^\d{4}-\d{2}-\d{2}$", description="YYYY-MM-DD, default today"),
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Everything logged on one day plus totals and how it compares with the profile's calorie target."""
    day = day or _today()
    meals = db.scalars(
        select(MealLog).where(MealLog.user_id == user.id, MealLog.date == day).order_by(MealLog.created_at)
    ).all()
    totals = DayTotals(
        kcal=sum(m.kcal for m in meals),
        protein_g=round(sum(m.protein_g or 0 for m in meals), 1),
        carbs_g=round(sum(m.carbs_g or 0 for m in meals), 1),
        fat_g=round(sum(m.fat_g or 0 for m in meals), 1),
    )
    target = daily_calorie_target(db.get(Profile, user.id))
    return DayLogOut(
        date=day,
        meals=meals,
        totals=totals,
        calorie_target=target,
        calories_remaining=(target - totals.kcal) if target is not None else None,
    )


@router.delete("/meals/{meal_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_meal(meal_id: str, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    entry = db.get(MealLog, meal_id)
    if entry is None or entry.user_id != user.id:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Meal not found")
    db.delete(entry)
    db.commit()


@router.get("/summary", response_model=list[DaySummary])
def summary(
    days: int = Query(7, ge=1, le=90),
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Calories per day for the last N days (oldest first), zero-filled, for the dashboard / weekly chart."""
    start = date.today() - timedelta(days=days - 1)
    rows = db.execute(
        select(MealLog.date, func.coalesce(func.sum(MealLog.kcal), 0), func.count(MealLog.id))
        .where(MealLog.user_id == user.id, MealLog.date >= start.isoformat())
        .group_by(MealLog.date)
    ).all()
    by_day = {d: (int(k), int(n)) for d, k, n in rows}
    out = []
    for i in range(days):
        d = (start + timedelta(days=i)).isoformat()
        kcal, n = by_day.get(d, (0, 0))
        out.append(DaySummary(date=d, kcal=kcal, meals=n))
    return out


# ------------------------------------------------------------------ weight
@router.post("/weight", response_model=WeightLogOut, status_code=status.HTTP_201_CREATED)
def add_weight(body: WeightLogIn, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """Record a weigh-in. Also updates the profile weight so the calorie target follows the user's progress."""
    entry = WeightLog(user_id=user.id, **body.model_dump(exclude_unset=True))
    entry.date = body.date or _today()
    db.add(entry)
    profile = db.get(Profile, user.id)
    if profile is None:
        profile = Profile(user_id=user.id)
        db.add(profile)
    profile.weight_kg = body.weight_kg
    db.commit()
    db.refresh(entry)
    return entry


@router.get("/weight", response_model=WeightHistoryOut)
def weight_history(
    days: int = Query(90, ge=1, le=730),
    user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Weigh-ins over the last N days, oldest first, with the overall change — the Progress chart's data."""
    start = (date.today() - timedelta(days=days - 1)).isoformat()
    entries = db.scalars(
        select(WeightLog)
        .where(WeightLog.user_id == user.id, WeightLog.date >= start)
        .order_by(WeightLog.date, WeightLog.created_at)
    ).all()
    first = entries[0].weight_kg if entries else None
    last = entries[-1].weight_kg if entries else None
    return WeightHistoryOut(
        entries=entries,
        start_kg=first,
        latest_kg=last,
        change_kg=round(last - first, 1) if entries else None,
    )


@router.delete("/weight/{entry_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_weight(entry_id: str, user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    entry = db.get(WeightLog, entry_id)
    if entry is None or entry.user_id != user.id:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "Entry not found")
    db.delete(entry)
    db.commit()

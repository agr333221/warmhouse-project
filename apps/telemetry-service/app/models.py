"""Модели данных телеметрии."""

from datetime import datetime, timezone

from pydantic import BaseModel, Field


class TelemetryCreate(BaseModel):
    """Входящее измерение."""

    device_id: str
    metric: str = "temperature"
    value: float
    unit: str | None = None
    recorded_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))


class TelemetryRecord(TelemetryCreate):
    """Сохранённое измерение."""

    id: int

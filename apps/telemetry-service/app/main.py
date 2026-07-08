"""Telemetry Service — MVP микросервис телеметрии (Python).

Принимает и отдаёт телеметрию устройств. Интегрируется с монолитом
smart_home: фоновый опросчик периодически читает показания сенсоров
через API монолита и сохраняет их как записи телеметрии
(паттерн Strangler Fig — постепенный вынос функциональности).
"""

import asyncio
import contextlib
import logging
import os

from fastapi import FastAPI, HTTPException

from .models import TelemetryCreate, TelemetryRecord
from .poller import MonolithPoller
from .repository import TelemetryRepository

logging.basicConfig(level=logging.INFO)
log = logging.getLogger("telemetry-service")

repository = TelemetryRepository()
poller = MonolithPoller(
    repository=repository,
    monolith_url=os.getenv("SMARTHOME_API_URL", "http://app:8080"),
    interval_seconds=float(os.getenv("POLL_INTERVAL_SECONDS", "10")),
)


@contextlib.asynccontextmanager
async def lifespan(_: FastAPI):
    task = asyncio.create_task(poller.run())
    yield
    task.cancel()
    with contextlib.suppress(asyncio.CancelledError):
        await task


app = FastAPI(title="Telemetry Service", version="1.0.0", lifespan=lifespan)


@app.get("/health")
async def health() -> dict:
    return {"status": "ok", "service": "telemetry-service"}


@app.post("/api/v1/telemetry", response_model=TelemetryRecord, status_code=201)
async def ingest(measurement: TelemetryCreate) -> TelemetryRecord:
    """Приём одного измерения телеметрии."""
    return repository.add(measurement)


@app.get("/api/v1/telemetry/{device_id}/latest", response_model=TelemetryRecord)
async def latest(device_id: str, metric: str | None = None) -> TelemetryRecord:
    """Последнее измерение устройства (опционально по метрике)."""
    record = repository.latest(device_id, metric)
    if record is None:
        raise HTTPException(status_code=404, detail="no telemetry for device")
    return record


@app.get("/api/v1/telemetry/{device_id}", response_model=list[TelemetryRecord])
async def history(device_id: str, metric: str | None = None, limit: int = 100) -> list[TelemetryRecord]:
    """История измерений устройства."""
    return repository.history(device_id, metric, limit)

"""Интеграция с монолитом: периодический сбор показаний сенсоров."""

import asyncio
import logging

import httpx

from .models import TelemetryCreate
from .repository import TelemetryRepository

log = logging.getLogger("telemetry-service.poller")


class MonolithPoller:
    """Опрашивает API монолита и складывает показания сенсоров в телеметрию."""

    def __init__(self, repository: TelemetryRepository, monolith_url: str, interval_seconds: float = 10.0) -> None:
        self._repository = repository
        self._monolith_url = monolith_url.rstrip("/")
        self._interval = interval_seconds

    async def run(self) -> None:
        async with httpx.AsyncClient(timeout=10.0) as client:
            while True:
                try:
                    await self._poll_once(client)
                except Exception as exc:  # монолит может быть ещё не готов
                    log.warning("poll failed: %s", exc)
                await asyncio.sleep(self._interval)

    async def _poll_once(self, client: httpx.AsyncClient) -> None:
        response = await client.get(f"{self._monolith_url}/api/v1/sensors")
        response.raise_for_status()
        sensors = response.json() or []
        for sensor in sensors:
            self._repository.add(
                TelemetryCreate(
                    device_id=str(sensor["id"]),
                    metric=sensor.get("type", "temperature"),
                    value=float(sensor.get("value", 0)),
                    unit=sensor.get("unit") or None,
                )
            )
        log.info("collected telemetry for %d sensors", len(sensors))

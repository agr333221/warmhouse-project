"""Хранилище телеметрии (MVP: in-memory)."""

import itertools
import threading

from .models import TelemetryCreate, TelemetryRecord


class TelemetryRepository:
    """Потокобезопасное in-memory хранилище измерений."""

    def __init__(self) -> None:
        self._records: list[TelemetryRecord] = []
        self._ids = itertools.count(1)
        self._lock = threading.Lock()

    def add(self, measurement: TelemetryCreate) -> TelemetryRecord:
        record = TelemetryRecord(id=next(self._ids), **measurement.model_dump())
        with self._lock:
            self._records.append(record)
        return record

    def latest(self, device_id: str, metric: str | None = None) -> TelemetryRecord | None:
        with self._lock:
            for record in reversed(self._records):
                if record.device_id == device_id and (metric is None or record.metric == metric):
                    return record
        return None

    def history(self, device_id: str, metric: str | None = None, limit: int = 100) -> list[TelemetryRecord]:
        with self._lock:
            matched = [
                r for r in self._records
                if r.device_id == device_id and (metric is None or r.metric == metric)
            ]
        return matched[-limit:]

# Тёплый дом — проектная работа спринта 1

Решение проектной работы: анализ монолита,
 проектирование микросервисной архитектуры экосистемы умного дома
 и MVP.

Описание решений — в [Project_template.md](Project_template.md).

## Структура репозитория

- [Project_template.md](Project_template.md) — решения.
- [schemas/](schemas) — диаграммы PlantUML
  (C4 Context / Containers / Components / Code, ER) и их SVG-рендеры.
- [api-docs/](api-docs) — спецификации API (OpenAPI 3.0, AsyncAPI 3.0).
- [apps/](apps) — приложения: монолит `smart_home`, `temperature-api`,
  MVP-микросервисы `device-service` (Java) и `telemetry-service` (Python).

## Быстрый старт

```bash
cd apps
docker-compose up -d --build
```

- Монолит: http://localhost:8080 (`/api/v1/sensors`)
- Temperature API: http://localhost:8081 (`/temperature?location=Kitchen`)
- Device Service: http://localhost:8082 (`/api/v1/devices/{id}`)
- Telemetry Service: http://localhost:8083 (`/api/v1/telemetry/{deviceId}/latest`)

Проверка через Postman: коллекция
 [apps/smarthome-api.postman_collection.json](apps/smarthome-api.postman_collection.json) (Create Sensor, Get All Sensors).

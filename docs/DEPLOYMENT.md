# Развёртывание

## Локально (Docker)

```bash
docker compose up --build          # PostgreSQL, backend :8000, веб-клиент :5173
```

Backend при старте применяет миграции (`alembic upgrade head`), загружает сценарии из `backend/scenarios`
и создаёт синтетических сотрудников на пустой базе (`SEED_DEMO_DATA=true`).
Сценарии и `backend/config` смонтированы с диска: правка файла + `docker compose restart backend`.

## Переменные окружения

| Переменная | Назначение | По умолчанию |
|------------|-----------|--------------|
| `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`, `POSTGRES_HOST`, `POSTGRES_PORT` | база | из `.env.example` |
| `JWT_SECRET` | подпись JWT, ≥ 32 символа | демо-значение |
| `JWT_TTL_MINUTES` | срок access-токена | 720 |
| `REFRESH_TTL_DAYS` | срок refresh-токена | 30 |
| `LOGIN_MAX_FAILURES`, `LOGIN_MAX_FAILURES_PER_IP`, `LOGIN_WINDOW_SECONDS` | лимит неудачных входов | 5, 20, 900 |
| `INTEGRATION_API_KEY` | ключ HR/LMS/биллинга, ≥ 32 символа | демо-значение |
| `SEED_DEMO_DATA` | создать синтетические данные | true |

Для любого окружения, кроме демо: `cp .env.example .env` и заменить секреты — `.env` не коммитится.

## Мобильные приложения

| | iOS | Android |
|--|-----|---------|
| Отладка | `API_BASE_URL = http://localhost:8000` (симулятор видит Mac) | `http://10.0.2.2:8000/` (эмулятор видит компьютер) |
| Устройство в той же сети | в `ios/project.yml` → `configs.Debug.API_BASE_URL` адрес компьютера | `buildConfigField` в `app/build.gradle.kts` |
| Продакшен | `xcodebuild … API_BASE_URL=https://api.example/` | `./gradlew assembleRelease -PapiBaseUrl=https://api.example/` |

Release-сборки подписываются ключами организации (Apple Developer, keystore Android); ключи в репозитории не хранятся.

## Продакшен

- Backend stateless — масштабируется копиями за балансировщиком; конфликты решает PostgreSQL
  (`FOR UPDATE`, `ON CONFLICT`, advisory lock), время берётся из БД. Подробнее — [ARCHITECTURE.md](ARCHITECTURE.md).
- Веб-клиент: `npm run build` и раздача `frontend/dist` через nginx.
- Миграции лучше вынести в отдельный шаг деплоя: `alembic upgrade head`.
- Журналы — JSON в stdout: собираются любым агрегатором (Loki, ELK) и ищутся по `request_id`.
- CI — `.github/workflows/ci.yml`: backend (тесты на PostgreSQL), frontend (tsc + сборка), iOS (тесты ядра
  + сборка под симулятор), Android (lint, тесты, APK).

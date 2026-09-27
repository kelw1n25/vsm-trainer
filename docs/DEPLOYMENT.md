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

## Хостинг Render (бесплатный тариф)

`render.yaml` в корне — Blueprint: один веб-сервис и бесплатный PostgreSQL в регионе Frankfurt.

- Образ собирается из корневого `Dockerfile`: сначала веб-клиент (`npm run build`), затем backend; FastAPI отдаёт
  и API `/api/*`, и сайт (переменная `WEB_DIR`). Один адрес — не нужны CORS и прокси между сервисами.
  Код frontend и backend по-прежнему раздельный; для локального запуска — docker-compose, как выше.
- `JWT_SECRET` и `INTEGRATION_API_KEY` Render генерирует сам, `POSTGRES_*` берутся из базы — секретов в репозитории нет.
- Миграции применяются при старте, демо-данные создаются на пустой базе.
- uvicorn запускается с `--proxy-headers`: адрес клиента берётся из `X-Forwarded-For` прокси Render, иначе лимит
  неудачных входов по IP был бы общим для всех.

Развернуть: репозиторий на GitHub → в Render «New → Blueprint» → выбрать репозиторий → «Apply».
Ограничения бесплатного тарифа: сервис засыпает после 15 минут без запросов (первое открытие — до минуты),
бесплатная база Render удаляется через 30 дней.

## Android-приложение

| | Адрес API |
|--|-----------|
| Отладка | `http://10.0.2.2:8000/` (эмулятор видит компьютер) |
| Устройство в той же сети | адрес компьютера в `buildConfigField` в `app/build.gradle.kts` |
| Продакшен | `./gradlew assembleRelease -PapiBaseUrl=https://api.example/` |

Release-сборка подписывается keystore организации; ключи в репозитории не хранятся.

## Продакшен

- Backend stateless — масштабируется копиями за балансировщиком; конфликты решает PostgreSQL
  (`FOR UPDATE`, `ON CONFLICT`, advisory lock), время берётся из БД. Подробнее — [ARCHITECTURE.md](ARCHITECTURE.md).
- Веб-клиент: `npm run build` и раздача `frontend/dist` через nginx.
- Миграции лучше вынести в отдельный шаг деплоя: `alembic upgrade head`.
- Журналы — JSON в stdout: собираются любым агрегатором (Loki, ELK) и ищутся по `request_id`.
- CI — `.github/workflows/ci.yml`: backend (тесты на PostgreSQL), frontend (tsc + сборка), Android (lint, тесты, APK).

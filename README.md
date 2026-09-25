# ВСМ-тренажёр проводника

Веб-приложение для хакатона Московского транспорта, кейс «Геймификация для ВСМ».
Проводник проходит нелинейные сценарии нештатных ситуаций, принимает решения под таймером,
видит последствия на шкалах «Лояльность пассажира» и «Рейтинг безопасности» и получает очки компетенций.

## Запуск

Нужны Docker и Docker Compose.

```bash
cp .env.example .env
docker compose up --build
```

| Что | Адрес |
|-----|-------|
| Приложение | http://localhost:5173 |
| Swagger (документация API) | http://localhost:8000/api/docs |
| Проверка backend и БД | http://localhost:8000/api/health |

Миграции БД применяются автоматически при старте backend (`alembic upgrade head`).

## Сценарии

Сценарии — YAML-файлы в `backend/scenarios/`, загружаются в БД при старте backend.
Файл с ошибками не загружается (в логе — понятный список ошибок), остальные работают.

```bash
# проверить сценарии без перезапуска
docker compose exec backend python -m app.scenarios.validator
# применить изменения
docker compose restart backend
```

JSON-схема формата: http://localhost:8000/api/scenarios/schema

## Тесты

```bash
cd backend
pip install -r requirements-dev.txt
pytest
```

## Структура

```
backend/             FastAPI (Python 3.12)
frontend/            React + TypeScript + Vite
docker-compose.yml   PostgreSQL + backend + frontend
.env.example         шаблон переменных окружения (реальный .env не коммитится)
```

Все данные в проекте синтетические.

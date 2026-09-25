# API

Полная интерактивная документация — Swagger: http://localhost:8000/api/docs
(OpenAPI-схема: `/api/openapi.json`).

## Формат ошибок

Все ошибки возвращаются в одном формате: `code` — для программ, `message` — для человека.

```json
{"detail": {"code": "time_expired", "message": "Время на решение истекло, ответ не принят"}}
```

| HTTP | code | Когда |
|------|------|-------|
| 401 | `not_authenticated`, `invalid_token`, `invalid_credentials`, `invalid_api_key` | нет или неверный токен, пароль, ключ |
| 403 | `forbidden` | раздел только для инструктора |
| 404 | `scenario_not_found`, `run_not_found`, `employee_not_found` | объекта нет (или он чужой) |
| 409 | `time_expired` | ответ пришёл после таймера — таймаут уже применён |
| 409 | `stale_node` | повторная отправка или ответ на устаревший шаг |
| 409 | `run_finished`, `run_in_progress` | действие не подходит к статусу прохождения |
| 422 | `validation_error`, `unknown_choice` | некорректные данные, несуществующий или скрытый вариант |

## Интеграция с HR, LMS и биллингом

Доступ по заголовку `X-API-Key`. Ключ задаётся переменной окружения `INTEGRATION_API_KEY`
(в демо — значение из `.env.example`). JWT пользователей к этому API не подходит.

```bash
export KEY=demo-only-integration-key-replace-me-32
export API=http://localhost:8000/api/integration
```

Значения в примерах ответов условные — они зависят от сгенерированных данных.

### Профиль компетенций сотрудника

```bash
curl -s "$API/employees/100001" -H "X-API-Key: $KEY"
```

```json
{
  "personnel_number": "100001",
  "full_name": "Смирнов Андрей Сергеевич",
  "role": "conductor",
  "brigade": "Бригада 1",
  "depot": "Депо Москва",
  "level": 2,
  "level_title": "Проводник",
  "xp": 540,
  "competences": {"communication": 45, "first_aid": 15, "safety_rules": 40, "service": 30, "stress_resistance": 16},
  "achievements": ["diplomat", "flawless"],
  "runs_completed": 4,
  "success_rate": 0.25,
  "last_activity_at": "2026-09-25T15:30:12.345+00:00"
}
```

### Результаты прохождений для журнала LMS

Возвращает прохождения, завершённые строго позже `since`, по возрастанию времени (до `limit`,
максимум 500). Для постраничной выгрузки передайте в `since` значение `finished_at` последней
полученной записи.

```bash
curl -s -G "$API/results" -H "X-API-Key: $KEY" \
  --data-urlencode "since=2026-09-01T00:00:00+03:00" --data-urlencode "limit=100"
```

```json
[
  {
    "run_id": "0b6f…",
    "personnel_number": "100001",
    "scenario_id": "business-seat-conflict",
    "scenario_title": "Конфликт из-за места в бизнес-классе",
    "category": "conflict",
    "outcome": "success",
    "xp_earned": 254,
    "loyalty": 95,
    "safety": 85,
    "finished_at": "2026-09-25T15:30:12.345+00:00"
  }
]
```

### Создание или обновление сотрудника из HR

Запрос идемпотентный: повторный вызов обновляет ФИО, роль и бригаду. Депо и бригада создаются,
если их ещё нет. HR пароль не передаёт, поэтому у таких сотрудников вход по паролю закрыт до
подключения SSO (см. [limitations.md](limitations.md)).

```bash
curl -s -X PUT "$API/employees/123456" -H "X-API-Key: $KEY" -H "Content-Type: application/json" \
  -d '{"full_name": "Синтетический Сотрудник", "role": "conductor", "brigade": "Бригада 7", "depot": "Депо Тверь"}'
```

```json
{"personnel_number": "123456", "created": true}
```

### Объём использования для биллинга

Сводка за календарный месяц по московскому времени: сколько сотрудников каждого депо
проходили сценарии, сколько прохождений завершено и сколько XP начислено.

```bash
curl -s -G "$API/billing/usage" -H "X-API-Key: $KEY" --data-urlencode "month=2026-09"
```

```json
{
  "month": "2026-09",
  "active_employees": 31,
  "runs_completed": 118,
  "depots": [
    {"depot": "Депо Москва", "active_employees": 14, "runs_completed": 55, "xp_awarded": 9870}
  ]
}
```

## Пользовательский API

Все запросы, кроме входа, передают `Authorization: Bearer <access_token>`.

| Метод и путь | Назначение |
|--------------|-----------|
| `POST /api/auth/login` | вход: `{personnel_number, password}` → `access_token` |
| `GET /api/scenarios` | каталог сценариев |
| `GET /api/scenarios/schema` | JSON-схема файла сценария |
| `POST /api/runs` | начать сценарий (или продолжить незавершённый) |
| `GET /api/runs/{id}` | состояние; заодно применяет истёкший таймер |
| `POST /api/runs/{id}/choices` | ответ: `{node_id, choice_id}` |
| `GET /api/runs/{id}/debrief` | разбор завершённого сценария |
| `GET /api/profile` | уровень, компетенции, ачивки, история |
| `GET /api/leaderboard?scope=brigade\|depot\|company&period=week\|month\|all` | рейтинг |
| `GET /api/notifications`, `POST /api/notifications/read-all` | уведомления |
| `GET /api/analytics/me` | аналитика и рекомендация |
| `GET /api/analytics/team`, `GET /api/analytics/employees/{id}` | для инструктора: проводники своего депо |
| `GET /api/meta` | названия компетенций для интерфейса |

Пример прохождения из командной строки:

```bash
TOKEN=$(curl -s localhost:8000/api/auth/login -H "Content-Type: application/json" \
  -d '{"personnel_number":"100001","password":"demo2026"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')
RUN=$(curl -s localhost:8000/api/runs -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"scenario_id":"business-seat-conflict"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')
curl -s localhost:8000/api/runs/$RUN/choices -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"node_id":"start","choice_id":"ask_tickets"}'
```

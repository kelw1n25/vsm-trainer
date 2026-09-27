# API

Полная интерактивная документация — Swagger: http://localhost:8000/api/docs.
OpenAPI-схема лежит в репозитории: [openapi.yaml](openapi.yaml) (выгружается `python scripts/export_openapi.py`,
тест `test_openapi.py` не даёт ей отстать от кода). Один API обслуживает веб-клиент, Android-приложение и внешние системы.

## Формат ошибок

Все ошибки возвращаются в одном формате: `code` — для программ, `message` — для человека.

```json
{"detail": {"code": "time_expired", "message": "Время на решение истекло, ответ не принят"}}
```

| HTTP | code | Когда |
|------|------|-------|
| 401 | `not_authenticated`, `invalid_token`, `invalid_credentials`, `invalid_api_key` | нет или неверный токен, пароль, ключ |
| 401 | `invalid_refresh_token` | refresh-токен истёк, отозван или предъявлен повторно |
| 403 | `forbidden` | раздел только для инструктора |
| 404 | `scenario_not_found`, `run_not_found`, `employee_not_found` | объекта нет (или он чужой) |
| 409 | `time_expired` | ответ пришёл после таймера — таймаут уже применён |
| 409 | `stale_node` | повторная отправка или ответ на устаревший шаг |
| 409 | `run_finished`, `run_in_progress` | действие не подходит к статусу прохождения |
| 422 | `validation_error`, `unknown_choice` | некорректные данные, несуществующий или скрытый вариант |
| 429 | `too_many_attempts` | 5 неудачных входов по номеру или 20 с одного IP за 15 минут; заголовок `Retry-After` |
| 500 | `internal_error` | непредвиденная ошибка: стек только в журнале сервера, в `message` — код запроса для поддержки |

Каждый ответ несёт заголовок `X-Request-ID`. Клиент может прислать свой (8–64 символа `A-Za-z0-9-`) —
Android-приложение так и делает, — и по нему запрос находится в JSON-журнале сервера.

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
подключения SSO (см. [LIMITATIONS.md](LIMITATIONS.md)).

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
| `POST /api/auth/login` | вход: `{personnel_number, password}` → `access_token`, `refresh_token`, `expires_in` |
| `POST /api/auth/refresh` | `{refresh_token}` → новая пара токенов; использованный токен отзывается (ротация) |
| `POST /api/auth/logout` | `{refresh_token}` → отзыв сессии этого устройства |
| `GET /api/scenarios` | каталог: класс, ситуации из справочника, число финалов |
| `GET /api/scenarios/schema` | JSON-схема файла сценария |
| `GET /api/scenarios/{id}/story-map` | архив веток сотрудника: исследованные развилки и открытые финалы (закрытые — без текста) |
| `GET /api/handbook` | справочник: 51 ситуация, ролевая модель, классы обслуживания, стандарты |
| `POST /api/runs` | начать сценарий (или продолжить незавершённый): сцена, диалог, персонажи |
| `GET /api/runs/{id}` | состояние; заодно применяет истёкший таймер |
| `POST /api/runs/{id}/reveal` | сцена дочитана: `{node_id}` → варианты и дедлайн таймера |
| `POST /api/runs/{id}/choices` | ответ: `{node_id, choice_id}` → реакция персонажей и следующая сцена |
| `GET /api/runs/{id}/debrief` | разбор: каждое решение, лучший вариант, стандарты по ситуациям |
| `GET /api/profile` | уровень, компетенции, ачивки, история, аватар |
| `PUT /api/profile/avatar` | сохранить аватар: `background` (blue, mint, sand, lilac, coral, night), `headwear` (cap, none), `tie` (red, blue, green, graphite); другое значение или лишнее поле — 422 |
| `GET /api/leaderboard?scope=brigade\|depot\|company&period=week\|month\|all` | рейтинг |
| `GET /api/notifications`, `POST /api/notifications/read-all` | уведомления |
| `GET /api/analytics/me` | аналитика: прогресс, компетенции, доля завершённых, среднее время решения, ошибки, рекомендация |
| `POST /api/analytics/events` | действие, видимое только клиенту: `{type, run_id?, notification_id?, platform}` → `202` |
| `GET /api/analytics/team`, `GET /api/analytics/employees/{id}` | для инструктора: проводники своего депо |
| `GET /api/meta` | названия компетенций для интерфейса |

Пример прохождения из командной строки:

```bash
TOKEN=$(curl -s localhost:8000/api/auth/login -H "Content-Type: application/json" \
  -d '{"personnel_number":"100001","password":"demo2026"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["access_token"])')
RUN=$(curl -s localhost:8000/api/runs -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"scenario_id":"business-seat-conflict"}' | python3 -c 'import sys,json;print(json.load(sys.stdin)["id"])')
curl -s localhost:8000/api/runs/$RUN/reveal -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"node_id":"start"}'
curl -s localhost:8000/api/runs/$RUN/choices -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"node_id":"start","choice_id":"check_both"}'
```

## Сессии Android-приложения

Access-токен живёт `JWT_TTL_MINUTES` (12 ч), refresh-токен — `REFRESH_TTL_DAYS` (30 дней). В базе хранится только
SHA-256 refresh-токена. Каждый обмен отзывает предъявленный токен и выдаёт новый. Если уже обменянный токен
предъявят ещё раз, значит, его скопировали: сервер отзывает все сессии сотрудника. Поэтому клиенты
обменивают токен строго одним запросом, даже когда на `401 invalid_token` наткнулись несколько запросов сразу
(Android — `TokenAuthenticator` с блокировкой).

```bash
curl -s localhost:8000/api/auth/refresh -H "Content-Type: application/json" -d '{"refresh_token":"<из ответа login>"}'
```

## События аналитики

Журнал `events` пишет сервер при каждом действии; три события присылает клиент, потому что сервер их не видит.
Бриф мобильной версии называет события иначе — соответствие ниже (имена в базе не менялись, на них опираются
разбор и аналитика).

| Событие брифа | Имя в журнале | Кто пишет |
|---------------|---------------|-----------|
| `scenario_started` | `run_started` | сервер, `POST /api/runs` |
| `choice_selected` / `decision_made` | `choices_shown` (варианты показаны) и `choice_made` (с `elapsed_seconds`, `best`, дельтами шкал) | сервер |
| `decision_timeout` | `timeout` | сервер, при первом обращении после дедлайна |
| `scenario_completed` | `run_finished` (итог, причина, XP, компетенции) | сервер |
| `achievement_unlocked` | `achievement_earned` | сервер |
| `level_up` | `level_up` | сервер |
| `scenario_abandoned` | `run_exited` | клиент: закрыл сценарий до финала |
| `feedback_opened` | `debrief_opened` | клиент: открыл разбор |
| `notification_opened` | `notification_opened` (+ тип уведомления) | клиент |

В события не пишутся ФИО, табельные номера и тексты ответов — только идентификаторы узлов и вариантов.

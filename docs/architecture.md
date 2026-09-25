# Архитектура

## Компоненты

```mermaid
flowchart LR
    subgraph Client["Браузер проводника / инструктора"]
        UI["React + TypeScript<br/>(Vite)"]
    end

    subgraph Backend["Backend — FastAPI, stateless"]
        AUTH["auth<br/>JWT, роли"]
        SCN["scenarios<br/>загрузка и валидация"]
        ENG["engine<br/>прохождение, таймер"]
        SCO["scoring<br/>шкалы, XP, сгорание"]
        ACH["achievements<br/>ачивки, уровни"]
        LB["leaderboard"]
        NOT["notifications"]
        AN["analytics"]
        PR["profiles"]
        INT["integration<br/>API-ключ"]
    end

    DB[("PostgreSQL")]
    FILES[/"scenarios/*.yaml<br/>config/game.yaml"/]
    EXT["HR / LMS"]

    UI -- "REST /api, JWT" --> AUTH
    UI --> ENG & PR & LB & NOT & AN
    ENG --> SCO --> ACH --> NOT
    FILES -- "при старте" --> SCN
    SCN --> DB
    ENG & SCO & ACH & LB & NOT & AN & PR & INT --> DB
    EXT -- "REST, X-API-Key" --> INT
```

**Frontend** — одностраничное приложение. Обращается к относительному адресу `/api`, который Vite
проксирует на backend, поэтому CORS не нужен.

**Backend** не хранит состояния в памяти: прохождение, таймер, шкалы и флаги лежат в строке
`scenario_runs`. Любую копию backend можно перезапустить или добавить за балансировщиком.
Где могут столкнуться несколько копий, это решено средствами PostgreSQL:

| Ситуация | Решение |
|----------|---------|
| Два ответа на один шаг одновременно | `SELECT … FOR UPDATE` строки прохождения |
| Два финиша одного сотрудника одновременно | блокировка строки сотрудника при начислении XP |
| Две копии загружают сценарии при старте | `INSERT … ON CONFLICT DO UPDATE` |
| Две копии создают демо-данные | `pg_advisory_xact_lock` |
| Две копии сжигают баллы одному сотруднику | `FOR UPDATE SKIP LOCKED` |
| Разные часы на серверах | время берётся из БД: `now()` |

**Storage** — PostgreSQL. Граф сценария хранится целиком в JSONB (файл → строка один к одному),
журнал действий — в таблице `events`.

## Модули backend

| Модуль | Ответственность |
|--------|-----------------|
| `profiles` | депо, бригады, сотрудники; `GET /api/profile` |
| `scenarios` | формат (Pydantic → JSON-схема), валидатор графа, загрузка файлов в БД, каталог |
| `engine` | старт, выбор, условия, эффекты, переходы, серверный таймер, финал, разбор |
| `scoring` | границы шкал 0–100, XP, бонус за скорость, челлендж недели, сгорание баллов |
| `achievements` | уровни по порогам XP, правила ачивок из конфига |
| `leaderboard` | рейтинг: бригада / депо / компания × неделя / месяц / всё время |
| `notifications` | уведомления внутри приложения, защита от дублей |
| `analytics` | прогресс, компетенции, типичные ошибки, рекомендация; журнал событий |
| `integration` | API для HR и LMS под API-ключом |
| `auth` | вход, JWT, роли «проводник» и «инструктор» |

Все правила игры — веса, пороги, ачивки, сроки — лежат в `backend/config/game.yaml`, а не в коде.

## Данные

```mermaid
erDiagram
    depots ||--o{ brigades : ""
    brigades ||--o{ employees : ""
    employees ||--o{ scenario_runs : ""
    scenarios ||--o{ scenario_runs : ""
    employees ||--o{ events : ""
    scenario_runs ||--o{ events : ""
    employees ||--o{ employee_achievements : ""
    employees ||--o{ notifications : ""

    scenarios { string id PK "из имени файла"
                jsonb definition "граф узлов"
                string content_hash }
    scenario_runs { uuid id PK
                    string current_node_id
                    int loyalty
                    int safety
                    jsonb flags
                    timestamptz node_entered_at "отсчёт таймера"
                    string status
                    int xp_earned }
    events { string type "choice_made, timeout, run_finished, ..."
             jsonb payload }
```

## Прохождение сценария

```mermaid
sequenceDiagram
    actor P as Проводник
    participant UI as Frontend
    participant API as Backend (engine)
    participant DB as PostgreSQL

    P->>UI: «Начать»
    UI->>API: POST /api/runs {scenario_id}
    API->>DB: INSERT scenario_runs (node_entered_at = now())
    API-->>UI: узел, варианты, deadline_at, timeout_at, server_time
    Note over UI: обратный отсчёт с поправкой<br/>на разницу часов клиента и сервера

    alt Ответ до дедлайна
        P->>UI: выбирает вариант
        UI->>API: POST /api/runs/{id}/choices {node_id, choice_id}
        API->>DB: SELECT … FOR UPDATE
        API->>API: проверки: время, актуальный узел, условие показа
        API->>API: эффекты → шкалы 0–100 → бонус за скорость → переход
        API->>DB: UPDATE run, INSERT events
        API-->>UI: новое состояние + последствия шага
    else Таймер истёк
        UI->>API: GET /api/runs/{id} в момент timeout_at
        API->>API: timeout_effects → переход в timeout_next
        API-->>UI: новое состояние + шаг «время истекло»
    end

    opt Финальный узел или шкала = 0
        API->>DB: XP и компетенции в профиль, ачивки, уровень, уведомления
        API-->>UI: итог, XP, новые ачивки, новый уровень
        UI->>API: GET /api/runs/{id}/debrief
    end
```

Клиентский таймер только показывает остаток. Решает сервер: ответ позже
`node_entered_at + timer_seconds` (плюс 1 секунда на задержку сети, настраивается)
отклоняется с кодом `time_expired`, а таймаут применяется при любом обращении к прохождению.
Если проводник пропустил несколько узлов подряд, таймауты применяются по цепочке
от точных моментов дедлайнов.

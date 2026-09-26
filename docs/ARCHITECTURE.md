# Архитектура

## Компоненты

```mermaid
flowchart LR
    subgraph Client["Браузер проводника / инструктора"]
        UI["React + TypeScript<br/>(Vite): дашборд"]
        STORY["StoryEngine (чистый TS)<br/>режим новеллы"]
        LS[("localStorage<br/>место в истории, журнал")]
        STORY --- LS
    end

    subgraph IOS["iOS — Swift, SwiftUI"]
        IUI["VSMFeatures<br/>экраны SwiftUI"]
        ICORE["VSMCore: ViewModel,<br/>репозитории, APIClient"]
        IKC[("Keychain · файловый кэш")]
        IUI --> ICORE --- IKC
    end

    subgraph AND["Android — Kotlin, Compose"]
        AUI["feature + design<br/>Compose + ViewModel"]
        ACORE["data: Retrofit/OkHttp,<br/>репозитории, WorkManager"]
        AKS[("Android Keystore · файловый кэш")]
        AUI --> ACORE --- AKS
    end

    subgraph Backend["Backend — FastAPI, stateless"]
        AUTH["auth<br/>JWT, роли"]
        SCN["scenarios<br/>загрузка, валидация,<br/>архив веток"]
        HB["handbook<br/>ситуации, стандарты"]
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
    FILES[/"scenarios/*.yaml<br/>config/game.yaml<br/>config/handbook.yaml"/]
    EXT["HR / LMS / биллинг"]

    ICORE -- "REST /api, JWT + refresh" --> AUTH
    ICORE --> ENG & PR & LB & NOT & AN
    ACORE -- "REST /api, JWT + refresh" --> AUTH
    ACORE --> ENG & PR & LB & NOT & AN
    UI -- "REST /api, JWT" --> AUTH
    UI --> PR & LB & NOT & AN & HB & SCN
    STORY -- "REST /api/runs" --> ENG
    ENG --> SCO --> ACH --> NOT
    FILES -- "при старте" --> SCN & HB
    SCN --> DB
    ENG & SCO & ACH & LB & NOT & AN & PR & INT --> DB
    EXT -- "REST, X-API-Key" --> INT
```

**Frontend** — одностраничное приложение. Обращается к относительному адресу `/api`, который Vite
проксирует на backend, поэтому CORS не нужен. Режим визуальной новеллы — отдельный движок
`frontend/src/story/StoryEngine.ts` на чистом TypeScript без React: очередь реплик, печать текста, смена сцен,
журнал, автопрокрутка, пропуск виденного, сохранение места в localStorage. React-компоненты только
подписываются на его состояние и рисуют сцену. Что происходит в истории, решает сервер.

**Мобильные приложения** — нативные: iOS на Swift/SwiftUI, Android на Kotlin/Jetpack Compose, без WebView.
Интерфейс перенесён с сайта один к одному: те же токены, шрифт Manrope, компоненты, ассеты (экспорт из
React-компонентов, `tools/mobile-assets`) и движок новеллы — порт `StoryEngine.ts`. Итог сверки —
[FINAL_UI_AUDIT.md](FINAL_UI_AUDIT.md). Устройство слоёв одинаковое:

| Слой | iOS (`ios/VSMKit`) | Android (`android/app`) |
|------|--------------------|-------------------------|
| Дизайн-система | `VSMFeatures/Design` (токены, Manrope, компоненты, графики) | `design` (токены, Manrope, компоненты, графики) |
| Экраны и навигация | `VSMFeatures/Screens`, `Gameplay`, `Navigation` (SwiftUI) | `feature/*`, `navigation` (Compose) |
| Движок новеллы | `VSMCore/Domain/StoryEngine.swift` | `domain/story/StoryEngine.kt` |
| Состояние экранов | `VSMCore/Presentation/*ViewModel.swift` (`@Observable`) | `feature/*/…ViewModel.kt` (`StateFlow`, Hilt) |
| Данные | `Data/Repositories`: сеть → кэш | `data/repository`: сеть → кэш |
| Сеть | `Networking/APIClient`, `TokenRefresher` (actor) | Retrofit + OkHttp, `TokenAuthenticator` |
| Секреты | `Security/KeychainTokenStore` | `security/KeystoreTokenStore` (AES-GCM) |
| Уведомления | `.backgroundTask(.appRefresh)` → `UNUserNotificationCenter` | `WorkManager` → `NotificationManager` |
| Внедрение зависимостей | `AppContainer` (через init) | Hilt (`di/AppModule.kt`) |

Бизнес-логики в клиентах нет: они показывают состояние сервера и отправляют действия. Отдельного «мобильного API»
тоже нет — это те же эндпоинты, что у веб-клиента, плюс обновление сессии refresh-токеном.

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
| `scenarios` | формат новеллы (Pydantic → JSON-схема), валидатор графа и сцен, загрузка файлов в БД, каталог, архив веток |
| `handbook` | справочник из материалов кейсодержателя: 51 ситуация, ролевая модель, классы обслуживания, стандарты |
| `engine` | старт, показ вариантов (reveal), выбор, условия по шкалам, скрытым параметрам и флагам, эффекты, реакции, серверный таймер, финал, разбор |
| `scoring` | границы шкал 0–100, XP, бонус за скорость, челлендж недели, сгорание баллов |
| `achievements` | уровни по порогам XP, правила ачивок из конфига |
| `leaderboard` | рейтинг: бригада / депо / компания × неделя / месяц / всё время |
| `notifications` | уведомления внутри приложения, защита от дублей |
| `analytics` | прогресс, компетенции, доля завершённых, время решения, типичные ошибки, рекомендация; журнал событий, клиентские события |
| `integration` | API для HR, LMS и биллинга под API-ключом |
| `auth` | вход, JWT, refresh-токены с ротацией, лимит неудачных входов, роли «проводник» и «инструктор» |

Все правила игры — веса, пороги, ачивки, сроки — лежат в `backend/config/game.yaml`, а не в коде.
Сквозные части: `app/observability.py` — JSON-журнал с `request_id` и ответ `500 internal_error` без деталей;
`app/errors.py` — единый формат ошибок.

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
    employees ||--o{ refresh_tokens : ""

    scenarios { string id PK "из имени файла"
                jsonb definition "граф узлов"
                string content_hash }
    scenario_runs { uuid id PK
                    string current_node_id
                    int loyalty
                    int safety
                    jsonb flags
                    jsonb stats "скрытые параметры"
                    timestamptz node_entered_at
                    timestamptz choices_shown_at "отсчёт таймера"
                    string status
                    int xp_earned }
    events { string type "choice_made, timeout, run_finished, ..."
             jsonb payload }
```

### Сущности брифа и где они хранятся

| Сущность | Где |
|----------|-----|
| User, EmployeeProfile | `employees` (ФИО, табельный номер, роль, XP, `competence_points`, последняя активность) |
| Team, Depot, Company | `brigades`, `depots`; компания — все депо |
| Scenario, ScenarioNode, ScenarioChoice, ScenarioCondition, ScenarioOutcome | граф в `scenarios.definition` (JSONB) из YAML: `nodes[].choices[]`, `condition`, `effects`, `next`, `outcome`; схема — `app/scenarios/schema.py` |
| ScenarioSession | `scenario_runs`: текущий узел, шкалы, флаги, скрытые параметры, метки таймера, итог |
| ScenarioAction, AnalyticsEvent | `events`: `choice_made`, `timeout`, `run_finished`, клиентские события |
| PassengerLoyalty, SafetyRating | `scenario_runs.loyalty`, `scenario_runs.safety` (0–100) |
| Competency, CompetencyScore | коды и названия — `config/game.yaml: competences`; очки — `employees.competence_points`, по прохождению — `scenario_runs.competence_points` |
| Achievement, UserAchievement | правила — `config/game.yaml: achievements`; выданные — `employee_achievements` (уникально по сотруднику и коду) |
| Leaderboard | не хранится: SQL по `scenario_runs.xp_earned` за период — всегда актуален |
| Notification | `notifications` (уникально по `dedup_key`) |
| UserProgress | уровень считается из XP по порогам `config/game.yaml: levels`; прогресс по неделям — из `scenario_runs` |
| Сессия устройства | `refresh_tokens` |

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
    API-->>UI: сцена, диалог, персонажи — без вариантов, таймер не идёт
    Note over UI: заставка, печать реплик,<br/>журнал, «Авто», «Пропустить»
    P->>UI: дочитал сцену
    UI->>API: POST /api/runs/{id}/reveal {node_id}
    API->>DB: choices_shown_at = now()
    API-->>UI: варианты, deadline_at, timeout_at, server_time
    Note over UI: обратный отсчёт с поправкой<br/>на разницу часов клиента и сервера

    alt Ответ до дедлайна
        P->>UI: выбирает вариант
        UI->>API: POST /api/runs/{id}/choices {node_id, choice_id}
        API->>DB: SELECT … FOR UPDATE
        API->>API: проверки: время, актуальный узел, условие показа
        API->>API: эффекты → шкалы 0–100 → бонус за скорость → переход
        API->>DB: UPDATE run, INSERT events
        API-->>UI: реакция персонажей + следующая сцена
    else Таймер истёк
        UI->>API: GET /api/runs/{id} в момент timeout_at
        API->>API: timeout_effects → переход в timeout_next
        API-->>UI: реакция на промедление + следующая сцена
    end

    opt Финальный узел или шкала = 0
        API->>DB: XP и компетенции в профиль, ачивки, уровень, уведомления
        API-->>UI: название финала, эпилог, XP, ачивки, уровень
        UI->>API: GET /api/scenarios/{id}/story-map, /api/runs/{id}/debrief
    end
```

Клиентский таймер только показывает остаток. Решает сервер: ответ позже
`choices_shown_at + timer_seconds` (плюс 1 секунда на задержку сети, настраивается)
отклоняется с кодом `time_expired`, а таймаут применяется при любом обращении к прохождению.
Таймер стартует, когда проводник дочитал сцену и увидел варианты: время на чтение диалога не штрафуется,
а варианты до этого момента сервер не отдаёт — подглядеть их, не запустив таймер, нельзя.
Ответ без показа вариантов отклоняется с кодом `choices_not_shown`.

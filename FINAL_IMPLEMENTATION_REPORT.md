# Итоговый отчёт: мобильная версия тренажёра проводника ВСМ

## 1. Что было в исходном проекте
Веб-приложение (React 19 + TypeScript) и backend (FastAPI + PostgreSQL) со всеми девятью функциональными
требованиями ТЗ: нелинейный движок сценариев с серверным таймером, шкалы лояльности и безопасности, очки
компетенций, уровни и ачивки, рейтинг бригада/депо/компания, разбор решений, уведомления, аналитика, API для
HR/LMS/биллинга; 14 сценариев по 51 ситуации из материалов кейсодержателя; 116 тестов backend.
Полный аудит — [docs/PROJECT_AUDIT.md](docs/PROJECT_AUDIT.md), сверка с ТЗ — [docs/REQUIREMENTS_MATRIX.md](docs/REQUIREMENTS_MATRIX.md).

## 2. Что изменено
| Область | Изменение |
|---------|-----------|
| Backend | refresh-токены с ротацией и отзывом (миграция `0005`), `POST /api/auth/refresh`, `/logout`; лимит неудачных входов (`429` + `Retry-After`); JSON-журнал с `request_id`; аудит входов без ПДН; `500 internal_error` без стека; метрики `completion_rate`, `avg_decision_seconds`, `unfinished_runs`; клиентские события `POST /api/analytics/events` |
| iOS | новое приложение: Swift, SwiftUI, async/await, Keychain, фоновая синхронизация уведомлений |
| Android | новое приложение: Kotlin, Jetpack Compose, Coroutines/Flow, Retrofit/OkHttp, Hilt, Android Keystore, WorkManager |
| Документация | `openapi.yaml` в репозитории с тестом актуальности; SCENARIO_ENGINE, SECURITY, DEPLOYMENT; обновлены README, ARCHITECTURE, API, USER_FLOW, LIMITATIONS |
| CI | `.github/workflows/ci.yml`: backend, frontend, iOS, Android |

## 3. Что переиспользовано
Вся бизнес-логика backend без изменения поведения: движок, скоринг, ачивки, рейтинг, уведомления, аналитика,
интеграции, контент. Мобильные клиенты работают с тем же API, что и веб: второго API нет. Формат ошибок
`{"detail": {"code", "message"}}` используется клиентами как есть. Веб-клиент не менялся.

## 4. Архитектура
```
iOS / Android / Веб  ──REST /api──►  FastAPI (stateless)  ──►  PostgreSQL
HR / LMS / биллинг   ──X-API-Key──►   движок · скоринг · ачивки · рейтинг · уведомления · аналитика
```
Клиенты показывают состояние сервера и отправляют действия; исход шага, таймер, XP и рейтинг считает сервер.
Диаграммы — [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## 5. iOS (`ios/`)
- `VSMKit/Sources/VSMCore` — DTO (Codable, даты с микросекундами), `APIClient` (единый разбор ошибок,
  `X-Request-ID`, повтор после обновления сессии), `TokenRefresher` (actor: один обмен на все параллельные 401),
  `KeychainTokenStore`, `FileResponseCache`, репозитории «сеть → кэш», `ServerClock`, `NotificationSync`,
  `StoryEngine` — порт движка новеллы сайта, `StoryMemory`, `AppPreferences`, ViewModel всех экранов (`@Observable`).
- `VSMKit/Sources/VSMFeatures` — интерфейс сайта один к одному: дизайн-система (`Design`), все 15 экранов сайта
  (`Screens`, `Gameplay`), шапка с меню аватара и нижняя навигация (`Navigation`), ассеты, Manrope, звук новеллы.
  Внедрение зависимостей — `AppContainer` через init. Сверка с сайтом — [docs/FINAL_UI_AUDIT.md](docs/FINAL_UI_AUDIT.md).
- `App` — `@main`, `.backgroundTask(.appRefresh)` для уведомлений; `project.yml` — проект XcodeGen.

## 6. Android (`android/`)
- `data/remote` — Retrofit-интерфейс, `AuthInterceptor`, `TokenAuthenticator` (синхронизированный обмен токена,
  сессию сбрасывает только отказ сервера, не сбой сети), `NetworkFactory`; `data/local` — файловый кэш,
  настройки; `security/KeystoreTokenStore` — AES-256-GCM с ключом в Android Keystore.
- `design` — дизайн-система сайта (токены, Manrope, компоненты, графики); `feature` — все 15 экранов сайта
  на Compose, ViewModel на `StateFlow` (Hilt); `navigation` — шапка с меню аватара и нижняя навигация;
  `domain/story/StoryEngine` — порт движка новеллы сайта.
- `work/NotificationWorker` — периодическая синхронизация уведомлений (WorkManager + Hilt).
- Сборка без SDK на машине — `android/ci/Dockerfile`.

## 7. Backend
FastAPI, SQLAlchemy 2, Alembic, PostgreSQL 17 — сохранён. Новое: `app/auth/tokens.py`, `app/auth/rate_limit.py`,
`app/observability.py`, клиентские события в `app/analytics/router.py`, `scripts/export_openapi.py`.

## 8. Database
Таблицы: `depots`, `brigades`, `employees`, `scenarios` (граф в JSONB), `scenario_runs` (всё состояние
прохождения), `events` (журнал для разбора и аналитики), `employee_achievements`, `notifications`,
`refresh_tokens` (новая: только SHA-256 токена, срок, причина отзыва). Сущности брифа сопоставлены с ними
в [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): узлы, варианты, условия и исходы — часть графа сценария,
сессия — `scenario_runs`, шкалы — её поля `loyalty` / `safety`.

## 9. Scenario Engine
`backend/app/engine/service.py`. Ответы на 20 контрольных вопросов (где ветвление, условия, таймер, шкалы,
XP, ачивки, защита от повторной награды, восстановление сессии) — таблица в
[docs/SCENARIO_ENGINE.md](docs/SCENARIO_ENGINE.md).

## 10. Gamification
Цикл «действие → очки → достижения → рейтинг → мотивация» замкнут во всех клиентах: финал показывает XP,
новые ачивки и уровень; главная — прогресс уровня, место в бригаде за неделю, последние достижения и
рекомендацию; уведомления о новых сценариях, челлендже недели и сгорающих баллах приходят системными баннерами.

## 11. Analytics
Сервер пишет события каждого шага; клиенты добавляют `debrief_opened`, `notification_opened`, `run_exited`.
Выводы: прогресс по неделям, сильные и проседающие компетенции, доля доведённых до финала, среднее время
решения, типичные ошибки по порогам, рекомендация сценария. ПДН в события не пишутся.
Соответствие имён событий брифу — [docs/API.md](docs/API.md#события-аналитики).

## 12. Security
Синтетические данные; секреты только в env; scrypt; JWT + ротация refresh-токенов с отзывом всех сессий при
повторе; лимит входа; Keychain `ThisDeviceOnly` / Keystore; запрет HTTP вне отладки; запрет резервных копий
сессии на Android; `500` без деталей; журналы без ПДН. Подробно — [docs/SECURITY.md](docs/SECURITY.md).

## 13. Testing
| Часть | Тесты | Где запускались |
|-------|-------|-----------------|
| Backend | 131 pytest: движок, таймер, ветвление, условия, шкалы, XP, ачивки, рейтинг, аналитика, клиентские события, refresh-ротация и повтор, лимит входа, `500` без деталей, `request_id`, актуальность OpenAPI | локально на PostgreSQL |
| iOS | 34 XCTest (включая сквозной прогон против живого backend): контракт по снятым с сервера ответам, ошибки API, офлайн, обновление сессии одним запросом, кэш, плеер (ветвление, двойное нажатие, ответ после таймера, обрыв связи, таймаут по часам сервера, восстановление), уведомления, сессия | Docker `swift:6.1` (Linux); `VSMFeatures` — `swift build` на macOS |
| Android | 27 JUnit (включая проверку тела запроса аналитики): контракт, MockWebServer (ошибки, офлайн, один обмен токена на параллельные 401, отказ и сбой обмена, кэш), `PlayerViewModel`, уведомления, Compose UI-тест входа на Robolectric | Docker (JDK 17, SDK 35) |

### Ручная проверка Android на эмуляторе (Pixel 7, Android 15, arm64) против живого backend
Вход → главная с данными сервера → разрешение на уведомления → карточка сценария → сцена без вариантов →
«К решению» → ветка «попросить освободить кресло» (лояльность −15) → шаг с таймером 20 с, ответ не дан →
ветка промедления от сервера (лояльность 45 → 30, «Время вышло») → финал «Оба пассажира довольны», +242 XP,
новый уровень → разбор со стандартами ситуаций → профиль (101 → 343 XP) → второй прохождение лучшим путём
(лояльность 95, безопасность 100) → выход из сценария с подтверждением. Найдено и исправлено: события
аналитики с Android отклонялись сервером (422 — `platform` не попадал в JSON), двойные кавычки у фраз стандарта,
низкий контраст реплики игрока, новый ключ подписи при каждой Docker-сборке.

## 14. API
[docs/API.md](docs/API.md), [docs/openapi.yaml](docs/openapi.yaml), Swagger `http://localhost:8000/api/docs`.

## 15. Deployment
`docker compose up --build`; переменные окружения, мобильные сборки и продакшен — [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md).

## 16. Known limitations
- iOS-таргет не собирался под симулятор на машине разработки (Xcode без принятой лицензии): ядро проверено
  тестами, в том числе против живого backend, экраны — сборкой пакета и отрисовкой тех же представлений SwiftUI
  на macOS с живыми данными; сборка под симулятор и UI-тест — задача CI на macOS.
  Android проверен вручную на эмуляторе (см. раздел 13).
- APNs и FCM не подключены (нужны ключи): уведомления доставляются фоновым опросом backend.
- Прохождение сценария требует сети — намеренно, чтобы таймер оставался честным.
- Лимит входа — в памяти процесса. Остальное — [docs/LIMITATIONS.md](docs/LIMITATIONS.md).

## 17. Future improvements
APNs/FCM, вход по биометрии, общий счётчик лимита (Redis),
публикация в магазинах, SSO; полный план — [docs/LIMITATIONS.md](docs/LIMITATIONS.md#план-развития-после-хакатона).

## 18. Как запустить проект
```bash
docker compose up --build                     # backend :8000, веб :5173
cd ios && xcodegen generate && open VSMTrainer.xcodeproj     # iOS → Run на симуляторе
cd android && ./gradlew installDebug          # Android → эмулятор
```
Демо: проводник `100001`, пароль `demo2026`. Подробно — [README.md](README.md).

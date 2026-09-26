# ВСМ-тренажёр проводника

Тренажёр для хакатона Московского транспорта, кейс «Геймификация для ВСМ»: backend на FastAPI + PostgreSQL,
веб-клиент на React и нативные мобильные приложения — **iOS (Swift, SwiftUI)** и **Android (Kotlin, Jetpack Compose)**.

Проводник высокоскоростной магистрали ВСМ-400 проживает нештатные ситуации как **интерактивную
визуальную новеллу**: сцены в салоне и на платформе, персонажи с эмоциями, диалоги и мысли, выборы с
последствиями, скрытые ветки и несколько финалов. Критические решения — **под серверным таймером**.
Каждое решение меняет две независимые шкалы — «Лояльность пассажира» и «Рейтинг безопасности» — и приносит
очки по пяти компетенциям. После финала — разбор каждого решения с лучшим вариантом и стандартом, архив
открытых веток, ачивки, уровни, рейтинг, уведомления и аналитика с рекомендацией.

14 сценариев построены на материалах кейсодержателя: все 51 ситуация из «Ситуаций на борту», ролевая модель
общения, стандарты СТО РЖД 03.011/03.013/03.014 и фото из датасета. Материалы — в разделе «Справочник».

## Требования

| Для чего | Что нужно |
|----------|-----------|
| Backend + веб | Docker и Docker Compose v2.24+ |
| iOS | macOS, Xcode 16+, [XcodeGen](https://github.com/yonaskolb/XcodeGen) (`brew install xcodegen`) |
| Android | Android Studio (JDK 17, SDK 35) — или только Docker (см. ниже) |

## Архитектура

```
iOS (SwiftUI) ─┐
Android (Compose) ─┼── REST /api (JWT + refresh) ──► Backend FastAPI ──► PostgreSQL
Веб (React) ───┘                                     движок сценариев, шкалы, XP, ачивки,
HR / LMS / биллинг ── REST /api/integration (X-API-Key) ─┘  рейтинг, уведомления, аналитика
```

Вся игровая логика — на сервере; клиенты показывают состояние и отправляют действия. Подробно —
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), движок — [docs/SCENARIO_ENGINE.md](docs/SCENARIO_ENGINE.md).

## Запуск backend и веб-клиента

Нужны Docker и Docker Compose v2.24+.

```bash
docker compose up --build
```

| Что | Адрес |
|-----|-------|
| Приложение | http://localhost:5173 |
| Swagger (документация API) | http://localhost:8000/api/docs |
| Проверка backend и БД | http://localhost:8000/api/health |

При первом запуске backend сам применяет миграции, загружает сценарии из `backend/scenarios/`
и создаёт синтетических сотрудников с историей прохождений.

Значения по умолчанию берутся из `.env.example` (только для демо). Для любого другого окружения:
`cp .env.example .env` и замените секреты в `.env` — он в `.gitignore`. Все переменные окружения
и настройка продакшена — в [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md).

База данных — PostgreSQL 17 в контейнере `db`; схема создаётся миграциями Alembic (`backend/alembic/versions`)
при старте backend. Сбросить демо-данные: `docker compose down -v && docker compose up --build`.

## Запуск iOS

Backend должен быть запущен (симулятор обращается к `http://localhost:8000`).

```bash
cd ios
xcodegen generate            # проект VSMTrainer.xcodeproj из project.yml
open VSMTrainer.xcodeproj    # схема VSMTrainer → симулятор iPhone → Run
```

Структура: `ios/VSMKit` — Swift-пакет (`VSMCore`: сеть, Keychain, кэш, ViewModel; `VSMFeatures`: экраны SwiftUI),
`ios/App` — точка входа и фоновая синхронизация уведомлений.

## Запуск Android

Backend должен быть запущен (эмулятор обращается к компьютеру по `http://10.0.2.2:8000`).

- **Android Studio:** открыть каталог `android/`, конфигурация `app` → эмулятор → Run.
- **Командная строка:** `cd android && ./gradlew installDebug` (нужен подключённый эмулятор или телефон).
- **Без Android SDK на машине** — сборка в Docker:

```bash
docker build --platform linux/amd64 -t vsm-android-build android/ci
docker run --rm --platform linux/amd64 -v "$PWD/android:/project" -v vsm-gradle:/root/.gradle vsm-android-build \
  ./gradlew -Pkotlin.compiler.execution.strategy=in-process testDebugUnitTest assembleDebug
# APK: android/app/build/outputs/apk/debug/app-debug.apk
```

## Тестовые учётные записи

Все данные синтетические, реальных персональных данных нет.

| Роль | Табельный номер | Пароль | Что посмотреть |
|------|-----------------|--------|----------------|
| Проводник | `100001` | `demo2026` | История с типичными ошибками, предупреждение о сгорании баллов, ачивки ещё не получены |
| Инструктор | `900001` | `demo2026` | Раздел «Команда»: проводники депо и их аналитика |

Остальные проводники: `100002`–`100042`, пароль тот же.

**Демо за 3 минуты:** войти как `100001` → колокольчик (предупреждение о сгорании, челлендж недели) →
«Аналитика» (типичные ошибки в медицинских сценариях, рекомендация) → сценарий «Два пассажира на одно место»:
заставка, сцена, диалог, выбор под таймером → финал «Оба пассажира довольны», ачивки «Дипломат» и
«Без права на ошибку» → «Посмотреть путь» (архив веток) → «Разбор решений» (стандарт по ситуациям) →
«Справочник» → «Рейтинг».

## Документация

| Документ | О чём |
|----------|-------|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Компоненты, модули backend, прохождение сценария (Mermaid) |
| [docs/API.md](docs/API.md) | API и примеры запросов для интеграции с HR, LMS и биллингом |
| [docs/USER_FLOW.md](docs/USER_FLOW.md) | Путь проводника от входа до рейтинга |
| [docs/SCENARIOS.md](docs/SCENARIOS.md) | Формат новеллы, «как добавить развилку за 5 минут», все 14 сценариев и ситуации |
| [docs/SCENARIO_ENGINE.md](docs/SCENARIO_ENGINE.md) | Движок: где ветвление, условия, таймер, шкалы, XP, ачивки — с путями к коду |
| [docs/SECURITY.md](docs/SECURITY.md) | 152-ФЗ, секреты, сессии, лимит входа, транспорт, обработка ошибок |
| [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md) | Переменные окружения, мобильные сборки, продакшен, CI |
| [docs/openapi.yaml](docs/openapi.yaml) | OpenAPI 3 — сверяется с кодом тестом |
| [docs/PROJECT_AUDIT.md](docs/PROJECT_AUDIT.md), [docs/REQUIREMENTS_MATRIX.md](docs/REQUIREMENTS_MATRIX.md) | Аудит перед мобильной версией и сверка с ТЗ |
| [docs/LIMITATIONS.md](docs/LIMITATIONS.md) | Ограничения решения и план развития |
| [FINAL_IMPLEMENTATION_REPORT.md](FINAL_IMPLEMENTATION_REPORT.md) | Итоговый отчёт о мобильной версии |

## Структура

```
backend/
  app/             FastAPI: модули profiles, scenarios, engine, scoring, achievements,
                   leaderboard, notifications, analytics, integration, auth
  alembic/         миграции БД
  config/game.yaml правила игры: компетенции, XP, уровни, ачивки, сгорание, челлендж
  config/handbook.yaml  справочник: 51 ситуация на борту, ролевая модель, классы обслуживания
  scenarios/       сценарии-новеллы в YAML — правятся без изменения кода
  tests/           тесты (pytest)
frontend/          React + TypeScript + Vite
  src/story/       режим новеллы: StoryEngine (чистый TypeScript), сцена, диалог, финал
  public/media/    фото и слайды из датасета кейсодержателя
ios/
  VSMKit/          Swift-пакет: VSMCore (DTO, APIClient, Keychain, кэш, ViewModel) и VSMFeatures (SwiftUI)
  App/             точка входа, Info.plist; project.yml — проект XcodeGen
  UITests/         UI-тест входа
android/
  app/src/main/    Kotlin: data (Retrofit, кэш), security (Keystore), presentation (Compose), work (WorkManager)
  app/src/test/    JUnit, MockWebServer, Compose UI-тесты на Robolectric
  ci/Dockerfile    окружение сборки без Android SDK на машине
docs/              документация
.github/workflows/ CI: backend, frontend, iOS, Android
```

## Сценарии

```bash
# проверить все сценарии без перезапуска
docker compose exec backend python -m app.scenarios.validator
# применить изменения
docker compose restart backend
```

JSON-схема формата: http://localhost:8000/api/scenarios/schema. Подробнее — [docs/SCENARIOS.md](docs/SCENARIOS.md).

## Тесты

**Backend.** Тесты валидатора работают без БД. Интеграционные тесты используют настоящий PostgreSQL
и отдельную базу с именем `*_test` (её таблицы очищаются).

```bash
cd backend
pip install -r requirements-dev.txt
POSTGRES_USER=... POSTGRES_PASSWORD=... POSTGRES_DB=vsm_test POSTGRES_HOST=localhost pytest
```

**iOS.** `cd ios/VSMKit && swift test` (нужен Xcode) — или без Xcode, в Linux-контейнере:
`docker run --rm -v "$PWD/ios/VSMKit:/pkg" -w /pkg swift:6.1-jammy swift test`.
UI-тест — схема `VSMTrainer` в Xcode (⌘U).

**Android.** `cd android && ./gradlew testDebugUnitTest` — JVM-тесты, MockWebServer и Compose UI-тесты
на Robolectric, эмулятор не нужен. Или в Docker — команда из раздела «Запуск Android».

**CI** — `.github/workflows/ci.yml` запускает всё это на каждый push.

## Решение проблем

| Симптом | Что делать |
|---------|-----------|
| Мобильное приложение: «Нет связи с сервером» | backend запущен? `curl localhost:8000/api/health`. На реальном телефоне замените `localhost` / `10.0.2.2` на IP компьютера в `project.yml` / `build.gradle.kts` |
| `429 too_many_attempts` при входе | 5 неудачных попыток за 15 минут — подождать `Retry-After` секунд или перезапустить backend |
| Android в Docker: «Gradle build daemon disappeared» | не хватает памяти Docker: добавьте `-Pkotlin.compiler.execution.strategy=in-process` или увеличьте память в Docker Desktop |
| `test_openapi_file_matches_code` упал | API изменился: `cd backend && python scripts/export_openapi.py` |
| iOS: `APIBaseURL` не задан | проект сгенерирован не из `project.yml` — выполните `xcodegen generate` |

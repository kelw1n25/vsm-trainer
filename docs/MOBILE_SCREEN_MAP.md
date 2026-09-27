# Карта экранов: сайт → Android-приложение

Каждый экран сайта переносится в нативный экран с той же структурой и визуальной системой.
Android — `android/app/src/main/java/ru/vsm/trainer/`.

```text
Website Screen
      ↓
Mobile Screen
      ↓
Android implementation
```

| Сайт | Маршрут | Мобильный экран | Android (`ru/vsm/trainer/`) |
|------|---------|-----------------|---------|
| Вход | /login | Вход | feature/auth/LoginScreen.kt |
| Шапка и навигация | Header + nav | Шапка + нижняя панель + меню аватара | navigation/AppNavigation.kt, AppHeader.kt, BottomNav.kt |
| Главная | / | Главная | feature/home/HomeScreen.kt |
| Сценарии | /scenarios | Сценарии | feature/scenarios/ScenarioScreens.kt |
| Сценарий | /scenarios/:id | Сценарий | feature/scenarios/ScenarioScreens.kt |
| Новелла | /runs/:id | Новелла на весь экран | feature/gameplay/StoryPlayerScreen.kt, StoryStage.kt, StoryAudio.kt |
| Развитие истории | /scenarios/:id/map | Развитие истории | feature/scenarios/StoryMapScreen.kt |
| Разбор | /runs/:id/debrief | Разбор | feature/debrief/DebriefScreen.kt |
| Профиль | /profile | Профиль | feature/profile/ProfileScreen.kt |
| Рейтинг | /rating | Рейтинг | feature/leaderboard/RatingScreen.kt |
| Аналитика | /analytics, /team/:id | Аналитика | feature/analytics/AnalyticsScreen.kt |
| Уведомления | /notifications | Уведомления | feature/notifications/NotificationsScreen.kt |
| Справочник | /handbook | Справочник | feature/handbook/HandbookScreen.kt |
| Настройки | /settings | Настройки | feature/settings/SettingsScreen.kt |
| Команда | /team | Команда (инструктор) | feature/team/TeamScreen.kt |

Движок новеллы — `domain/story/StoryEngine.kt`, дизайн-система — `design/`.

## Навигация

```text
Вход ──► Главная ─┬─ Сценарии ──► Сценарий ─┬─► Новелла ──► Финал ─┬─► Разбор
                  │                         │                      ├─► Развитие истории
                  │                         └─► Развитие истории    └─► Пройти заново
                  ├─ Рейтинг
                  ├─ Аналитика ──► Сценарий (рекомендация), Разбор (история)
                  ├─ Справочник ◄── чипы ситуаций со страницы сценария
                  └─ Аватар ─► Профиль ─► Разбор · Уведомления · Настройки · Команда ─► Аналитика проводника · Выйти
```

Нижняя панель: Главная, Сценарии, Рейтинг, Аналитика, Справочник. Шапка на всех вкладках: логотип «ВСМ», ползунок темы,
аватар с бейджем непрочитанных и меню (как на сайте). Новелла — полноэкранный модальный экран без шапки и панели.

## Чек-листы экранов

Все пункты проверены на обеих платформах; отличия от сайта и их причины — в [FINAL_UI_AUDIT.md](FINAL_UI_AUDIT.md).

### SCREEN: Вход

- [x] Layout
- [x] Header
- [x] Navigation
- [x] Typography
- [x] Colors
- [x] Cards
- [x] Buttons
- [x] Icons
- [x] Images
- [x] Spacing
- [x] Animations
- [x] States
- [x] Interactions
- [x] Mobile adaptation
- [x] Android

### SCREEN: Шапка и навигация

- [x] Layout
- [x] Header
- [x] Navigation
- [x] Typography
- [x] Colors
- [x] Cards
- [x] Buttons
- [x] Icons
- [x] Images
- [x] Spacing
- [x] Animations
- [x] States
- [x] Interactions
- [x] Mobile adaptation
- [x] Android

### SCREEN: Главная

- [x] Layout
- [x] Header
- [x] Navigation
- [x] Typography
- [x] Colors
- [x] Cards
- [x] Buttons
- [x] Icons
- [x] Images
- [x] Spacing
- [x] Animations
- [x] States
- [x] Interactions
- [x] Mobile adaptation
- [x] Android

### SCREEN: Сценарии

- [x] Layout
- [x] Header
- [x] Navigation
- [x] Typography
- [x] Colors
- [x] Cards
- [x] Buttons
- [x] Icons
- [x] Images
- [x] Spacing
- [x] Animations
- [x] States
- [x] Interactions
- [x] Mobile adaptation
- [x] Android

### SCREEN: Сценарий

- [x] Layout
- [x] Header
- [x] Navigation
- [x] Typography
- [x] Colors
- [x] Cards
- [x] Buttons
- [x] Icons
- [x] Images
- [x] Spacing
- [x] Animations
- [x] States
- [x] Interactions
- [x] Mobile adaptation
- [x] Android

### SCREEN: Новелла

- [x] Layout
- [x] Header
- [x] Navigation
- [x] Typography
- [x] Colors
- [x] Cards
- [x] Buttons
- [x] Icons
- [x] Images
- [x] Spacing
- [x] Animations
- [x] States
- [x] Interactions
- [x] Mobile adaptation
- [x] Android

### SCREEN: Развитие истории

- [x] Layout
- [x] Header
- [x] Navigation
- [x] Typography
- [x] Colors
- [x] Cards
- [x] Buttons
- [x] Icons
- [x] Images
- [x] Spacing
- [x] Animations
- [x] States
- [x] Interactions
- [x] Mobile adaptation
- [x] Android

### SCREEN: Разбор

- [x] Layout
- [x] Header
- [x] Navigation
- [x] Typography
- [x] Colors
- [x] Cards
- [x] Buttons
- [x] Icons
- [x] Images
- [x] Spacing
- [x] Animations
- [x] States
- [x] Interactions
- [x] Mobile adaptation
- [x] Android

### SCREEN: Профиль

- [x] Layout
- [x] Header
- [x] Navigation
- [x] Typography
- [x] Colors
- [x] Cards
- [x] Buttons
- [x] Icons
- [x] Images
- [x] Spacing
- [x] Animations
- [x] States
- [x] Interactions
- [x] Mobile adaptation
- [x] Android

### SCREEN: Рейтинг

- [x] Layout
- [x] Header
- [x] Navigation
- [x] Typography
- [x] Colors
- [x] Cards
- [x] Buttons
- [x] Icons
- [x] Images
- [x] Spacing
- [x] Animations
- [x] States
- [x] Interactions
- [x] Mobile adaptation
- [x] Android

### SCREEN: Аналитика

- [x] Layout
- [x] Header
- [x] Navigation
- [x] Typography
- [x] Colors
- [x] Cards
- [x] Buttons
- [x] Icons
- [x] Images
- [x] Spacing
- [x] Animations
- [x] States
- [x] Interactions
- [x] Mobile adaptation
- [x] Android

### SCREEN: Уведомления

- [x] Layout
- [x] Header
- [x] Navigation
- [x] Typography
- [x] Colors
- [x] Cards
- [x] Buttons
- [x] Icons
- [x] Images
- [x] Spacing
- [x] Animations
- [x] States
- [x] Interactions
- [x] Mobile adaptation
- [x] Android

### SCREEN: Справочник

- [x] Layout
- [x] Header
- [x] Navigation
- [x] Typography
- [x] Colors
- [x] Cards
- [x] Buttons
- [x] Icons
- [x] Images
- [x] Spacing
- [x] Animations
- [x] States
- [x] Interactions
- [x] Mobile adaptation
- [x] Android

### SCREEN: Настройки

- [x] Layout
- [x] Header
- [x] Navigation
- [x] Typography
- [x] Colors
- [x] Cards
- [x] Buttons
- [x] Icons
- [x] Images
- [x] Spacing
- [x] Animations
- [x] States
- [x] Interactions
- [x] Mobile adaptation
- [x] Android

### SCREEN: Команда

- [x] Layout
- [x] Header
- [x] Navigation
- [x] Typography
- [x] Colors
- [x] Cards
- [x] Buttons
- [x] Icons
- [x] Images
- [x] Spacing
- [x] Animations
- [x] States
- [x] Interactions
- [x] Mobile adaptation
- [x] Android

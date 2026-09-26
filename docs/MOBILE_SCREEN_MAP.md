# Карта экранов: сайт → мобильные приложения

Каждый экран сайта переносится в нативный экран с той же структурой и визуальной системой.
iOS — `ios/VSMKit/Sources/VSMFeatures/`, Android — `android/app/src/main/java/ru/vsm/trainer/`.

```text
Website Screen
      ↓
Mobile Screen
      ↓
iOS implementation
      ↓
Android implementation
```

| Сайт | Маршрут | Мобильный экран | iOS (`VSMFeatures/`) | Android (`ru/vsm/trainer/`) |
|------|---------|-----------------|-----|---------|
| Вход | /login | Вход | LoginView.swift | feature/auth/LoginScreen.kt |
| Шапка и навигация | Header + nav | Шапка + нижняя панель + меню аватара | Navigation/Shell.swift (AppShell, AppHeader, BottomNav) | navigation/AppNavigation.kt, AppHeader.kt, BottomNav.kt |
| Главная | / | Главная | Screens/Home.swift (HeroCarousel) | feature/home/HomeScreen.kt |
| Сценарии | /scenarios | Сценарии | Screens/Scenarios.swift (ScenarioListScreen) | feature/scenarios/ScenarioScreens.kt |
| Сценарий | /scenarios/:id | Сценарий | Screens/Scenarios.swift (ScenarioDetailScreen) | feature/scenarios/ScenarioScreens.kt |
| Новелла | /runs/:id | Новелла на весь экран | Gameplay/StoryPlayerScreen.swift, StoryStage.swift, StoryAudio.swift | feature/gameplay/StoryPlayerScreen.kt, StoryStage.kt, StoryAudio.kt |
| Развитие истории | /scenarios/:id/map | Развитие истории | Screens/Scenarios.swift (StoryMapScreen) | feature/scenarios/StoryMapScreen.kt |
| Разбор | /runs/:id/debrief | Разбор | Screens/Debrief.swift | feature/debrief/DebriefScreen.kt |
| Профиль | /profile | Профиль | Screens/Progress.swift (ProfileScreen) | feature/profile/ProfileScreen.kt |
| Рейтинг | /rating | Рейтинг | Screens/Progress.swift (RatingScreen) | feature/leaderboard/RatingScreen.kt |
| Аналитика | /analytics, /team/:id | Аналитика | Screens/Progress.swift (AnalyticsScreen) | feature/analytics/AnalyticsScreen.kt |
| Уведомления | /notifications | Уведомления | Screens/Reference.swift (NotificationsScreen) | feature/notifications/NotificationsScreen.kt |
| Справочник | /handbook | Справочник | Screens/Reference.swift (HandbookScreen) | feature/handbook/HandbookScreen.kt |
| Настройки | /settings | Настройки | Screens/Reference.swift (SettingsScreen) | feature/settings/SettingsScreen.kt |
| Команда | /team | Команда (инструктор) | Screens/Reference.swift (TeamScreen) | feature/team/TeamScreen.kt |

Движок новеллы — `VSMCore/Domain/StoryEngine.swift` и `domain/story/StoryEngine.kt`, дизайн-система —
`VSMFeatures/Design/` и `design/`.

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
- [x] iOS
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
- [x] iOS
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
- [x] iOS
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
- [x] iOS
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
- [x] iOS
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
- [x] iOS
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
- [x] iOS
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
- [x] iOS
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
- [x] iOS
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
- [x] iOS
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
- [x] iOS
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
- [x] iOS
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
- [x] iOS
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
- [x] iOS
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
- [x] iOS
- [x] Android

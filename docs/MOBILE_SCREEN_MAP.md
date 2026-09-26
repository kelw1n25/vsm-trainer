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

| Сайт | Маршрут | Мобильный экран | iOS | Android |
|------|---------|-----------------|-----|---------|
| Вход | /login | Вход | Features/Auth/LoginView.swift | feature/auth/LoginScreen.kt |
| Шапка и навигация | Header + nav | Шапка + нижняя панель + меню аватара | Navigation/RootView.swift, Navigation/AppHeader.swift | navigation/AppNavigation.kt, navigation/AppHeader.kt |
| Главная | / | Главная | Features/Home/HomeView.swift, HeroCarouselView.swift | feature/home/HomeScreen.kt, HeroCarousel.kt |
| Сценарии | /scenarios | Сценарии | Features/Scenarios/ScenarioListView.swift | feature/scenarios/ScenarioListScreen.kt |
| Сценарий | /scenarios/:id | Сценарий | Features/Scenarios/ScenarioDetailView.swift | feature/scenarios/ScenarioDetailScreen.kt |
| Новелла | /runs/:id | Новелла на весь экран | Features/Gameplay/StoryPlayerView.swift (+ StoryStage, StoryBar, DialogueBox, ChoicesView, HistorySheet, StoryEndView) | feature/gameplay/StoryPlayerScreen.kt (+ StoryStage, StoryBar, DialogueBox, Choices, HistorySheet, StoryEnd) |
| Развитие истории | /scenarios/:id/map | Развитие истории | Features/Scenarios/StoryMapView.swift | feature/scenarios/StoryMapScreen.kt |
| Разбор | /runs/:id/debrief | Разбор | Features/Debrief/DebriefView.swift | feature/debrief/DebriefScreen.kt |
| Профиль | /profile | Профиль | Features/Profile/ProfileView.swift | feature/profile/ProfileScreen.kt |
| Рейтинг | /rating | Рейтинг | Features/Leaderboard/RatingView.swift | feature/leaderboard/RatingScreen.kt |
| Аналитика | /analytics, /team/:id | Аналитика | Features/Analytics/AnalyticsView.swift | feature/analytics/AnalyticsScreen.kt |
| Уведомления | /notifications | Уведомления | Features/Notifications/NotificationsView.swift | feature/notifications/NotificationsScreen.kt |
| Справочник | /handbook | Справочник | Features/Handbook/HandbookView.swift | feature/handbook/HandbookScreen.kt |
| Настройки | /settings | Настройки | Features/Settings/SettingsView.swift | feature/settings/SettingsScreen.kt |
| Команда | /team | Команда (инструктор) | Features/Team/TeamView.swift | feature/team/TeamScreen.kt |

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

Отмечается по мере переноса; итог — в [FINAL_UI_AUDIT.md](FINAL_UI_AUDIT.md).

### SCREEN: Вход

- [ ] Layout
- [ ] Header
- [ ] Navigation
- [ ] Typography
- [ ] Colors
- [ ] Cards
- [ ] Buttons
- [ ] Icons
- [ ] Images
- [ ] Spacing
- [ ] Animations
- [ ] States
- [ ] Interactions
- [ ] Mobile adaptation
- [ ] iOS
- [ ] Android

### SCREEN: Шапка и навигация

- [ ] Layout
- [ ] Header
- [ ] Navigation
- [ ] Typography
- [ ] Colors
- [ ] Cards
- [ ] Buttons
- [ ] Icons
- [ ] Images
- [ ] Spacing
- [ ] Animations
- [ ] States
- [ ] Interactions
- [ ] Mobile adaptation
- [ ] iOS
- [ ] Android

### SCREEN: Главная

- [ ] Layout
- [ ] Header
- [ ] Navigation
- [ ] Typography
- [ ] Colors
- [ ] Cards
- [ ] Buttons
- [ ] Icons
- [ ] Images
- [ ] Spacing
- [ ] Animations
- [ ] States
- [ ] Interactions
- [ ] Mobile adaptation
- [ ] iOS
- [ ] Android

### SCREEN: Сценарии

- [ ] Layout
- [ ] Header
- [ ] Navigation
- [ ] Typography
- [ ] Colors
- [ ] Cards
- [ ] Buttons
- [ ] Icons
- [ ] Images
- [ ] Spacing
- [ ] Animations
- [ ] States
- [ ] Interactions
- [ ] Mobile adaptation
- [ ] iOS
- [ ] Android

### SCREEN: Сценарий

- [ ] Layout
- [ ] Header
- [ ] Navigation
- [ ] Typography
- [ ] Colors
- [ ] Cards
- [ ] Buttons
- [ ] Icons
- [ ] Images
- [ ] Spacing
- [ ] Animations
- [ ] States
- [ ] Interactions
- [ ] Mobile adaptation
- [ ] iOS
- [ ] Android

### SCREEN: Новелла

- [ ] Layout
- [ ] Header
- [ ] Navigation
- [ ] Typography
- [ ] Colors
- [ ] Cards
- [ ] Buttons
- [ ] Icons
- [ ] Images
- [ ] Spacing
- [ ] Animations
- [ ] States
- [ ] Interactions
- [ ] Mobile adaptation
- [ ] iOS
- [ ] Android

### SCREEN: Развитие истории

- [ ] Layout
- [ ] Header
- [ ] Navigation
- [ ] Typography
- [ ] Colors
- [ ] Cards
- [ ] Buttons
- [ ] Icons
- [ ] Images
- [ ] Spacing
- [ ] Animations
- [ ] States
- [ ] Interactions
- [ ] Mobile adaptation
- [ ] iOS
- [ ] Android

### SCREEN: Разбор

- [ ] Layout
- [ ] Header
- [ ] Navigation
- [ ] Typography
- [ ] Colors
- [ ] Cards
- [ ] Buttons
- [ ] Icons
- [ ] Images
- [ ] Spacing
- [ ] Animations
- [ ] States
- [ ] Interactions
- [ ] Mobile adaptation
- [ ] iOS
- [ ] Android

### SCREEN: Профиль

- [ ] Layout
- [ ] Header
- [ ] Navigation
- [ ] Typography
- [ ] Colors
- [ ] Cards
- [ ] Buttons
- [ ] Icons
- [ ] Images
- [ ] Spacing
- [ ] Animations
- [ ] States
- [ ] Interactions
- [ ] Mobile adaptation
- [ ] iOS
- [ ] Android

### SCREEN: Рейтинг

- [ ] Layout
- [ ] Header
- [ ] Navigation
- [ ] Typography
- [ ] Colors
- [ ] Cards
- [ ] Buttons
- [ ] Icons
- [ ] Images
- [ ] Spacing
- [ ] Animations
- [ ] States
- [ ] Interactions
- [ ] Mobile adaptation
- [ ] iOS
- [ ] Android

### SCREEN: Аналитика

- [ ] Layout
- [ ] Header
- [ ] Navigation
- [ ] Typography
- [ ] Colors
- [ ] Cards
- [ ] Buttons
- [ ] Icons
- [ ] Images
- [ ] Spacing
- [ ] Animations
- [ ] States
- [ ] Interactions
- [ ] Mobile adaptation
- [ ] iOS
- [ ] Android

### SCREEN: Уведомления

- [ ] Layout
- [ ] Header
- [ ] Navigation
- [ ] Typography
- [ ] Colors
- [ ] Cards
- [ ] Buttons
- [ ] Icons
- [ ] Images
- [ ] Spacing
- [ ] Animations
- [ ] States
- [ ] Interactions
- [ ] Mobile adaptation
- [ ] iOS
- [ ] Android

### SCREEN: Справочник

- [ ] Layout
- [ ] Header
- [ ] Navigation
- [ ] Typography
- [ ] Colors
- [ ] Cards
- [ ] Buttons
- [ ] Icons
- [ ] Images
- [ ] Spacing
- [ ] Animations
- [ ] States
- [ ] Interactions
- [ ] Mobile adaptation
- [ ] iOS
- [ ] Android

### SCREEN: Настройки

- [ ] Layout
- [ ] Header
- [ ] Navigation
- [ ] Typography
- [ ] Colors
- [ ] Cards
- [ ] Buttons
- [ ] Icons
- [ ] Images
- [ ] Spacing
- [ ] Animations
- [ ] States
- [ ] Interactions
- [ ] Mobile adaptation
- [ ] iOS
- [ ] Android

### SCREEN: Команда

- [ ] Layout
- [ ] Header
- [ ] Navigation
- [ ] Typography
- [ ] Colors
- [ ] Cards
- [ ] Buttons
- [ ] Icons
- [ ] Images
- [ ] Spacing
- [ ] Animations
- [ ] States
- [ ] Interactions
- [ ] Mobile adaptation
- [ ] iOS
- [ ] Android

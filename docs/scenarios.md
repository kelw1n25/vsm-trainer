# Сценарии

Сценарий — это YAML-файл в `backend/scenarios/`. Имя файла совпадает с `id`.
При старте backend проверяет каждый файл и загружает корректные в БД. Файл с ошибками
пропускается с понятным списком ошибок в логе, а в БД остаётся его прошлая корректная версия,
поэтому опечатка не ломает демонстрацию.

JSON-схема формата: `GET /api/scenarios/schema` (генерируется из тех же классов, что и проверка).

## Формат

```yaml
id: business-seat-conflict        # латиница, цифры, «-» и «_»; совпадает с именем файла
title: Конфликт из-за места в бизнес-классе
category: conflict                # conflict | medical | service | safety
difficulty: 2                     # 1–3, множитель XP
service_class: Бизнес
route: Москва — Санкт-Петербург
demo: true                        # контент написан командой, а не взят из материалов кейса
initial: {loyalty: 60, safety: 80}  # стартовые значения шкал 0–100
start_node: start

nodes:
  - id: start
    situation: Текст ситуации
    line:                           # необязательно: реплика персонажа
      speaker: passenger            # passenger | colleague | train_chief
      name: Пассажир с билетом на 5А
      text: Это моё место!
    timer_seconds: 20               # необязательно; только вместе с timeout_next
    timeout_next: escalation        # куда ведёт истечение таймера
    timeout_effects: {loyalty: -15} # обязан снижать хотя бы одну шкалу
    choices:
      - id: ask_tickets
        text: Попросить обоих показать билеты
        condition: [safety >= 40, flag chief_called]  # необязательно; все условия через «И»
        effects:
          loyalty: 5                # изменение шкал
          safety: 0
          set_flags: [tickets_checked]
          competences: {communication: 10, safety_rules: 5}
        next: check_tickets
        best: true                  # ровно один лучший вариант в узле — показывается в разборе
        explanation: Почему решение так влияет и как можно было лучше

  - id: final_success               # финальный узел: outcome + final_text, без вариантов
    outcome: success                # success | partial | failure
    final_text: Конфликт урегулирован.
```

Коды компетенций задаются в `backend/config/game.yaml`: `communication`, `first_aid`,
`safety_rules`, `service`, `stress_resistance`.

### Условия показа варианта

| Запись | Смысл |
|--------|-------|
| `safety >= 40` | шкала безопасности не меньше 40 (операторы `>=`, `<=`, `>`, `<`, `==`) |
| `loyalty < 30` | лояльность меньше 30 |
| `flag medic_called` | ранее установлен флаг |
| `not flag medic_called` | флаг не установлен |

## Что проверяет валидатор

- поля, типы и диапазоны; неизвестное поле — ошибка (опечатка `timout_next` не пройдёт молча);
- нет переходов в несуществующие узлы;
- нет недостижимых узлов;
- из каждого узла есть путь до финала (нет зацикливаний без выхода);
- условия записаны корректно, а флаги из условий где-то устанавливаются;
- коды компетенций есть в конфиге;
- у таймера есть `timeout_next`, а истечение таймера реально штрафует;
- в каждом узле ровно один `best: true`;
- узел не может оказаться тупиком: если у всех вариантов есть условия, нужен таймер.

Ошибки называют узел и вариант по их `id`:

```
✗ business-seat-conflict.yaml
    - узел «escalation», вариант «separate_and_call»: переход в несуществующий узел «chief_arrive»
    - недостижимые узлы (в них нет переходов от start_node): chief_arrives
```

## Как добавить развилку за 5 минут

Пример: в сценарии «Конфликт из-за места» добавить вариант «позвать коллегу из соседнего вагона»,
который ведёт в новый узел.

1. Откройте `backend/scenarios/business-seat-conflict.yaml`.
2. В узле `escalation` добавьте в `choices` новый вариант:

   ```yaml
         - id: call_colleague
           text: Позвать по рации проводника из соседнего вагона на помощь
           effects:
             safety: 5
             competences: {communication: 5, safety_rules: 5}
           next: colleague_arrives
           explanation: >
             Второй член экипажа помогает разделить пассажиров, но решение всё равно
             должен принять начальник поезда.
   ```

3. Добавьте новый узел в `nodes`:

   ```yaml
     - id: colleague_arrives
       situation: Коллега пришёл и встал между пассажирами. Спор стих, но не закончен.
       line:
         speaker: colleague
         name: Проводник соседнего вагона
         text: Я их подержу. Вызывай начальника поезда!
       timer_seconds: 15
       timeout_next: final_partial
       timeout_effects: {loyalty: -10}
       choices:
         - id: now_call_chief
           text: Вызвать начальника поезда
           effects: {safety: 5, set_flags: [chief_called]}
           next: chief_arrives
           best: true
           explanation: Старший на месте — конфликт решается по регламенту.
         - id: handle_alone
           text: Разобраться вдвоём без начальника
           effects: {loyalty: 5, safety: -10}
           next: final_partial
           explanation: Без начальника поезда решение не оформлено и может повториться.
   ```

4. Проверьте файл — без перезапуска:

   ```bash
   docker compose exec backend python -m app.scenarios.validator
   ```

5. Примените изменения:

   ```bash
   docker compose restart backend
   ```

Новый вариант появится в игре, разборе и аналитике. Рекомендации тоже учтут его сами:
они считаются по содержимому сценариев. Код менять не нужно: каталог `backend/scenarios`
подключён к контейнеру с диска.

Новый сценарий добавляется так же: создайте файл `<id>.yaml` рядом с остальными.
Проводникам придёт уведомление «Новый сценарий».

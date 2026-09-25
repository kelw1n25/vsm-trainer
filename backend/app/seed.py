"""Генератор синтетических данных для демо: депо, бригады, сотрудники и история прохождений.

Все ФИО и табельные номера вымышлены и собраны из случайных сочетаний (152-ФЗ).
История прохождений строится тем же движком, что и живая игра, — поэтому XP, ачивки,
рейтинг и аналитика у демо-сотрудников согласованы.

Запуск: автоматически при старте backend на пустой БД (SEED_DEMO_DATA=true) или вручную:
    python -m app.seed
"""

import logging
import random
from datetime import datetime, timedelta

from sqlalchemy import func, select, text, update
from sqlalchemy.orm import Session

from app.analytics.models import Event
from app.auth.security import hash_password
from app.db import SessionLocal
from app.engine.models import RunStatus, ScenarioRun
from app.engine.service import Rewards, apply_choice, apply_timeout, visible_choices
from app.profiles.models import Brigade, Depot, Employee, Role
from app.scenarios.loader import load_scenarios
from app.scenarios.models import Scenario
from app.scenarios.schema import ScenarioDefinition

logger = logging.getLogger(__name__)

DEMO_PASSWORD = "demo2026"
DEMO_CONDUCTOR = "100001"
DEMO_INSTRUCTOR = "900001"
HISTORY_DAYS = 35

DEPOTS = {
    "Депо Москва": ["Бригада 1", "Бригада 2", "Бригада 3"],
    "Депо Тверь": ["Бригада 4", "Бригада 5"],
    "Депо Санкт-Петербург": ["Бригада 6", "Бригада 7"],
}
CONDUCTORS_PER_BRIGADE = 6

LAST_NAMES = ["Иванов", "Смирнов", "Кузнецов", "Попов", "Волков", "Соколов", "Лебедев", "Козлов", "Новиков",
              "Морозов", "Орлов", "Павлов", "Семёнов", "Голубев", "Виноградов", "Богданов", "Воробьёв", "Фёдоров"]
MALE_NAMES = ["Алексей", "Дмитрий", "Сергей", "Андрей", "Максим", "Иван", "Артём", "Никита", "Михаил", "Егор"]
FEMALE_NAMES = ["Анна", "Мария", "Елена", "Ольга", "Наталья", "Екатерина", "Ирина", "Дарья", "Светлана", "Юлия"]
PATRONYMICS = ["Александров", "Сергеев", "Андреев", "Николаев", "Викторов", "Павлов", "Игорев", "Олегов"]

# История демо-проводника: заранее выбранные решения, чтобы аналитика показала понятные выводы
# («часто истекает таймер в медицинских сценариях»), а ачивки остались для живой демонстрации.
# None — дать таймеру истечь.
DEMO_HISTORY = [
    ("medical-heart-attack", [None, "start_cpr", None]),
    ("drunk-passenger", ["calm_approach", "sell_beer", "chief_and_police", "leave_him"]),
    ("train-delay-compensation", [None, "stop_door", "back_to_routine"]),
]


def seed_if_empty(db: Session) -> None:
    # Блокировка на время транзакции: две копии backend не создадут данные дважды
    db.execute(text("SELECT pg_advisory_xact_lock(20260927)"))
    if db.scalar(select(func.count()).select_from(Employee)):
        db.commit()
        return
    seed(db)
    db.commit()
    logger.info("Созданы синтетические данные для демо")


def seed(db: Session) -> None:
    rng = random.Random(2026)
    # Один хеш на всех: пароль у демо-учёток общий, а scrypt на каждого замедлил бы старт
    password_hash = hash_password(DEMO_PASSWORD)
    scenarios = db.scalars(select(Scenario).order_by(Scenario.id)).all()
    now = db.scalar(select(func.now()))

    number = int(DEMO_CONDUCTOR)
    conductors = []
    for depot_name, brigade_names in DEPOTS.items():
        depot = Depot(name=depot_name)
        for brigade_name in brigade_names:
            brigade = Brigade(name=brigade_name, depot=depot)
            for _ in range(CONDUCTORS_PER_BRIGADE):
                employee = Employee(
                    full_name=_synthetic_name(rng),
                    personnel_number=str(number),
                    password_hash=password_hash,
                    role=Role.CONDUCTOR,
                    brigade=brigade,
                    competence_points={},
                )
                db.add(employee)
                conductors.append(employee)
                number += 1
    moscow = conductors[0].brigade.depot
    db.add(Employee(
        full_name=_synthetic_name(rng),
        personnel_number=DEMO_INSTRUCTOR,
        password_hash=password_hash,
        role=Role.INSTRUCTOR,
        brigade=Brigade(name="Инструкторская группа", depot=moscow),
        competence_points={},
    ))
    db.flush()

    demo, others = conductors[0], conductors[1:]
    by_id = {s.id: s for s in scenarios}
    for index, (scenario_id, script) in enumerate(DEMO_HISTORY):
        finished = now - timedelta(days=5, hours=12 - index)
        _simulate(db, rng, demo, by_id[scenario_id], finished, script=script)

    for employee in others:
        skill = rng.uniform(0.35, 0.9)
        moments = sorted(now - timedelta(minutes=rng.randint(60, HISTORY_DAYS * 24 * 60)) for _ in range(rng.randint(1, 8)))
        for finished in moments:
            _simulate(db, rng, employee, rng.choice(scenarios), finished, skill=skill)


def _synthetic_name(rng: random.Random) -> str:
    last, patronymic = rng.choice(LAST_NAMES), rng.choice(PATRONYMICS)
    if rng.random() < 0.5:
        return f"{last} {rng.choice(MALE_NAMES)} {patronymic}ич"
    return f"{last}а {rng.choice(FEMALE_NAMES)} {patronymic}на"


def _simulate(
    db: Session,
    rng: random.Random,
    employee: Employee,
    scenario: Scenario,
    finished_at: datetime,
    skill: float = 0.5,
    script: list[str | None] | None = None,
) -> None:
    """Проходит сценарий движком: по сценарию решений (script) или случайно с учётом «навыка»."""
    definition = ScenarioDefinition.model_validate(scenario.definition)
    nodes = {node.id: node for node in definition.nodes}
    at = finished_at - timedelta(minutes=10)
    run = ScenarioRun(
        employee_id=employee.id,
        scenario_id=scenario.id,
        current_node_id=definition.start_node,
        loyalty=definition.initial.loyalty,
        safety=definition.initial.safety,
        flags=[],
        competence_points={},
        node_entered_at=at,
        started_at=at,
    )
    db.add(run)
    db.flush()
    db.add(Event(employee_id=employee.id, run_id=run.id, type="run_started", payload={"scenario_id": scenario.id}))

    steps = iter(script or [])
    rewards = Rewards()
    while run.status == RunStatus.IN_PROGRESS:
        node = nodes[run.current_node_id]
        if script is not None:
            choice_id = next(steps)
        elif node.timer_seconds and rng.random() < (1 - skill) * 0.3:
            choice_id = None
        else:
            options = visible_choices(node, run)
            best = next((c for c in options if c.best), None)
            choice_id = (best if best and rng.random() < skill else rng.choice(options)).id

        if choice_id is None:
            at += timedelta(seconds=node.timer_seconds)
            apply_timeout(db, run, scenario, definition, at, rewards)
        else:
            choice = next(c for c in node.choices if c.id == choice_id)
            elapsed = rng.uniform(0.4, 0.9) * node.timer_seconds if node.timer_seconds else rng.uniform(5, 40)
            at += timedelta(seconds=elapsed)
            apply_choice(db, run, scenario, definition, choice, elapsed, at, rewards)

    # Движок пишет события с текущим временем — переносим их в момент прохождения
    db.flush()
    db.execute(update(Event).where(Event.run_id == run.id).values(created_at=run.finished_at))


def main() -> None:
    logging.basicConfig(level=logging.INFO, format="%(levelname)s [%(name)s] %(message)s")
    with SessionLocal() as db:
        load_scenarios(db)
        seed_if_empty(db)


if __name__ == "__main__":
    main()

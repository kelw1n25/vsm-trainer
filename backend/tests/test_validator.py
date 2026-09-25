import copy

import pytest
import yaml

from app.handbook.data import handbook
from app.scenarios.conditions import FlagCondition, ScaleCondition, parse_condition
from app.scenarios.validator import SCENARIOS_DIR, validate_file, validate_text

SCENE = {"background": "salon", "characters": [{"id": "player", "position": "left"}, {"id": "guest", "position": "right"}]}

MINIMAL = {
    "id": "test",
    "title": "Тест",
    "category": "service",
    "difficulty": 1,
    "service_class": "Стандарт",
    "route": "Москва — Санкт-Петербург",
    "situations": [1],
    "initial": {"loyalty": 50, "safety": 50},
    "stats": {"trust": 50},
    "characters": {"guest": {"name": "Гость", "role": "Пассажир"}},
    "start_node": "start",
    "nodes": [
        {
            "id": "start",
            "scene": SCENE,
            "situation": "Ситуация",
            "dialogue": [
                {"speaker": "narrator", "text": "Поезд в пути."},
                {"speaker": "guest", "text": "Здравствуйте!", "expression": "happy", "sound": "sigh"},
                {"speaker": "player", "text": "Что ему нужно?", "thought": True},
            ],
            "timer_seconds": 10,
            "timeout_next": "bad_end",
            "timeout_effects": {"loyalty": -10},
            "choices": [
                {"id": "good", "text": "Хорошо", "next": "good_end", "explanation": "…", "best": True,
                 "effects": {"set_flags": ["helped"], "stats": {"trust": 10}},
                 "reaction": [{"speaker": "guest", "text": "Спасибо!"}]},
                {"id": "bad", "text": "Плохо", "next": "bad_end", "explanation": "…"},
            ],
        },
        {"id": "good_end", "scene": SCENE, "outcome": "success", "ending": "Всё хорошо", "final_text": "Успех"},
        {"id": "bad_end", "scene": {"background": "salon"}, "outcome": "failure", "ending": "Всё плохо", "final_text": "Провал"},
    ],
}


def validate(data: dict) -> list[str]:
    return validate_text(yaml.safe_dump(data, allow_unicode=True), data.get("id", "test"))[1]


@pytest.fixture
def scenario() -> dict:
    return copy.deepcopy(MINIMAL)


@pytest.mark.parametrize("path", sorted(SCENARIOS_DIR.glob("*.yaml")), ids=lambda p: p.name)
def test_repository_scenarios_are_valid(path):
    assert validate_file(path)[1] == []


def test_every_handbook_situation_is_trained():
    # Каждая из ситуаций справочника «Ситуации на борту» отрабатывается хотя бы в одном сценарии
    covered = set()
    for path in SCENARIOS_DIR.glob("*.yaml"):
        covered |= set(yaml.safe_load(path.read_text(encoding="utf-8"))["situations"])
    assert set(range(1, len(handbook.situations) + 1)) - covered == set()


def test_minimal_scenario_is_valid(scenario):
    assert validate(scenario) == []


def test_missing_node_reference(scenario):
    scenario["nodes"][0]["choices"][0]["next"] = "nowhere"
    errors = validate(scenario)
    assert "узел «start», вариант «good»: переход в несуществующий узел «nowhere»" in errors


def test_unreachable_node(scenario):
    scenario["nodes"].append({"id": "orphan", "scene": SCENE, "outcome": "partial", "ending": "…", "final_text": "…"})
    assert any("недостижимые узлы" in e and "orphan" in e for e in validate(scenario))


def test_branch_without_final(scenario):
    scenario["nodes"].append({
        "id": "loop",
        "scene": SCENE,
        "situation": "Ходим по кругу",
        "dialogue": [{"speaker": "narrator", "text": "Снова то же самое."}],
        "choices": [{"id": "again", "text": "Ещё раз", "next": "loop", "explanation": "…", "best": True}],
    })
    scenario["nodes"][0]["choices"][1]["next"] = "loop"
    assert any("нельзя дойти ни до одного финала" in e and "loop" in e for e in validate(scenario))


def test_bad_condition_syntax_points_to_choice(scenario):
    scenario["nodes"][0]["choices"][1]["condition"] = ["safety больше 40"]
    errors = validate(scenario)
    assert len(errors) == 1
    assert errors[0].startswith("узел «start», вариант «bad», поле condition")
    assert "не удалось разобрать условие" in errors[0]


def test_condition_on_flag_that_is_never_set(scenario):
    scenario["nodes"][0]["choices"][1]["condition"] = ["flag medic_called"]
    assert any("medic_called" in e and "нигде в сценарии не устанавливается" in e for e in validate(scenario))


def test_condition_on_hidden_stat(scenario):
    scenario["nodes"][0]["choices"][1]["condition"] = ["trust >= 60"]
    assert validate(scenario) == []
    scenario["nodes"][0]["choices"][1]["condition"] = ["courage > 5"]
    assert any("нет шкалы или скрытого параметра «courage»" in e for e in validate(scenario))


def test_effect_on_undeclared_stat(scenario):
    scenario["nodes"][0]["choices"][0]["effects"]["stats"] = {"tension": 10}
    assert any("скрытый параметр «tension» не объявлен" in e for e in validate(scenario))


def test_speaker_must_be_on_stage(scenario):
    scenario["nodes"][2]["scene"] = {"background": "salon"}
    scenario["nodes"][0]["choices"][0]["reaction"] = [{"speaker": "guest", "text": "Ок"}]
    scenario["nodes"][0]["scene"] = {"background": "salon", "characters": [{"id": "player", "position": "left"}]}
    errors = validate(scenario)
    assert any("говорит «guest», но его нет в сцене" in e and "dialogue" in e for e in errors)
    assert any("вариант «good», reaction" in e for e in errors)


def test_character_must_be_declared(scenario):
    scenario["nodes"][0]["scene"]["characters"].append({"id": "stranger", "position": "center"})
    assert any("персонаж «stranger» не объявлен" in e for e in validate(scenario))


def test_only_player_thinks(scenario):
    scenario["nodes"][0]["dialogue"][1]["thought"] = True
    assert any("мысли (thought: true) бывают только у проводника" in e for e in validate(scenario))


def test_unknown_sound(scenario):
    scenario["nodes"][0]["dialogue"][1]["sound"] = "scream"
    assert any("допустимые значения: 'sigh', 'sigh_long' or 'sigh_male'" in e for e in validate(scenario))


def test_final_needs_ending_title(scenario):
    del scenario["nodes"][1]["ending"]
    assert any("название финала ending" in e for e in validate(scenario))


def test_decision_needs_dialogue(scenario):
    scenario["nodes"][0]["dialogue"] = []
    assert any("нужен диалог (dialogue)" in e for e in validate(scenario))


def test_situation_must_exist_in_handbook(scenario):
    scenario["situations"] = [99]
    assert any("нет ситуаций с номерами 99" in e for e in validate(scenario))


def test_service_class_from_handbook(scenario):
    scenario["service_class"] = "Эконом"
    assert any("класс обслуживания «Эконом» не из справочника" in e for e in validate(scenario))


def test_unknown_competence(scenario):
    scenario["nodes"][0]["choices"][0]["effects"]["competences"] = {"charisma": 5}
    assert any("неизвестные компетенции: charisma" in e for e in validate(scenario))


def test_typo_in_field_name(scenario):
    scenario["nodes"][0]["timout_next"] = scenario["nodes"][0].pop("timeout_next")
    errors = validate(scenario)
    assert "узел «start», поле timout_next: неизвестное поле (опечатка в названии?)" in errors


def test_timeout_must_hurt(scenario):
    scenario["nodes"][0]["timeout_effects"] = {"loyalty": 5}
    assert any("истечение таймера должно снижать" in e for e in validate(scenario))


def test_exactly_one_best_choice(scenario):
    scenario["nodes"][0]["choices"][1]["best"] = True
    assert any("ровно один вариант" in e for e in validate(scenario))


def test_id_must_match_file_name(scenario):
    errors = validate_text(yaml.safe_dump(scenario), "other-name")[1]
    assert any("должен совпадать с именем файла" in e for e in errors)


def test_yaml_syntax_error():
    errors = validate_text("id: [broken", "test")[1]
    assert errors[0].startswith("ошибка синтаксиса YAML")


def test_parse_conditions():
    assert parse_condition("safety >= 40") == ScaleCondition("safety", ">=", 40)
    assert parse_condition("trust < 50") == ScaleCondition("trust", "<", 50)
    assert parse_condition("flag вызван_медик") == FlagCondition("вызван_медик", present=True)
    assert parse_condition("not flag medic_called") == FlagCondition("medic_called", present=False)
    with pytest.raises(ValueError):
        parse_condition("доверие > 5")

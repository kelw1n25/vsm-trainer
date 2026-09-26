import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "scripts"))

from export_openapi import TARGET, render  # noqa: E402


def test_openapi_file_matches_code():
    assert TARGET.read_text(encoding="utf-8") == render(), "docs/openapi.yaml устарел: python scripts/export_openapi.py"


def test_mobile_endpoints_are_documented():
    paths = __import__("yaml").safe_load(TARGET.read_text(encoding="utf-8"))["paths"]
    for path in ("/api/auth/login", "/api/auth/refresh", "/api/auth/logout", "/api/runs", "/api/runs/{run_id}/choices",
                 "/api/runs/{run_id}/debrief", "/api/leaderboard", "/api/analytics/events", "/api/notifications"):
        assert path in paths

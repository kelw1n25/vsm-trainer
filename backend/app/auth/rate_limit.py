import math
import threading
import time
from collections import defaultdict, deque

from app.config import settings
from app.errors import api_error


class LoginLimiter:
    """Ограничивает неудачные попытки входа скользящим окном — защита от подбора пароля.

    Считаются только неудачи: по табельному номеру (перебор паролей одного сотрудника) и по IP
    (перебор номеров с одного адреса). Успешный вход сбрасывает счётчик номера.
    Состояние в памяти процесса: при нескольких копиях backend лимит действует на каждую копию.
    """

    def __init__(self, max_failures: int, window_seconds: int, max_failures_per_ip: int) -> None:
        self.window = window_seconds
        self.limits = {"number": max_failures, "ip": max_failures_per_ip}
        self._failures: dict[tuple[str, str], deque[float]] = defaultdict(deque)
        self._lock = threading.Lock()

    def _recent(self, key: tuple[str, str], now: float) -> deque[float]:
        attempts = self._failures[key]
        while attempts and attempts[0] <= now - self.window:
            attempts.popleft()
        return attempts

    def check(self, personnel_number: str, ip: str) -> None:
        now = time.monotonic()
        with self._lock:
            for key in (("number", personnel_number), ("ip", ip)):
                attempts = self._recent(key, now)
                if len(attempts) >= self.limits[key[0]]:
                    retry_after = max(1, math.ceil(attempts[0] + self.window - now))
                    raise api_error(
                        429, "too_many_attempts",
                        f"Слишком много неудачных попыток входа. Повторите через {math.ceil(retry_after / 60)} мин.",
                        headers={"Retry-After": str(retry_after)},
                    )

    def failed(self, personnel_number: str, ip: str) -> None:
        now = time.monotonic()
        with self._lock:
            self._failures[("number", personnel_number)].append(now)
            self._failures[("ip", ip)].append(now)

    def succeeded(self, personnel_number: str) -> None:
        with self._lock:
            self._failures.pop(("number", personnel_number), None)

    def reset(self) -> None:
        with self._lock:
            self._failures.clear()


login_limiter = LoginLimiter(
    settings.login_max_failures, settings.login_window_seconds, settings.login_max_failures_per_ip
)

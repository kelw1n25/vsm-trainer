from pydantic import Field
from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    postgres_user: str
    postgres_password: str
    postgres_db: str
    postgres_host: str = "db"
    postgres_port: int = 5432
    # Для HS256 рекомендуется ключ не короче 32 байт (RFC 7518, 3.2)
    jwt_secret: str = Field(min_length=32)
    jwt_ttl_minutes: int = 720
    # Ключ для внешних систем (HR, LMS): заголовок X-API-Key
    integration_api_key: str = Field(min_length=32)
    # Создать синтетических сотрудников и историю при старте на пустой БД
    seed_demo_data: bool = True

    @property
    def database_url(self) -> str:
        return (
            f"postgresql+psycopg://{self.postgres_user}:{self.postgres_password}"
            f"@{self.postgres_host}:{self.postgres_port}/{self.postgres_db}"
        )


settings = Settings()

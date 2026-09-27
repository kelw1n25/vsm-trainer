# Единый образ для хостинга (Render): собранный веб-клиент + backend в одном сервисе.
# Локальная разработка по-прежнему через docker-compose.yml (отдельные контейнеры frontend и backend).

FROM node:22-alpine AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

FROM python:3.12-slim
WORKDIR /app
COPY backend/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY backend/alembic.ini .
COPY backend/alembic ./alembic
COPY backend/config ./config
COPY backend/scenarios ./scenarios
COPY backend/app ./app
COPY --from=web /web/dist ./web
ENV WEB_DIR=/app/web
# Хостинг задаёт порт в $PORT и ставит перед сервисом свой прокси: адрес клиента берём из X-Forwarded-For,
# иначе лимит неудачных входов по IP считался бы общим для всех пользователей
CMD ["sh", "-c", "alembic upgrade head && uvicorn app.main:app --host 0.0.0.0 --port ${PORT:-8000} --proxy-headers --forwarded-allow-ips='*'"]

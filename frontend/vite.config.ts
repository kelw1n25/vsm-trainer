import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// В docker-compose бэкенд доступен по имени сервиса; локально — через localhost.
const apiTarget = process.env.API_TARGET ?? "http://localhost:8000";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api": apiTarget,
    },
  },
});

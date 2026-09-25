import { useEffect, useState } from "react";

type ApiStatus = "checking" | "ok" | "unavailable";

const statusText: Record<ApiStatus, string> = {
  checking: "проверяем…",
  ok: "работает",
  unavailable: "недоступен",
};

export default function App() {
  const [status, setStatus] = useState<ApiStatus>("checking");

  useEffect(() => {
    fetch("/api/health")
      .then((response) => setStatus(response.ok ? "ok" : "unavailable"))
      .catch(() => setStatus("unavailable"));
  }, []);

  return (
    <main style={{ fontFamily: "system-ui, sans-serif", padding: 24 }}>
      <h1>ВСМ-тренажёр проводника</h1>
      <p>Backend: {statusText[status]}</p>
    </main>
  );
}

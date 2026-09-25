import type { ReactNode } from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router";
import { AuthProvider, useAuth } from "./auth";
import { Layout } from "./components/Layout";
import { AnalyticsPage } from "./pages/AnalyticsPage";
import { DebriefPage } from "./pages/DebriefPage";
import { HomePage } from "./pages/HomePage";
import { LoginPage } from "./pages/LoginPage";
import { NotificationsPage } from "./pages/NotificationsPage";
import { ProfilePage } from "./pages/ProfilePage";
import { RatingPage } from "./pages/RatingPage";
import { ScenarioPage } from "./pages/ScenarioPage";
import { ScenariosPage } from "./pages/ScenariosPage";
import { SettingsPage } from "./pages/SettingsPage";
import { StoryMapPage } from "./pages/StoryMapPage";
import { TeamPage } from "./pages/TeamPage";
import { StoryPlayer } from "./story/StoryPlayer";

function RequireAuth({ children }: { children: ReactNode }) {
  const { session } = useAuth();
  return session ? children : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          {/* Новелла открывается на весь экран — без шапки и навигации дашборда */}
          <Route
            path="/runs/:runId"
            element={
              <RequireAuth>
                <StoryPlayer />
              </RequireAuth>
            }
          />
          <Route
            element={
              <RequireAuth>
                <Layout />
              </RequireAuth>
            }
          >
            <Route path="/" element={<HomePage />} />
            <Route path="/scenarios" element={<ScenariosPage />} />
            <Route path="/scenarios/:scenarioId" element={<ScenarioPage />} />
            <Route path="/scenarios/:scenarioId/map" element={<StoryMapPage />} />
            <Route path="/runs/:runId/debrief" element={<DebriefPage />} />
            <Route path="/profile" element={<ProfilePage />} />
            <Route path="/rating" element={<RatingPage />} />
            <Route path="/analytics" element={<AnalyticsPage />} />
            <Route path="/notifications" element={<NotificationsPage />} />
            <Route path="/settings" element={<SettingsPage />} />
            <Route path="/team" element={<TeamPage />} />
            <Route path="/team/:employeeId" element={<AnalyticsPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

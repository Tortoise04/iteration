import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider, theme } from 'antd';
import Login from './components/Login';
import ProtectedRoute from './components/ProtectedRoute';
import Layout from './components/Layout';
import Dashboard from './components/Dashboard';
import GoalPage from './pages/GoalPage';
import PhoneUsagePage from './pages/PhoneUsagePage';
import DailyActivityPage from './pages/DailyActivityPage';
import PeriodSummaryPage from './pages/PeriodSummaryPage';
import AchievementPage from './pages/AchievementPage';
import StatisticsPage from './pages/StatisticsPage';

const { darkAlgorithm, defaultAlgorithm } = theme;

function App() {
  const savedTheme = localStorage.getItem('theme') || 'dark';
  const isDark = savedTheme === 'dark';

  return (
    <ConfigProvider
      theme={{
        algorithm: isDark ? darkAlgorithm : defaultAlgorithm,
        token: {
          colorPrimary: '#1890ff',
          colorText: isDark ? '#ffffffd9' : '#333333',
          colorBgLayout: isDark ? '#141414' : '#f5f5f5',
          colorBgContainer: isDark ? '#1f1f1f' : '#ffffff',
          colorBorder: isDark ? '#434343' : '#e8e8e8',
        },
      }}
    >
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<Login />} />

          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route index element={<Dashboard />} />
            <Route path="dashboard" element={<Dashboard />} />
            <Route path="phone-usage" element={<PhoneUsagePage />} />
            <Route path="goals" element={<GoalPage />} />
            <Route path="daily-activities" element={<DailyActivityPage />} />
            <Route path="period-summaries" element={<PeriodSummaryPage />} />
            <Route path="achievements" element={<AchievementPage />} />
            <Route path="statistics" element={<StatisticsPage />} />
          </Route>

          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </ConfigProvider>
  );
}

export default App;

import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { LanguageProvider } from './context/LanguageContext'
import { ThemeProvider } from './context/ThemeContext'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import VerifyEmailPage from './pages/VerifyEmailPage'

export default function App() {
  return (
    <ThemeProvider>
      <LanguageProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/"          element={<Navigate to="/login" replace />} />
            <Route path="/login"        element={<LoginPage />} />
            <Route path="/dashboard"    element={<DashboardPage />} />
            <Route path="/verify-email" element={<VerifyEmailPage />} />
          </Routes>
        </BrowserRouter>
      </LanguageProvider>
    </ThemeProvider>
  )
}

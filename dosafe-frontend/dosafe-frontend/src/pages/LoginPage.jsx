import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { login } from '../api/auth'
import { useLanguage } from '../context/LanguageContext'
import { useTheme } from '../context/ThemeContext'
import { FlagES, FlagUK } from '../components/FlagIcon'

export default function LoginPage() {
  const { lang, toggle, t } = useLanguage()
  const { theme, toggle: toggleTheme } = useTheme()
  const navigate = useNavigate()

  const [identifier, setIdentifier] = useState('')
  const [password, setPassword]     = useState('')
  const [errors, setErrors]         = useState({})
  const [apiError, setApiError]     = useState(null)
  const [loading, setLoading]       = useState(false)

  function validate() {
    const errs = {}
    if (!identifier.trim()) errs.identifier = t.required
    if (!password.trim())   errs.password   = t.required
    return errs
  }

  async function handleSubmit(e) {
    e.preventDefault()

    const errs = validate()
    if (Object.keys(errs).length) {
      setErrors(errs)
      return
    }

    setErrors({})
    setApiError(null)
    setLoading(true)

    try {
      const data = await login(identifier, password)
      localStorage.setItem('dosafe_token', data.token)
      navigate('/dashboard')
    } catch (err) {
      const code = err.code || 'DEFAULT'
      setApiError(t.errors[code] ?? t.errors.DEFAULT)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-root">

      {/* Animated grid + scanline atmosphere */}
      <div className="grid-bg"    aria-hidden="true" />
      <div className="scanlines"  aria-hidden="true" />
      <div className="corner-glow" aria-hidden="true" />

      {/* Top-right controls: theme + language */}
      <div className="login-top-controls">
        <button className="lang-toggle" onClick={toggleTheme} aria-label="Toggle theme">
          {theme === 'dark' ? <MoonIcon /> : <SunIcon />}
        </button>
        <button className="lang-toggle" onClick={toggle} aria-label="Toggle language">
          {lang === 'es' ? <><FlagES /> ES</> : <><FlagUK /> EN</>}
        </button>
      </div>

      {/* Main card */}
      <div className="login-card" role="main">
        <div className="card-accent-line" />

        {/* Wordmark */}
        <div className="wordmark">DoSafe</div>

        {/* Divider with system label */}
        <div className="divider-row" aria-hidden="true">
          <span className="divider-line" />
          <span className="system-label">{t.systemLabel}</span>
          <span className="divider-line" />
        </div>

        {/* Login form */}
        <form onSubmit={handleSubmit} noValidate>
          <div className="field-group">
            <label htmlFor="identifier">{t.identifier}</label>
            <input
              id="identifier"
              type="text"
              value={identifier}
              onChange={e => setIdentifier(e.target.value)}
              autoComplete="username"
              spellCheck={false}
              autoFocus
            />
            {errors.identifier && (
              <span className="field-error" role="alert">{errors.identifier}</span>
            )}
          </div>

          <div className="field-group">
            <label htmlFor="password">{t.password}</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={e => setPassword(e.target.value)}
              autoComplete="current-password"
            />
            {errors.password && (
              <span className="field-error" role="alert">{errors.password}</span>
            )}
          </div>

          {apiError && (
            <div className="api-error" role="alert">{apiError}</div>
          )}

          <button type="submit" disabled={loading} className="submit-btn">
            {loading ? t.loading : t.submit}
          </button>
        </form>

        <p className="card-nav-text">
          {t.noAccount} <Link to="/register">{t.registerLink}</Link>
        </p>

        {/* Footer */}
        <div className="card-footer">
          <span className="secure-badge">
            <LockIcon />
            {t.secureConn}
          </span>
        </div>
      </div>
    </div>
  )
}

function MoonIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
      <path d="M12.5 9A6 6 0 0 1 5 1.5a6 6 0 1 0 7.5 7.5Z"
        stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  )
}

function SunIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
      <circle cx="7" cy="7" r="2.8" stroke="currentColor" strokeWidth="1.2"/>
      <line x1="7" y1="0.5" x2="7" y2="2"    stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
      <line x1="7" y1="12"  x2="7" y2="13.5"  stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
      <line x1="0.5" y1="7" x2="2"   y2="7"   stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
      <line x1="12"  y1="7" x2="13.5" y2="7"  stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
      <line x1="2.4" y1="2.4"   x2="3.4" y2="3.4"   stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
      <line x1="10.6" y1="10.6" x2="11.6" y2="11.6" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
      <line x1="11.6" y1="2.4"  x2="10.6" y2="3.4"  stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
      <line x1="3.4"  y1="10.6" x2="2.4" y2="11.6"  stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
    </svg>
  )
}

function LockIcon() {
  return (
    <svg width="9" height="11" viewBox="0 0 9 11" fill="none" aria-hidden="true">
      <rect x="0.6" y="4.1" width="7.8" height="6.3" rx="0.8"
        stroke="currentColor" strokeWidth="1.1" />
      <path d="M2.5 4.1V2.8a2 2 0 0 1 4 0v1.3"
        stroke="currentColor" strokeWidth="1.1" strokeLinecap="round" />
      <circle cx="4.5" cy="7.25" r="0.85" fill="currentColor" />
    </svg>
  )
}

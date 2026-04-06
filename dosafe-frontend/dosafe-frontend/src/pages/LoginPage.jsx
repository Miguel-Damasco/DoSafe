import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { login } from '../api/auth'
import { useLanguage } from '../context/LanguageContext'
import { FlagES, FlagUK } from '../components/FlagIcon'

export default function LoginPage() {
  const { lang, toggle, t } = useLanguage()
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

      {/* Language toggle — fixed top-right */}
      <button className="lang-toggle" onClick={toggle} aria-label="Toggle language">
        {lang === 'es' ? <><FlagES /> ES</> : <><FlagUK /> EN</>}
      </button>

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

import { useEffect, useRef, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { verifyEmail } from '../api/auth'
import { useLanguage } from '../context/LanguageContext'

// Possible states of the verification attempt
const STATE = {
  LOADING:  'LOADING',
  SUCCESS:  'SUCCESS',
  ERROR:    'ERROR',
}

export default function VerifyEmailPage() {
  const [searchParams]    = useSearchParams()
  const navigate          = useNavigate()
  const { t }             = useLanguage()
  const [state, setState] = useState(STATE.LOADING)
  const [errorMsg, setErrorMsg] = useState(null)

  // React 18 Strict Mode runs effects twice in development.
  // This ref ensures the verification request is only sent once —
  // without it, the token would be consumed on the first call and
  // the second call would fail with INVALID_VERIFICATION_TOKEN.
  const hasCalled = useRef(false)

  useEffect(() => {
    if (hasCalled.current) return
    hasCalled.current = true

    const token = searchParams.get('token')

    if (!token) {
      setState(STATE.ERROR)
      setErrorMsg(t.errors.DEFAULT)
      return
    }

    verifyEmail(token)
      .then(() => {
        setState(STATE.SUCCESS)
        // Give the user a moment to read the confirmation, then go to dashboard
        // (or login if they are not authenticated yet).
        setTimeout(() => {
          const hasSession = !!localStorage.getItem('dosafe_token')
          navigate(hasSession ? '/dashboard' : '/login', { replace: true })
        }, 2500)
      })
      .catch(err => {
        const code = err.code || 'DEFAULT'
        setState(STATE.ERROR)
        setErrorMsg(t.verifyErrors?.[code] ?? t.verifyErrors?.DEFAULT)
      })
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <div className="verify-page">
      <div className="grid-bg"   aria-hidden="true" />
      <div className="scanlines" aria-hidden="true" />

      <div className="verify-card">
        <div className="card-accent-line" />

        <p className="verify-wordmark">Do<span>Safe</span></p>

        {state === STATE.LOADING && (
          <>
            <span className="spinner" style={{ margin: '1.5rem auto', display: 'block' }} />
            <p className="verify-msg">{t.verifyLoading}</p>
          </>
        )}

        {state === STATE.SUCCESS && (
          <>
            <p className="verify-icon">✓</p>
            <p className="verify-msg verify-msg--success">{t.verifySuccess}</p>
            <p className="verify-sub">{t.verifyRedirecting}</p>
          </>
        )}

        {state === STATE.ERROR && (
          <>
            <p className="verify-icon verify-icon--error">✕</p>
            <p className="verify-msg verify-msg--error">{errorMsg}</p>
            <button className="verify-back-btn" onClick={() => navigate('/login')}>
              {t.verifyGoToLogin}
            </button>
          </>
        )}
      </div>
    </div>
  )
}

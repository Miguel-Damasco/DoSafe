const BASE_URL = 'http://localhost:8080'

/**
 * Authenticates a user against the DoSafe backend.
 *
 * @param {string} identifier  Username or email
 * @param {string} password
 * @returns {Promise<{token: string, refreshToken: string}>}
 * @throws  Error with .code matching the backend error code (e.g. "USER_NOT_FOUND")
 */
export async function login(identifier, password) {
  let response

  try {
    response = await fetch(`${BASE_URL}/authentication/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ identifier, password }),
    })
  } catch {
    // fetch() itself threw — network unreachable, CORS preflight failed, etc.
    const err = new Error('NETWORK_ERROR')
    err.code = 'NETWORK_ERROR'
    throw err
  }

  const body = await response.json()

  if (!body.meta?.success) {
    const code = body.error?.code || 'DEFAULT'
    const err = new Error(code)
    err.code = code
    throw err
  }

  localStorage.setItem('dosafe_email_verified', String(body.data.emailVerified))
  localStorage.setItem('dosafe_email', body.data.email ?? '')
  return body.data
}

/**
 * Verifies an email address using the token from the verification link.
 * @param {string} token
 */
export async function verifyEmail(token) {
  let response

  try {
    response = await fetch(
      `${BASE_URL}/authentication/verify-email?token=${encodeURIComponent(token)}`
    )
  } catch {
    const err = new Error('NETWORK_ERROR')
    err.code = 'NETWORK_ERROR'
    throw err
  }

  const body = await response.json()

  if (!body.meta?.success) {
    const code = body.error?.code || 'DEFAULT'
    const err = new Error(code)
    err.code = code
    throw err
  }

  // Mark email as verified in localStorage so the banner disappears immediately.
  localStorage.setItem('dosafe_email_verified', 'true')
}

/**
 * Registers a new user account.
 * On success the backend also sends a verification email automatically.
 *
 * @param {string} username
 * @param {string} email
 * @param {string} password
 * @returns {Promise<{id: number, username: string}>}
 * @throws  Error with .code matching the backend error code (e.g. "USER_ALREADY_EXISTS")
 */
export async function register(username, email, password) {
  let response

  try {
    response = await fetch(`${BASE_URL}/authentication/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, email, password }),
    })
  } catch {
    const err = new Error('NETWORK_ERROR')
    err.code = 'NETWORK_ERROR'
    throw err
  }

  const body = await response.json()

  if (!body.meta?.success) {
    const code = body.error?.code || 'DEFAULT'
    const err = new Error(code)
    err.code = code
    throw err
  }

  return body.data
}

/**
 * Sends a new verification email to the given address.
 * Always returns 200 on the backend (anti-enumeration) — never throws on 200.
 * @param {string} email
 */
export async function resendVerification(email) {
  let response

  try {
    response = await fetch(`${BASE_URL}/authentication/resend-verification`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email }),
    })
  } catch {
    const err = new Error('NETWORK_ERROR')
    err.code = 'NETWORK_ERROR'
    throw err
  }

  const body = await response.json()

  if (!body.meta?.success) {
    const code = body.error?.code || 'DEFAULT'
    const err = new Error(code)
    err.code = code
    throw err
  }
}

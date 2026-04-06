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

  return body.data
}

const BASE_URL = 'http://localhost:8080'

function authHeaders() {
  const token = localStorage.getItem('dosafe_token')
  return { Authorization: `Bearer ${token}` }
}

/**
 * Returns the number of alerts sent to the user but not yet read.
 * @returns {Promise<number>}
 */
export async function getAlertUnreadCount() {
  let response
  try {
    response = await fetch(`${BASE_URL}/alert/my-alerts/unread-count`, {
      headers: authHeaders(),
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
  return body.data.count
}

/**
 * Returns a paginated list of sent alerts for the authenticated user.
 * @param {number} page  0-based page index
 * @param {number} size  Max 50
 * @returns {Promise<{content, page, size, totalElements, totalPages, last}>}
 */
export async function getMyAlerts(page = 0, size = 20) {
  let response
  try {
    response = await fetch(
      `${BASE_URL}/alert/my-alerts?page=${page}&size=${size}`,
      { headers: authHeaders() }
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
  return body.data
}

/**
 * Marks all unread sent alerts as read for the authenticated user.
 * @returns {Promise<void>}
 */
export async function markAllAlertsRead() {
  let response
  try {
    response = await fetch(`${BASE_URL}/alert/my-alerts/mark-all-read`, {
      method: 'POST',
      headers: authHeaders(),
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

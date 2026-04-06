const BASE_URL = 'http://localhost:8080'

function authHeaders() {
  const token = localStorage.getItem('dosafe_token')
  return { Authorization: `Bearer ${token}` }
}

/**
 * Returns a page of the authenticated user's documents.
 * @param {number} page  0-based page index
 * @param {number} size  Max 50
 * @returns {Promise<{content, page, size, totalElements, totalPages, last}>}
 */
export async function getMyDocuments(page = 0, size = 10) {
  let response

  try {
    response = await fetch(
      `${BASE_URL}/document/my-documents?page=${page}&size=${size}`,
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
 * Updates the expiration date of a document.
 * @param {string} documentId  UUID
 * @param {string} expireAt    ISO date string "YYYY-MM-DD"
 * @returns {Promise<DocumentSummaryResponseDTO>}
 */
export async function updateExpirationDate(documentId, expireAt) {
  let response

  try {
    response = await fetch(`${BASE_URL}/document/${documentId}/expiration-date`, {
      method: 'PATCH',
      headers: { ...authHeaders(), 'Content-Type': 'application/json' },
      body: JSON.stringify({ expireAt }),
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
 * Uploads an identity document file (JPG, PNG, or PDF).
 * @param {File} file
 * @returns {Promise<{id, status, originalFilename, type, expireAt, createdAt}>}
 */
export async function uploadDocument(file) {
  const formData = new FormData()
  formData.append('file', file)

  let response

  try {
    response = await fetch(`${BASE_URL}/document/upload`, {
      method: 'POST',
      headers: authHeaders(),
      body: formData,
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

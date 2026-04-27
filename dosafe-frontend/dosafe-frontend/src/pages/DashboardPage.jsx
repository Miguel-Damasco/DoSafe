import { useState, useEffect, useRef, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMyDocuments, getMyDocumentsByType, getMyExpiredDocuments, uploadDocument, updateExpirationDate, deleteDocument } from '../api/documents'
import { getAlertUnreadCount, getMyAlerts, markAllAlertsRead } from '../api/alerts'
import { resendVerification } from '../api/auth'
import { useLanguage } from '../context/LanguageContext'
import { useTheme } from '../context/ThemeContext'
import { FlagES, FlagUK } from '../components/FlagIcon'

// ─── Helpers ──────────────────────────────────────────────────────────────────

function formatDate(isoString) {
  if (!isoString) return '—'
  return new Date(isoString).toLocaleDateString(undefined, {
    year: 'numeric', month: 'short', day: 'numeric',
  })
}

function isExpired(dateStr) {
  if (!dateStr) return false
  return new Date(dateStr) < new Date()
}

// ─── Status badge ──────────────────────────────────────────────────────────────

function StatusBadge({ status, t }) {
  const label = {
    PROCESSING: t.processing,
    PROCESSED:  t.processed,
    FAILED:     t.failed,
  }[status] ?? status

  return (
    <span className={`status-badge ${status.toLowerCase()}`}>
      {label}
    </span>
  )
}

// ─── PDF thumbnail ──────────────────────────────────────────────────────────────

// Uses an <iframe> scaled down with CSS transform to show the first page of the PDF.
// The iframe does a browser navigation (not fetch), so S3 CORS doesn't apply —
// unlike PDF.js / react-pdf which do an XHR that S3 would block without CORS config.
//
// Technique: render the iframe at a fixed large width (RENDER_W), then scale it down
// so it fills the card. pointer-events:none lets clicks fall through to the card.
// The iframe renders at this fixed width. We then scale it down to fit the card.
const RENDER_W = 640

function PdfThumbnail({ downloadUrl }) {
  const [loaded, setLoaded] = useState(false)
  const containerRef        = useRef(null)
  const [scale, setScale]   = useState(0.44) // initial estimate; corrected on mount

  // ResizeObserver: whenever the card changes width (responsive grid), recalculate
  // the scale so the iframe always fills the container exactly.
  useEffect(() => {
    if (!containerRef.current) return
    const observer = new ResizeObserver(entries => {
      const width = entries[0].contentRect.width
      if (width > 0) setScale(width / RENDER_W)
    })
    observer.observe(containerRef.current)
    return () => observer.disconnect()
  }, [])

  if (!downloadUrl) {
    return (
      <div className="pdf-thumb pdf-thumb--pending">
        <ScanIcon />
      </div>
    )
  }

  return (
    <div className="pdf-thumb" ref={containerRef}>
      {!loaded && <div className="pdf-thumb__skeleton" />}
      <div className="pdf-iframe-wrapper">
        <iframe
          src={downloadUrl}
          title="document preview"
          scrolling="no"
          tabIndex={-1}
          style={{
            width:  RENDER_W,
            height: RENDER_W * (4 / 3),
            transform: `scale(${scale})`,
            transformOrigin: 'top left',
          }}
          onLoad={() => setLoaded(true)}
        />
      </div>
    </div>
  )
}

// ─── Expiration date editor ────────────────────────────────────────────────────

// Inline editor for the expiration date field inside a card.
// Shows the date as text with a pencil button. On click, switches to a date input.
// Returns today's date as "YYYY-MM-DD" in the local timezone — used as the min
// attribute of the date input so the browser's picker greys out past dates.
function todayISO() {
  const d = new Date()
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function ExpirationDateField({ doc, t, onUpdated }) {
  const [editing, setEditing]   = useState(false)
  const [value, setValue]       = useState(doc.expireAt ?? '')   // "YYYY-MM-DD" or ""
  const [loading, setLoading]   = useState(false)
  const [success, setSuccess]   = useState(false)
  const [error, setError]       = useState(null)
  const inputRef                = useRef(null)

  // Focus the input as soon as it appears
  useEffect(() => {
    if (editing && inputRef.current) inputRef.current.focus()
  }, [editing])

  async function handleSave() {
    if (!value) return
    // Client-side guard: reject past dates immediately without hitting the server
    if (value < todayISO()) {
      setError(t.errors.INVALID_EXPIRATION_DATE)
      return
    }
    setLoading(true)
    setSuccess(false)
    setError(null)
    try {
      const updated = await updateExpirationDate(doc.id, value)
      setSuccess(true)
      setEditing(false)
      onUpdated(updated)
      setTimeout(() => setSuccess(false), 2000)
    } catch (err) {
      const code = err.code || 'DEFAULT'
      setError(t.errors[code] ?? t.errors.DEFAULT)
    } finally {
      setLoading(false)
    }
  }

  function handleCancel() {
    setValue(doc.expireAt ?? '')
    setEditing(false)
    setError(null)
  }

  function handleKeyDown(e) {
    if (e.key === 'Enter')  handleSave()
    if (e.key === 'Escape') handleCancel()
  }

  const expired = isExpired(value || doc.expireAt)

  if (editing) {
    return (
      <div className="expire-edit-col">
        <div className="expire-edit-row">
          <input
            ref={inputRef}
            type="date"
            className="expire-input"
            value={value}
            min={todayISO()}
            onChange={e => { setValue(e.target.value); setError(null) }}
            onKeyDown={handleKeyDown}
            disabled={loading}
          />
          <button
            className="expire-action-btn expire-save"
            onClick={handleSave}
            disabled={loading || !value}
            title={t.saveExpireAt}
          >
            {loading ? '…' : '✓'}
          </button>
          <button
            className="expire-action-btn expire-cancel"
            onClick={handleCancel}
            disabled={loading}
            title={t.cancelEdit}
          >
            ✕
          </button>
        </div>
        {error && <span className="expire-field-error">{error}</span>}
      </div>
    )
  }

  return (
    <div className="expire-display-row">
      <span className={`meta-value ${expired ? 'expired' : ''}`}>
        {formatDate(value || doc.expireAt)}
      </span>
      {success && <span className="expire-success">✓</span>}
      <button
        className="expire-edit-btn"
        onClick={() => setEditing(true)}
        title={t.editExpireAt}
        aria-label={t.editExpireAt}
      >
        <PencilIcon />
      </button>
    </div>
  )
}

// ─── Delete confirm modal ──────────────────────────────────────────────────────

function DeleteConfirmModal({ filename, docId, t, onConfirmed, onClose }) {
  const [deleting, setDeleting] = useState(false)
  const [error, setError]       = useState(null)

  async function handleConfirm() {
    setDeleting(true)
    setError(null)
    try {
      await deleteDocument(docId)
      onConfirmed()
    } catch (err) {
      const code = err.code || 'DEFAULT'
      setError(t.errors[code] ?? t.errors.DEFAULT)
      setDeleting(false)
    }
  }

  function handleBackdropClick(e) {
    if (e.target === e.currentTarget && !deleting) onClose()
  }

  return (
    <div className="modal-backdrop" onClick={handleBackdropClick} role="dialog" aria-modal="true">
      <div className="modal-box delete-modal-box">
        <div className="card-accent-line card-accent-line--error" />

        <p className="modal-title">{t.deleteConfirm}</p>
        <p className="delete-modal-filename">{filename}</p>

        {error && <div className="modal-error" role="alert">{error}</div>}

        <div className="modal-actions">
          <button className="modal-cancel" onClick={onClose} disabled={deleting}>
            {t.cancelEdit}
          </button>
          <button
            className="modal-confirm modal-confirm--danger"
            onClick={handleConfirm}
            disabled={deleting}
          >
            {deleting ? t.deleting : t.deleteDocument}
          </button>
        </div>
      </div>
    </div>
  )
}

// ─── Document card ─────────────────────────────────────────────────────────────

function DocCard({ doc, t, onDeleted }) {
  // Local copy of the doc so we can update it after a successful date edit
  // without waiting for a full page reload.
  const [localDoc, setLocalDoc] = useState(doc)
  const [showDeleteModal, setShowDeleteModal] = useState(false)
  const typeLabel = localDoc.type ? (t.typeLabels[localDoc.type] ?? t.unknownType) : '—'

  const typeClass = {
    IDENTITY_CARD:  'doc-card--identity',
    PASSPORT:       'doc-card--passport',
    DRIVER_LICENCE: 'doc-card--licence',
    OTHER:          'doc-card--other',
  }[localDoc.type] ?? 'doc-card--other'

  const expiredClass = isExpired(localDoc.expireAt) ? 'doc-card--expired' : ''

  return (
    <article className={`doc-card ${typeClass} ${expiredClass}`}>
      <PdfThumbnail downloadUrl={localDoc.downloadUrl} />

      <div className="doc-card-body">
        <div className="doc-card-header">
          <p className="doc-filename">{localDoc.originalFilename}</p>
          <div className="doc-card-header-right">
            <StatusBadge status={localDoc.status} t={t} />
            <button
              className="delete-btn"
              onClick={() => setShowDeleteModal(true)}
              title={t.deleteDocument}
              aria-label={t.deleteDocument}
            >
              <TrashIcon />
            </button>
          </div>
        </div>

        {showDeleteModal && (
          <DeleteConfirmModal
            filename={localDoc.originalFilename}
            docId={localDoc.id}
            t={t}
            onConfirmed={onDeleted}
            onClose={() => setShowDeleteModal(false)}
          />
        )}

        <span className="type-pill">{typeLabel}</span>

        <div className="doc-meta">
          <div className="meta-row">
            <span className="meta-label">{t.expires}</span>
            <ExpirationDateField
              doc={localDoc}
              t={t}
              onUpdated={updated => setLocalDoc(prev => ({ ...prev, expireAt: updated.expireAt }))}
            />
          </div>
          <div className="meta-divider" />
          <div className="meta-row">
            <span className="meta-label">{t.uploaded}</span>
            <span className="meta-value">{formatDate(localDoc.createdAt)}</span>
          </div>
        </div>

        {localDoc.downloadUrl && (
          <a
            href={localDoc.downloadUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="download-link"
          >
            <DownloadIcon />
            {t.download}
          </a>
        )}
      </div>
    </article>
  )
}

// ─── Upload modal ──────────────────────────────────────────────────────────────

function UploadModal({ onClose, onSuccess, t }) {
  const [file, setFile]         = useState(null)
  const [dragOver, setDragOver] = useState(false)
  const [loading, setLoading]   = useState(false)
  const [error, setError]       = useState(null)
  const [success, setSuccess]   = useState(false)

  function handleFile(f) {
    setError(null)
    setSuccess(false)
    setFile(f)
  }

  function handleDrop(e) {
    e.preventDefault()
    setDragOver(false)
    const f = e.dataTransfer.files[0]
    if (f) handleFile(f)
  }

  async function handleSubmit() {
    if (!file) return
    setLoading(true)
    setError(null)

    try {
      await uploadDocument(file)
      setSuccess(true)
      // Give the user a moment to read the success message, then close + refresh
      setTimeout(() => {
        onSuccess()
        onClose()
      }, 1800)
    } catch (err) {
      const code = err.code || 'DEFAULT'
      setError(t.errors[code] ?? t.errors.DEFAULT)
    } finally {
      setLoading(false)
    }
  }

  // Close on backdrop click
  function handleBackdropClick(e) {
    if (e.target === e.currentTarget) onClose()
  }

  return (
    <div className="modal-backdrop" onClick={handleBackdropClick} role="dialog" aria-modal="true">
      <div className="modal-box">
        <div className="card-accent-line" />

        <p className="modal-title">{t.uploadTitle}</p>
        <p className="modal-sub">{t.uploadSub}</p>

        {/* Drop zone */}
        <div
          className={`drop-zone ${dragOver ? 'drag-over' : ''}`}
          onDragOver={e => { e.preventDefault(); setDragOver(true) }}
          onDragLeave={() => setDragOver(false)}
          onDrop={handleDrop}
        >
          <input
            type="file"
            accept=".jpg,.jpeg,.png,.pdf"
            onChange={e => e.target.files[0] && handleFile(e.target.files[0])}
            aria-label={t.uploadDrop}
          />
          <UploadIcon />
          <p className="drop-label" style={{ marginTop: '0.6rem' }}>{t.uploadDrop}</p>
          {file && (
            <p className="drop-selected">{file.name}</p>
          )}
        </div>

        {error   && <div className="modal-error"   role="alert">{error}</div>}
        {success && <div className="modal-success" role="status">{t.uploadSuccess}</div>}

        <div className="modal-actions">
          <button className="modal-cancel" onClick={onClose} disabled={loading}>
            {t.uploadCancel}
          </button>
          <button
            className="modal-confirm"
            onClick={handleSubmit}
            disabled={!file || loading || success}
          >
            {loading ? t.uploading : t.uploadConfirm}
          </button>
        </div>
      </div>
    </div>
  )
}

// ─── Email verification banner ────────────────────────────────────────────────

function EmailVerificationBanner({ t }) {
  const [dismissed, setDismissed]   = useState(false)
  const [verified, setVerified]     = useState(
    () => localStorage.getItem('dosafe_email_verified') === 'true'
  )
  const [loading, setLoading]       = useState(false)
  const [success, setSuccess]       = useState(false)
  const [error, setError]           = useState(null)

  // Listen for localStorage changes from other tabs (e.g. user verifies email
  // in the tab that opened from the verification link).
  useEffect(() => {
    function handleStorage(e) {
      if (e.key === 'dosafe_email_verified' && e.newValue === 'true') {
        setVerified(true)
      }
    }
    window.addEventListener('storage', handleStorage)
    return () => window.removeEventListener('storage', handleStorage)
  }, [])

  if (verified || dismissed) return null

  async function handleResend() {
    const email = localStorage.getItem('dosafe_email') || ''
    setLoading(true)
    setError(null)
    setSuccess(false)
    try {
      await resendVerification(email)
      setSuccess(true)
    } catch (err) {
      const code = err.code || 'DEFAULT'
      setError(t.errors[code] ?? t.errors.DEFAULT)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="verification-banner">
      <div className="verification-banner__body">
        <WarningIcon />
        <div className="verification-banner__text">
          <span className="verification-banner__title">{t.emailNotVerified}</span>
          <span className="verification-banner__sub">{t.emailNotVerifiedSub}</span>
        </div>
        {success ? (
          <span className="verification-banner__success">{t.resendSuccess}</span>
        ) : (
          <button
            className="verification-banner__btn"
            onClick={handleResend}
            disabled={loading}
          >
            {loading ? t.resendingEmail : t.resendEmail}
          </button>
        )}
        {error && <span className="verification-banner__error">{error}</span>}
      </div>
      <button
        className="verification-banner__close"
        onClick={() => setDismissed(true)}
        aria-label="Dismiss"
      >
        ✕
      </button>
    </div>
  )
}

// ─── Dashboard page ────────────────────────────────────────────────────────────

export default function DashboardPage() {
  const { lang, toggle, t } = useLanguage()
  const { theme, toggle: toggleTheme } = useTheme()
  const navigate = useNavigate()

  // activeFilter: 'ALL' | 'IDENTITY_CARD' | 'PASSPORT' | 'DRIVER_LICENCE' | 'OTHER' | 'EXPIRED'
  const [activeFilter, setActiveFilter] = useState('ALL')
  const [docsPage, setDocsPage] = useState({ content: [], page: 0, totalPages: 0, totalElements: 0, last: true })
  const [currentPage, setCurrentPage] = useState(0)
  const [loading, setLoading]         = useState(true)
  const [showUpload, setShowUpload]   = useState(false)
  const [showAlerts, setShowAlerts]   = useState(false)
  const [expiredCount, setExpiredCount] = useState(0)

  const loadExpiredCount = useCallback(async () => {
    try {
      const count = await getAlertUnreadCount()
      setExpiredCount(count)
    } catch { /* silently ignore — not critical */ }
  }, [])

  useEffect(() => {
    loadExpiredCount()
  }, [loadExpiredCount])

  const loadDocuments = useCallback(async (page = 0) => {
    setLoading(true)
    try {
      let data
      if (activeFilter === 'ALL') {
        data = await getMyDocuments(page, 9)
      } else if (activeFilter === 'EXPIRED') {
        data = await getMyExpiredDocuments(page, 9)
      } else {
        data = await getMyDocumentsByType(activeFilter, page, 9)
      }
      setDocsPage(data)
    } catch (err) {
      if (err.code === 'UNAUTHORIZED' || err.code === 'NETWORK_ERROR') {
        navigate('/login')
      }
    } finally {
      setLoading(false)
    }
  }, [activeFilter, navigate])

  useEffect(() => {
    loadDocuments(currentPage)
  }, [currentPage, loadDocuments])

  function handleFilterChange(filter) {
    setActiveFilter(filter)
    setCurrentPage(0)
  }

  function handleLogout() {
    localStorage.removeItem('dosafe_token')
    localStorage.removeItem('dosafe_email_verified')
    localStorage.removeItem('dosafe_email')
    navigate('/login')
  }

  function goToPage(page) {
    setCurrentPage(page)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  // Returns a mixed array of 0-based page indices and null (= ellipsis).
  // Always shows first, last, current, and one neighbour on each side.
  function pageWindows(current, total) {
    if (total <= 7) return Array.from({ length: total }, (_, i) => i)
    const visible = new Set([0, total - 1, current])
    if (current > 0) visible.add(current - 1)
    if (current < total - 1) visible.add(current + 1)
    const sorted = [...visible].sort((a, b) => a - b)
    const result = []
    for (let i = 0; i < sorted.length; i++) {
      if (i > 0 && sorted[i] - sorted[i - 1] > 1) result.push(null)
      result.push(sorted[i])
    }
    return result
  }

  return (
    <div className="dashboard-root">
      <div className="grid-bg"   aria-hidden="true" />
      <div className="scanlines" aria-hidden="true" />

      {/* ── Header ── */}
      <header className="dashboard-header">
        <div className="header-wordmark">
          Do<span>Safe</span>
        </div>
        <div className="header-right">
          <button className="header-btn alert-btn" onClick={() => setShowAlerts(true)} aria-label="Alertas">
            <span className={`alert-count${expiredCount > 0 ? ' alert-count--active' : ''}`}>
              {expiredCount}
            </span>
            <BellIcon />
          </button>
          <button className="header-btn theme-toggle-btn" onClick={toggleTheme} aria-label="Toggle theme">
            {theme === 'dark' ? <MoonIcon /> : <SunIcon />}
          </button>
          <button className="header-btn lang-flag-btn" onClick={toggle} aria-label="Toggle language">
            {lang === 'es' ? <><FlagES /> ES</> : <><FlagUK /> EN</>}
          </button>
          <button className="header-btn" onClick={handleLogout}>
            {t.logout}
          </button>
        </div>
      </header>

      <EmailVerificationBanner t={t} />

      {/* ── Content ── */}
      <main className="dashboard-content">
        <div className="page-title-row">
          <div>
            <h1 className="page-title">{t.myDocuments}</h1>
            {!loading && (
              <p className="page-count">
                {docsPage.totalElements} {lang === 'es' ? 'documento(s)' : 'document(s)'}
              </p>
            )}
          </div>
          <div className="title-row-actions">
            <select
              className="filter-select"
              value={activeFilter}
              onChange={e => handleFilterChange(e.target.value)}
              aria-label="Filter documents"
            >
              <option value="ALL">{t.filterAll}</option>
              <option value="IDENTITY_CARD">{t.typeLabels.IDENTITY_CARD}</option>
              <option value="PASSPORT">{t.typeLabels.PASSPORT}</option>
              <option value="DRIVER_LICENCE">{t.typeLabels.DRIVER_LICENCE}</option>
              <option value="OTHER">{t.typeLabels.OTHER}</option>
              <option value="EXPIRED">{t.filterExpired}</option>
            </select>
            <button className="upload-btn" onClick={() => setShowUpload(true)}>
              <PlusIcon />
              {t.uploadDocument}
            </button>
          </div>
        </div>

        {/* ── Documents grid ── */}
        {loading ? (
          <div className="loading-state">
            <span className="spinner" />
            {t.loadingDocuments}
          </div>
        ) : (
          <>
            <div className="docs-grid">
              {docsPage.content.length === 0 ? (
                <div className="empty-state">
                  <FolderIcon className="empty-icon" />
                  <p className="empty-title">{t.noDocuments}</p>
                  <p className="empty-sub">{t.noDocumentsSub}</p>
                </div>
              ) : (
                docsPage.content.map(doc => (
                  <DocCard key={doc.id} doc={doc} t={t} onDeleted={() => { loadDocuments(currentPage); loadExpiredCount() }} />
                ))
              )}
            </div>

            {/* ── Pagination ── */}
            {docsPage.totalElements > 0 && <nav className="pagination" aria-label="Pagination">
              <button
                className="page-btn"
                disabled={docsPage.page === 0}
                onClick={() => goToPage(currentPage - 1)}
              >
                ←
              </button>

              <div className="pagination-pages">
                {pageWindows(currentPage, Math.max(1, docsPage.totalPages)).map((p, i) =>
                  p === null
                    ? <span key={`dots-${i}`} className="pagination-dots">…</span>
                    : <button
                        key={p}
                        className={`page-num ${p === currentPage ? 'active' : ''}`}
                        onClick={() => goToPage(p)}
                      >
                        {p + 1}
                      </button>
                )}
              </div>

              <button
                className="page-btn"
                disabled={docsPage.last}
                onClick={() => goToPage(currentPage + 1)}
              >
                →
              </button>
            </nav>}
          </>
        )}
      </main>

      {/* ── Upload modal ── */}
      {showUpload && (
        <UploadModal
          t={t}
          onClose={() => setShowUpload(false)}
          onSuccess={() => { setCurrentPage(0); loadDocuments(0); loadExpiredCount() }}
        />
      )}

      {/* ── Alerts panel ── */}
      {showAlerts && (
        <AlertsPanel
          t={t}
          onClose={() => setShowAlerts(false)}
          onRead={() => setExpiredCount(0)}
        />
      )}
    </div>
  )
}

// ─── Alerts panel ─────────────────────────────────────────────────────────────

const TYPE_LABEL = {
  IDENTITY_CARD:  'DNI',
  PASSPORT:       'Pasaporte',
  DRIVER_LICENCE: 'Licencia',
  OTHER:          'Otro',
}

function AlertsPanel({ t, onClose, onRead }) {
  const [alerts, setAlerts]   = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState(null)

  useEffect(() => {
    async function loadAndRead() {
      try {
        const data = await getMyAlerts(0, 50)
        setAlerts(data.content)
        // Mark all as read and reset the bell counter in the parent
        await markAllAlertsRead()
        onRead()
      } catch {
        setError('No se pudieron cargar las alertas.')
      } finally {
        setLoading(false)
      }
    }
    loadAndRead()
  }, [onRead])

  function handleBackdropClick(e) {
    if (e.target === e.currentTarget) onClose()
  }

  return (
    <div className="alerts-backdrop" onClick={handleBackdropClick}>
      <aside className="alerts-panel">

        {/* Header */}
        <div className="alerts-panel-header">
          <div className="alerts-panel-title">
            <BellIcon />
            <span>ALERTAS</span>
          </div>
          <button className="alerts-panel-close" onClick={onClose} aria-label="Cerrar">✕</button>
        </div>

        {/* Body */}
        <div className="alerts-panel-body">
          {loading ? (
            <div className="loading-state">
              <span className="spinner" />
            </div>
          ) : error ? (
            <div className="alerts-empty">
              <p className="alerts-empty-text">{error}</p>
            </div>
          ) : alerts.length === 0 ? (
            <div className="alerts-empty">
              <BellIcon />
              <p className="alerts-empty-text">Sin alertas por ahora</p>
              <p className="alerts-empty-sub">Te notificaremos cuando un documento esté por vencer.</p>
            </div>
          ) : (
            <ul className="alerts-list">
              {alerts.map(alert => (
                <li key={alert.id} className={`alert-item${!alert.read ? ' alert-item--unread' : ''}`}>
                  <div className="alert-item-type">
                    {TYPE_LABEL[alert.documentType] ?? alert.documentType ?? '—'}
                  </div>
                  <div className="alert-item-body">
                    <p className="alert-item-filename">{alert.documentFilename}</p>
                    <p className="alert-item-meta">
                      Vence {formatDate(alert.expireAt)}
                    </p>
                    <p className="alert-item-sent">
                      Email enviado {formatDate(alert.sentAt)}
                    </p>
                  </div>
                  {!alert.read && <span className="alert-item-dot" aria-hidden="true" />}
                </li>
              ))}
            </ul>
          )}
        </div>

      </aside>
    </div>
  )
}

// ─── Icons ────────────────────────────────────────────────────────────────────

function WarningIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none" aria-hidden="true" style={{ flexShrink: 0 }}>
      <path d="M8 2L14.5 13.5H1.5L8 2Z" stroke="currentColor" strokeWidth="1.3" strokeLinejoin="round"/>
      <line x1="8" y1="7" x2="8" y2="10" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round"/>
      <circle cx="8" cy="12" r="0.6" fill="currentColor"/>
    </svg>
  )
}

function PlusIcon() {
  return (
    <svg width="11" height="11" viewBox="0 0 11 11" fill="none" aria-hidden="true">
      <line x1="5.5" y1="1" x2="5.5" y2="10" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"/>
      <line x1="1"   y1="5.5" x2="10" y2="5.5" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"/>
    </svg>
  )
}

function DownloadIcon() {
  return (
    <svg width="10" height="11" viewBox="0 0 10 11" fill="none" aria-hidden="true">
      <path d="M5 1v6.5M2 7.5l3 2.5 3-2.5" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round"/>
      <line x1="1" y1="10" x2="9" y2="10" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
    </svg>
  )
}

function UploadIcon() {
  return (
    <svg width="28" height="28" viewBox="0 0 28 28" fill="none" aria-hidden="true" style={{ margin: '0 auto', display: 'block' }}>
      <path d="M14 18V6M9 11l5-5 5 5" stroke="var(--text-muted)" strokeWidth="1.4" strokeLinecap="round" strokeLinejoin="round"/>
      <path d="M4 20v2a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2v-2" stroke="var(--border)" strokeWidth="1.4" strokeLinecap="round"/>
    </svg>
  )
}

function FolderIcon({ className }) {
  return (
    <svg width="48" height="48" viewBox="0 0 48 48" fill="none" aria-hidden="true" className={className}>
      <path d="M6 14a3 3 0 0 1 3-3h10l4 5h16a3 3 0 0 1 3 3v16a3 3 0 0 1-3 3H9a3 3 0 0 1-3-3V14Z"
        stroke="currentColor" strokeWidth="1.5"/>
    </svg>
  )
}

// Shown inside the card when the document is still PROCESSING
function ScanIcon() {
  return (
    <svg width="40" height="40" viewBox="0 0 40 40" fill="none" aria-hidden="true">
      <rect x="8" y="6" width="24" height="28" rx="2" stroke="currentColor" strokeWidth="1.4"/>
      <line x1="13" y1="14" x2="27" y2="14" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
      <line x1="13" y1="19" x2="27" y2="19" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
      <line x1="13" y1="24" x2="21" y2="24" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
    </svg>
  )
}

// Shown inside the card when the PDF fails to load
function PencilIcon() {
  return (
    <svg width="10" height="10" viewBox="0 0 10 10" fill="none" aria-hidden="true">
      <path d="M7 1.5l1.5 1.5-5 5L2 8.5l.5-1.5 4.5-5.5z"
        stroke="currentColor" strokeWidth="1.1" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  )
}

function TrashIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 11 11" fill="none" aria-hidden="true">
      <path d="M1.5 3h8M4 3V2h3v1M2.5 3l.5 6h5l.5-6" stroke="currentColor" strokeWidth="1.1" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  )
}

function DocIcon() {
  return (
    <svg width="36" height="36" viewBox="0 0 36 36" fill="none" aria-hidden="true">
      <path d="M8 4h14l8 8v20a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z"
        stroke="currentColor" strokeWidth="1.4"/>
      <path d="M22 4v8h8" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"/>
    </svg>
  )
}

function BellIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 14 14" fill="none" aria-hidden="true">
      <line x1="7" y1="0.8" x2="7" y2="2" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
      <path d="M3.5 9V5.5a3.5 3.5 0 0 1 7 0V9"
        stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" strokeLinejoin="round"/>
      <line x1="2" y1="9" x2="12" y2="9" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
      <path d="M5.7 10a1.3 1.3 0 0 0 2.6 0"
        stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
    </svg>
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
      <line x1="7" y1="0.5" x2="7" y2="2"   stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
      <line x1="7" y1="12" x2="7" y2="13.5" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
      <line x1="0.5" y1="7" x2="2"   y2="7" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
      <line x1="12"  y1="7" x2="13.5" y2="7" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
      <line x1="2.4" y1="2.4" x2="3.4" y2="3.4" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
      <line x1="10.6" y1="10.6" x2="11.6" y2="11.6" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
      <line x1="11.6" y1="2.4" x2="10.6" y2="3.4" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
      <line x1="3.4"  y1="10.6" x2="2.4" y2="11.6" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round"/>
    </svg>
  )
}

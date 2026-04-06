import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMyDocuments, uploadDocument, updateExpirationDate } from '../api/documents'
import { useLanguage } from '../context/LanguageContext'
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

// ─── Document card ─────────────────────────────────────────────────────────────

function DocCard({ doc, t }) {
  // Local copy of the doc so we can update it after a successful date edit
  // without waiting for a full page reload.
  const [localDoc, setLocalDoc] = useState(doc)
  const typeLabel = localDoc.type ? (t.typeLabels[localDoc.type] ?? t.unknownType) : '—'

  return (
    <article className="doc-card">
      <PdfThumbnail downloadUrl={localDoc.downloadUrl} />

      <div className="doc-card-body">
        <div className="doc-card-header">
          <p className="doc-filename">{localDoc.originalFilename}</p>
          <StatusBadge status={localDoc.status} t={t} />
        </div>

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

// ─── Dashboard page ────────────────────────────────────────────────────────────

export default function DashboardPage() {
  const { lang, toggle, t } = useLanguage()
  const navigate = useNavigate()

  const [docsPage, setDocsPage] = useState({ content: [], page: 0, totalPages: 0, totalElements: 0, last: true })
  const [currentPage, setCurrentPage] = useState(0)
  const [loading, setLoading]         = useState(true)
  const [showUpload, setShowUpload]   = useState(false)

  async function loadDocuments(page = 0) {
    setLoading(true)
    try {
      const data = await getMyDocuments(page, 9)
      setDocsPage(data)
    } catch (err) {
      // If unauthorized, redirect to login
      if (err.code === 'UNAUTHORIZED' || err.code === 'NETWORK_ERROR') {
        navigate('/login')
      }
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadDocuments(currentPage)
  }, [currentPage])

  function handleLogout() {
    localStorage.removeItem('dosafe_token')
    navigate('/login')
  }

  function goToPage(page) {
    setCurrentPage(page)
    window.scrollTo({ top: 0, behavior: 'smooth' })
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
          <button className="header-btn lang-flag-btn" onClick={toggle} aria-label="Toggle language">
            {lang === 'es' ? <><FlagES /> ES</> : <><FlagUK /> EN</>}
          </button>
          <button className="header-btn" onClick={handleLogout}>
            {t.logout}
          </button>
        </div>
      </header>

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
          <button className="upload-btn" onClick={() => setShowUpload(true)}>
            <PlusIcon />
            {t.uploadDocument}
          </button>
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
                  <DocCard key={doc.id} doc={doc} t={t} />
                ))
              )}
            </div>

            {/* ── Pagination ── */}
            {docsPage.totalPages > 1 && (
              <nav className="pagination" aria-label="Pagination">
                <span className="pagination-info">
                  {t.page} {docsPage.page + 1} {t.of} {docsPage.totalPages}
                </span>
                <div className="pagination-btns">
                  <button
                    className="page-btn"
                    disabled={docsPage.page === 0}
                    onClick={() => goToPage(currentPage - 1)}
                  >
                    ← {t.previous}
                  </button>
                  <button
                    className="page-btn"
                    disabled={docsPage.last}
                    onClick={() => goToPage(currentPage + 1)}
                  >
                    {t.next} →
                  </button>
                </div>
              </nav>
            )}
          </>
        )}
      </main>

      {/* ── Upload modal ── */}
      {showUpload && (
        <UploadModal
          t={t}
          onClose={() => setShowUpload(false)}
          onSuccess={() => loadDocuments(0).then(() => setCurrentPage(0))}
        />
      )}
    </div>
  )
}

// ─── Icons ────────────────────────────────────────────────────────────────────

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

function DocIcon() {
  return (
    <svg width="36" height="36" viewBox="0 0 36 36" fill="none" aria-hidden="true">
      <path d="M8 4h14l8 8v20a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z"
        stroke="currentColor" strokeWidth="1.4"/>
      <path d="M22 4v8h8" stroke="currentColor" strokeWidth="1.4" strokeLinecap="round"/>
    </svg>
  )
}

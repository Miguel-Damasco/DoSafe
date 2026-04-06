// SVG flag icons for Spain and UK.
// Used instead of emoji because Windows doesn't render regional indicator
// emoji as flags — they appear as plain text (ES, GB) instead.

export function FlagES() {
  return (
    <svg width="18" height="13" viewBox="0 0 18 13" aria-hidden="true" style={{ display: 'block', flexShrink: 0 }}>
      {/* Red top stripe */}
      <rect width="18" height="3" fill="#c60b1e" />
      {/* Yellow middle stripe */}
      <rect y="3" width="18" height="7" fill="#ffc400" />
      {/* Red bottom stripe */}
      <rect y="10" width="18" height="3" fill="#c60b1e" />
    </svg>
  )
}

export function FlagUK() {
  return (
    <svg width="18" height="13" viewBox="0 0 60 40" aria-hidden="true" style={{ display: 'block', flexShrink: 0 }}>
      <rect width="60" height="40" fill="#012169" />
      {/* White diagonals */}
      <path d="M0,0 L60,40 M60,0 L0,40" stroke="#fff" strokeWidth="8" />
      {/* Red diagonals */}
      <path d="M0,0 L60,40 M60,0 L0,40" stroke="#c8102e" strokeWidth="5" />
      {/* White cross */}
      <path d="M30,0 V40 M0,20 H60" stroke="#fff" strokeWidth="13" />
      {/* Red cross */}
      <path d="M30,0 V40 M0,20 H60" stroke="#c8102e" strokeWidth="8" />
    </svg>
  )
}

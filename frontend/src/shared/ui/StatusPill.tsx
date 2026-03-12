interface StatusPillProps {
  value: string
}

export function StatusPill({ value }: StatusPillProps) {
  const normalized = value.toLowerCase()
  const label = value.replaceAll('_', ' ')

  return <span className={`status-pill ${normalized}`}>{label}</span>
}

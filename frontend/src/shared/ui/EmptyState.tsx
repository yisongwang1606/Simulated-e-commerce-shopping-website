interface EmptyStateProps {
  title: string
  message: string
}

export function EmptyState({ title, message }: EmptyStateProps) {
  return (
    <div className="empty-state centered">
      <p className="eyebrow">Nothing here yet</p>
      <h2 className="card-title">{title}</h2>
      <p className="section-copy">{message}</p>
    </div>
  )
}

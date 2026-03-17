interface LoadingStateProps {
  title?: string
}

export function LoadingState({
  title = 'Loading the operations workspace...',
}: LoadingStateProps) {
  return (
    <div className="loading-state centered">
      <p className="eyebrow">Please wait</p>
      <h2 className="card-title">{title}</h2>
      <p className="section-copy">Queue data, order metrics, and catalog controls are being prepared.</p>
    </div>
  )
}

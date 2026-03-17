interface LoadingStateProps {
  title?: string
}

export function LoadingState({
  title = 'Loading your page...',
}: LoadingStateProps) {
  return (
    <div className="loading-state centered">
      <p className="eyebrow">Please wait</p>
      <h2 className="card-title">{title}</h2>
      <p className="section-copy">We are getting the latest catalog and account information ready.</p>
    </div>
  )
}

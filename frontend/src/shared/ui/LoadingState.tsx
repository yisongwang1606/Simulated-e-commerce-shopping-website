interface LoadingStateProps {
  title?: string
}

export function LoadingState({
  title = 'Loading live data from the backend...',
}: LoadingStateProps) {
  return (
    <div className="loading-state centered">
      <p className="eyebrow">Please wait</p>
      <h2 className="card-title">{title}</h2>
      <p className="section-copy">
        The React shell is connected to your Spring Boot API and is preparing
        the next view.
      </p>
    </div>
  )
}

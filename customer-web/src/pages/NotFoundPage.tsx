import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <section className="surface centered stack-lg">
      <p className="eyebrow">404</p>
      <h1 className="page-title">That route is not part of the storefront shell.</h1>
      <p className="section-copy">
        The page you requested does not exist in this React build yet.
      </p>
      <div className="hero-actions" style={{ justifyContent: 'center' }}>
        <Link className="button" to="/">
          Back home
        </Link>
        <Link className="button-outline" to="/catalog">
          Open catalog
        </Link>
      </div>
    </section>
  )
}

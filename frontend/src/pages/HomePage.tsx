import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'

import { getCategories, getPopularProducts } from '../api/products'
import type { PopularProduct } from '../api/contracts'
import { extractErrorMessage } from '../shared/error'
import { formatCurrency } from '../shared/formatters'
import { LoadingState } from '../shared/ui/LoadingState'
import { ProductCard } from '../shared/ui/ProductCard'
import { SectionHeading } from '../shared/ui/SectionHeading'

export function HomePage() {
  const [popularProducts, setPopularProducts] = useState<PopularProduct[]>([])
  const [categories, setCategories] = useState<string[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    const loadHome = async () => {
      setIsLoading(true)
      setErrorMessage('')

      try {
        const [popular, categoryList] = await Promise.all([
          getPopularProducts(6),
          getCategories(),
        ])

        setPopularProducts(popular)
        setCategories(categoryList)
      } catch (error) {
        setErrorMessage(extractErrorMessage(error))
      } finally {
        setIsLoading(false)
      }
    }

    void loadHome()
  }, [])

  if (isLoading) {
    return <LoadingState title="Building the storefront overview..." />
  }

  return (
    <div className="stack-lg">
      <section className="hero">
        <div className="hero-grid">
          <div className="stack-lg">
            <div className="hero-copy-block">
              <p className="eyebrow">React storefront shell</p>
              <h1 className="hero-title">
                A frontend control layer ready for products, carts, orders, and
                admin ops.
              </h1>
              <p className="hero-copy">
                This is the first working React pass: routed, token-aware, and
                wired to your live Swagger-documented backend instead of demo
                JSON.
              </p>
            </div>

            <div className="hero-actions">
              <Link className="button" to="/catalog">
                Browse catalog
              </Link>
              <Link className="button-outline" to="/login">
                Sign in with demo users
              </Link>
              <a
                className="button-ghost"
                href="http://127.0.0.1:8080/swagger-ui.html"
                rel="noreferrer"
                target="_blank"
              >
                Open Swagger
              </a>
            </div>
          </div>

          <div className="metric-grid">
            <article className="stat-card">
              <span className="eyebrow">Catalog breadth</span>
              <strong>{categories.length}</strong>
              <span className="supporting-copy">Distinct product categories</span>
            </article>
            <article className="stat-card">
              <span className="eyebrow">Trending now</span>
              <strong>{popularProducts.length}</strong>
              <span className="supporting-copy">Popular products from Redis</span>
            </article>
            <article className="stat-card">
              <span className="eyebrow">Currency mode</span>
              <strong>{formatCurrency(89.99)}</strong>
              <span className="supporting-copy">Formatted in en-CA CAD</span>
            </article>
          </div>
        </div>
      </section>

      {errorMessage ? <div className="message error">{errorMessage}</div> : null}

      <section className="surface stack-lg">
        <SectionHeading
          description="These cards come from the live /api/products/popular endpoint and give the frontend a realistic hero feed."
          eyebrow="Popular feed"
          title="Redis-backed product momentum"
        />

        <div className="product-grid">
          {popularProducts.map((entry) => (
            <ProductCard key={entry.product.id} product={entry.product} />
          ))}
        </div>
      </section>

      <section className="home-grid">
        <article className="surface stack">
          <SectionHeading
            description="Customer-facing features are already mapped to your backend routes."
            eyebrow="Flow"
            title="User journey"
          />
          <div className="chip-row">
            <span className="chip">Register / Login</span>
            <span className="chip">Browse products</span>
            <span className="chip">Cart in Redis</span>
            <span className="chip">Create orders</span>
          </div>
        </article>

        <article className="surface stack">
          <SectionHeading
            description="Admin screens are separated behind JWT role checks."
            eyebrow="Ops"
            title="Back office"
          />
          <div className="chip-row">
            <span className="chip">Create products</span>
            <span className="chip">Inspect orders</span>
            <span className="chip">Review status</span>
          </div>
        </article>

        <article className="surface stack">
          <SectionHeading
            description="Use these credentials while the frontend shell is still being expanded."
            eyebrow="Demo access"
            title="Ready-made accounts"
          />
          <div className="stack">
            <span className="signal">Admin@example.com / 123456</span>
            <span className="signal">Jack@example.com / 123456</span>
          </div>
        </article>
      </section>
    </div>
  )
}

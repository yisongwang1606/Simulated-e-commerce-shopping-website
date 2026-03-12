import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'

import { getCategories, getPopularProducts } from '../api/products'
import type { PopularProduct } from '../api/contracts'
import { extractErrorMessage } from '../shared/error'
import { formatCurrency, formatInteger } from '../shared/formatters'
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
              <p className="eyebrow">Commerce workspace</p>
              <h1 className="hero-title">
                The storefront now feels like the customer-facing side of a
                real operations platform.
              </h1>
              <p className="hero-copy">
                Customers can browse live inventory, move through cart and
                order flows, and step into after-sales service while the admin
                workspace manages the same data in parallel.
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
                Review API docs
              </a>
            </div>

            <div className="hero-story-grid">
              <article className="story-card">
                <p className="eyebrow">Storefront</p>
                <strong>Live catalog and cart</strong>
                <span className="supporting-copy">
                  Product search, stock-aware detail pages, Redis-backed cart,
                  and address-based checkout.
                </span>
              </article>
              <article className="story-card">
                <p className="eyebrow">Service</p>
                <strong>After-sales handling</strong>
                <span className="supporting-copy">
                  Refund requests, shipment visibility, and support tickets
                  live next to each order.
                </span>
              </article>
              <article className="story-card">
                <p className="eyebrow">Operations</p>
                <strong>Admin control tower</strong>
                <span className="supporting-copy">
                  Search orders, tag issues, review refunds, manage support,
                  and watch stock risk from one console.
                </span>
              </article>
            </div>
          </div>

          <div className="metric-grid">
            <article className="stat-card">
              <span className="eyebrow">Catalog breadth</span>
              <strong>{formatInteger(categories.length)}</strong>
              <span className="supporting-copy">Distinct live product categories</span>
            </article>
            <article className="stat-card">
              <span className="eyebrow">Trending now</span>
              <strong>{formatInteger(popularProducts.length)}</strong>
              <span className="supporting-copy">Popular products ranked from Redis</span>
            </article>
            <article className="stat-card">
              <span className="eyebrow">Store currency</span>
              <strong>{formatCurrency(89.99)}</strong>
              <span className="supporting-copy">Formatted for en-CA checkout flow</span>
            </article>
          </div>
        </div>
      </section>

      {errorMessage ? <div className="message error">{errorMessage}</div> : null}

      <section className="surface stack-lg">
        <SectionHeading
          description="These cards come from the live /api/products/popular endpoint and give the homepage a real merchandising rail instead of placeholder content."
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
            description="The customer side is no longer just a mock shell. Every state on this panel calls real backend routes."
            eyebrow="Flow"
            title="Customer journey"
          />
          <div className="chip-row">
            <span className="chip">Register / Login</span>
            <span className="chip">Browse products</span>
            <span className="chip">Cart in Redis</span>
            <span className="chip">Address checkout</span>
            <span className="chip">Create orders</span>
          </div>
        </article>

        <article className="surface stack">
          <SectionHeading
            description="The operations workspace is role-protected and tied to the same order, refund, and support data as the storefront."
            eyebrow="Ops"
            title="Back office"
          />
          <div className="chip-row">
            <span className="chip">Dashboard summary</span>
            <span className="chip">Create products</span>
            <span className="chip">Search orders</span>
            <span className="chip">Review refunds</span>
            <span className="chip">Support tickets</span>
          </div>
        </article>

        <article className="surface stack">
          <SectionHeading
            description="Use these seeded accounts while validating the enterprise workflow from both customer and admin views."
            eyebrow="Demo access"
            title="Ready-made accounts"
          />
          <div className="stack">
            <span className="signal">admin@ecom.local / Admin123!</span>
            <span className="signal">demo@ecom.local / Demo123!</span>
          </div>
        </article>
      </section>
    </div>
  )
}

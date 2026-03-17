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
              <p className="eyebrow">Customer storefront</p>
              <h1 className="hero-title">
                Shop the live catalog and manage the full order journey in one place.
              </h1>
              <p className="hero-copy">
                Browse live inventory, move from cart to checkout, and handle
                delivery, refund, and support follow-up without leaving the
                customer portal.
              </p>
            </div>

            <div className="hero-actions">
              <Link className="button" to="/catalog">
                Browse catalog
              </Link>
              <Link className="button-outline" to="/login">
                Sign in to shop
              </Link>
            </div>

            <div className="hero-story-grid">
              <article className="story-card">
                <p className="eyebrow">Browse</p>
                <strong>Live catalog and cart</strong>
                <span className="supporting-copy">
                  Product search, stock-aware detail pages, Redis-backed cart,
                  and address-based checkout.
                </span>
              </article>
              <article className="story-card">
                <p className="eyebrow">Checkout</p>
                <strong>Address-based order placement</strong>
                <span className="supporting-copy">
                  Choose a delivery address, place an order, and keep the
                  customer-facing flow simple and direct.
                </span>
              </article>
              <article className="story-card">
                <p className="eyebrow">After-sales</p>
                <strong>Delivery, refunds, and support</strong>
                <span className="supporting-copy">
                  Track shipment progress, request refunds, and open support
                  cases from the order history.
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
            description="The customer portal keeps the purchase journey focused on shopping, checkout, and service follow-up."
            eyebrow="Service"
            title="Customer tools"
          />
          <div className="chip-row">
            <span className="chip">Shipment tracking</span>
            <span className="chip">Refund requests</span>
            <span className="chip">Support cases</span>
            <span className="chip">Order history</span>
            <span className="chip">Address book</span>
          </div>
        </article>

        <article className="surface stack">
          <SectionHeading
            description="Use the seeded customer account for checkout, order history, refunds, and support testing."
            eyebrow="Demo access"
            title="Ready-made customer account"
          />
          <div className="stack">
            <span className="signal">demo@ecom.local / Demo123!</span>
            <span className="signal">Guest browsing is also available from the catalog.</span>
          </div>
        </article>
      </section>
    </div>
  )
}

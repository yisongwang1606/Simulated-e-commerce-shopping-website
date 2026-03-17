import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'

import { getCategories, getPopularProducts } from '../api/products'
import type { PopularProduct } from '../api/contracts'
import { extractErrorMessage } from '../shared/error'
import { formatCurrency, formatInteger } from '../shared/formatters'
import { LoadingState } from '../shared/ui/LoadingState'
import { ProductCard } from '../shared/ui/ProductCard'

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
          getPopularProducts(8),
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
    return <LoadingState title="Opening the marketplace..." />
  }

  return (
    <div className="market-stack">
      <section className="market-hero">
        <aside className="market-category-rail">
          <p className="eyebrow">Departments</p>
          {categories.slice(0, 8).map((category) => (
            <Link className="market-category-link" key={category} to={`/catalog?keyword=${encodeURIComponent(category)}`}>
              {category}
            </Link>
          ))}
        </aside>

        <div className="market-hero-main">
          <div className="market-hero-copy">
            <p className="eyebrow">Marketplace event</p>
            <h1>Everyday deals, quick checkout, and clear order follow-up in one marketplace.</h1>
            <p>
              Browse by department, compare offers fast, and keep returns, delivery updates,
              and support in the same account without leaving the storefront.
            </p>
            <div className="market-hero-actions">
              <Link className="button" to="/catalog">
                Shop the catalog
              </Link>
              <Link className="button-outline" to="/orders">
                View recent orders
              </Link>
            </div>
          </div>

          <div className="market-promo-grid">
            <article className="market-promo-card accent">
              <span className="eyebrow">Featured</span>
              <strong>{formatInteger(popularProducts.length)} products trending</strong>
              <span>Fast-moving picks shoppers are checking most often.</span>
            </article>
            <article className="market-promo-card dark">
              <span className="eyebrow">Checkout</span>
              <strong>Address-aware ordering</strong>
              <span>Save an address, place the order, and keep every update in one place.</span>
            </article>
          </div>
        </div>

        <aside className="market-hero-sidebar">
          <article className="market-stat-tile">
            <span className="eyebrow">Starting offer</span>
            <strong>{formatCurrency(popularProducts[0]?.product.price ?? 49.99)}</strong>
            <span>Prices shown in Canadian dollars.</span>
          </article>
          <article className="market-stat-tile">
            <span className="eyebrow">Category count</span>
            <strong>{formatInteger(categories.length)}</strong>
            <span>Departments ready to browse today.</span>
          </article>
          <article className="market-stat-tile">
            <span className="eyebrow">Demo login</span>
            <strong>demo@ecom.local</strong>
            <span>Use `Demo123!` for full customer flows.</span>
          </article>
        </aside>
      </section>

      {errorMessage ? <div className="message error">{errorMessage}</div> : null}

      <section className="market-strip">
        <div className="market-strip-card">
          <span className="eyebrow">Store promise</span>
          <strong>Fast browse</strong>
          <span>Server-side catalog search and category filtering.</span>
        </div>
        <div className="market-strip-card">
          <span className="eyebrow">Order journey</span>
          <strong>Cart to after-sales</strong>
          <span>Checkout, shipment follow-up, refunds, and support in one account.</span>
        </div>
        <div className="market-strip-card">
          <span className="eyebrow">Payments</span>
          <strong>Stripe test mode</strong>
          <span>Embedded card payment flow for customer checkout testing.</span>
        </div>
      </section>

      <section className="market-section">
        <div className="market-section-heading">
          <div>
            <p className="eyebrow">Hot products</p>
            <h2>Trending offers right now</h2>
          </div>
          <Link className="ghost-link" to="/catalog">
            Browse full catalog
          </Link>
        </div>
        <div className="product-grid market-product-grid">
          {popularProducts.map((entry) => (
            <ProductCard key={entry.product.id} product={entry.product} />
          ))}
        </div>
      </section>

      <section className="market-section market-two-up">
        <article className="market-info-card">
          <p className="eyebrow">Shopping highlights</p>
          <h3>What customers can do</h3>
          <ul className="market-bullet-list">
            <li>Live catalog, product detail, cart, and checkout</li>
            <li>Address selection persisted to the order snapshot</li>
            <li>Order history with payment, refund, and support actions</li>
            <li>Customer-facing experience split from the admin portal</li>
          </ul>
        </article>

        <article className="market-info-card">
          <p className="eyebrow">Store promises</p>
          <h3>Built for repeat buying</h3>
          <ul className="market-bullet-list">
            <li>Dense browsing, clear pricing, and fast cart actions</li>
            <li>Payment, shipment, and return status in the order workspace</li>
            <li>Support case intake directly from each order</li>
            <li>Separate customer and admin experiences</li>
          </ul>
        </article>
      </section>
    </div>
  )
}

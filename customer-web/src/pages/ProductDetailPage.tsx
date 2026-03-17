import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'

import { addCartItem } from '../api/cart'
import { getProduct } from '../api/products'
import type { Product } from '../api/contracts'
import { useSessionStore } from '../store/sessionStore'
import { extractErrorMessage } from '../shared/error'
import { formatCurrency } from '../shared/formatters'
import { LoadingState } from '../shared/ui/LoadingState'
import { SectionHeading } from '../shared/ui/SectionHeading'

export function ProductDetailPage() {
  const { productId } = useParams()
  const navigate = useNavigate()
  const token = useSessionStore((state) => state.token)
  const numericProductId = Number(productId)

  const [product, setProduct] = useState<Product | null>(null)
  const [quantity, setQuantity] = useState(1)
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [message, setMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')

  useEffect(() => {
    const loadProduct = async () => {
      if (!Number.isFinite(numericProductId)) {
        setErrorMessage('Invalid product identifier')
        setIsLoading(false)
        return
      }

      setIsLoading(true)
      setErrorMessage('')

      try {
        const data = await getProduct(numericProductId)
        setProduct(data)
        setQuantity(data.stock > 0 ? 1 : 0)
      } catch (error) {
        setErrorMessage(extractErrorMessage(error))
      } finally {
        setIsLoading(false)
      }
    }

    void loadProduct()
  }, [numericProductId])

  async function handleAddToCart() {
    if (!product) {
      return
    }

    if (!token) {
      navigate('/login', { state: { from: `/products/${product.id}` } })
      return
    }

    setIsSubmitting(true)
    setMessage('')
    setErrorMessage('')

    try {
      const responseMessage = await addCartItem({
        productId: product.id,
        quantity,
      })
      setMessage(responseMessage)
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  if (isLoading) {
    return <LoadingState title="Loading product detail..." />
  }

  if (!product) {
    return (
      <div className="stack-lg">
        <div className="message error">{errorMessage || 'Product not found'}</div>
        <Link className="button-outline" to="/catalog">
          Back to catalog
        </Link>
      </div>
    )
  }

  return (
    <div className="stack-lg">
      <section className="surface stack-lg">
        <SectionHeading
          description="Compare the offer, check availability, and add the right quantity to your cart."
          eyebrow="Product detail"
          title={product.name}
        />

        {message ? <div className="message">{message}</div> : null}
        {errorMessage ? <div className="message error">{errorMessage}</div> : null}

        <div className="detail-layout-market">
          <div className="detail-content-stack">
            <div className="detail-gallery-card">
              <div className="detail-figure" />
            </div>

            <div className="detail-copy-card">
              <div className="detail-badges">
                <span className="pill">{product.category}</span>
                {product.brand ? <span className="signal">{product.brand}</span> : null}
                {product.featured ? <span className="signal">Popular pick</span> : null}
              </div>

              <div className="detail-price-block">
                <h2 className="detail-title">{formatCurrency(product.price)}</h2>
                <span className="supporting-copy">
                  SKU {product.sku || 'Assigned at listing'} | Lead time {product.leadTimeDays} day(s)
                </span>
              </div>

              <p className="section-copy">{product.description}</p>

              <div className="detail-highlight-card">
                <p className="eyebrow">Offer details</p>
                <ul className="detail-spec-list">
                  <li>{product.stock} units currently available</li>
                  <li>Ships to supported Canadian addresses</li>
                  <li>Saved in your order history after checkout</li>
                </ul>
              </div>
            </div>
          </div>

          <aside className="buy-box">
            <div className="stack">
              <span className="eyebrow">Buy now</span>
              <h2 className="detail-title">{formatCurrency(product.price)}</h2>
              <span className="buy-box-note">
                {product.stock > 0
                  ? `${product.stock} in stock and ready to add to cart`
                  : 'Temporarily unavailable'}
              </span>
            </div>

            <div className="buy-box-row">
              <span className="supporting-copy">Quantity</span>
              <div className="field-inline">
                <input
                  id="quantity"
                  max={Math.max(product.stock, 1)}
                  min={1}
                  onChange={(event) => setQuantity(Number(event.target.value))}
                  type="number"
                  value={quantity}
                />
              </div>
            </div>

            <div className="buy-box-actions">
              <button
                className="button"
                disabled={isSubmitting || product.stock === 0}
                onClick={() => void handleAddToCart()}
                type="button"
              >
                {isSubmitting ? 'Adding...' : 'Add to cart'}
              </button>
              <Link className="button-outline" to="/catalog">
                Continue shopping
              </Link>
            </div>
          </aside>
        </div>
      </section>
    </div>
  )
}

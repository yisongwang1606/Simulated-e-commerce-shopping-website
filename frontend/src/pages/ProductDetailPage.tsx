import { useEffect, useEffectEvent, useState } from 'react'
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

  const loadProduct = useEffectEvent(async () => {
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
  })

  useEffect(() => {
    void loadProduct()
  }, [])

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
          description="This view reads from /api/products/{id} and sends cart mutations to the Redis-backed cart API."
          eyebrow="Product detail"
          title={product.name}
        />

        {message ? <div className="message">{message}</div> : null}
        {errorMessage ? <div className="message error">{errorMessage}</div> : null}

        <div className="detail-grid">
          <div className="stack-lg">
            <div className="detail-figure" />
            <div className="surface stack">
              <div className="detail-meta">
                <span className="pill">{product.category}</span>
                <span>{product.stock} units available</span>
              </div>
              <p className="section-copy">{product.description}</p>
            </div>
          </div>

          <aside className="detail-panel stack-lg">
            <div className="stack">
              <span className="eyebrow">Price</span>
              <h2 className="detail-title">{formatCurrency(product.price)}</h2>
              <p className="section-copy">
                Built to plug straight into the cart and order flow you already
                implemented in Spring Boot.
              </p>
            </div>

            <div className="field">
              <label htmlFor="quantity">Quantity</label>
              <input
                id="quantity"
                max={Math.max(product.stock, 1)}
                min={1}
                onChange={(event) => setQuantity(Number(event.target.value))}
                type="number"
                value={quantity}
              />
            </div>

            <div className="stack-row">
              <button
                className="button"
                disabled={isSubmitting || product.stock === 0}
                onClick={() => void handleAddToCart()}
                type="button"
              >
                {isSubmitting ? 'Adding...' : 'Add to cart'}
              </button>
              <Link className="button-outline" to="/catalog">
                Keep browsing
              </Link>
            </div>
          </aside>
        </div>
      </section>
    </div>
  )
}

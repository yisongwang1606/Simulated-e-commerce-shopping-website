import { useEffect, useEffectEvent, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'

import { getCart, removeCartItem, updateCartItem } from '../api/cart'
import { createOrder } from '../api/orders'
import type { Cart } from '../api/contracts'
import { extractErrorMessage } from '../shared/error'
import { formatCurrency } from '../shared/formatters'
import { EmptyState } from '../shared/ui/EmptyState'
import { LoadingState } from '../shared/ui/LoadingState'
import { SectionHeading } from '../shared/ui/SectionHeading'

export function CartPage() {
  const navigate = useNavigate()
  const [cart, setCart] = useState<Cart | null>(null)
  const [draftQuantities, setDraftQuantities] = useState<Record<number, number>>({})
  const [isLoading, setIsLoading] = useState(true)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [message, setMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')

  async function fetchCartData() {
    setIsLoading(true)
    setErrorMessage('')

    try {
      const data = await getCart()
      setCart(data)
      setDraftQuantities(
        Object.fromEntries(
          data.items.map((item) => [item.productId, item.quantity]),
        ),
      )
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }

  const loadCart = useEffectEvent(async () => {
    await fetchCartData()
  })

  useEffect(() => {
    void loadCart()
  }, [])

  async function handleUpdate(productId: number) {
    setIsSubmitting(true)
    setMessage('')
    setErrorMessage('')

    try {
      const responseMessage = await updateCartItem(productId, {
        quantity: draftQuantities[productId],
      })
      setMessage(responseMessage)
      await fetchCartData()
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleRemove(productId: number) {
    setIsSubmitting(true)
    setMessage('')
    setErrorMessage('')

    try {
      const responseMessage = await removeCartItem(productId)
      setMessage(responseMessage)
      await fetchCartData()
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  async function handleCheckout() {
    setIsSubmitting(true)
    setMessage('')
    setErrorMessage('')

    try {
      const order = await createOrder()
      setMessage(`Order #${order.id} created successfully.`)
      await fetchCartData()
      navigate('/orders')
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="stack-lg">
      <section className="surface stack-lg">
        <SectionHeading
          description="This screen is already wired to Redis cart endpoints, so quantity changes and checkout hit the real backend."
          eyebrow="Cart"
          title="Review and place the current order"
        />

        {message ? <div className="message">{message}</div> : null}
        {errorMessage ? <div className="message error">{errorMessage}</div> : null}

        {isLoading ? (
          <LoadingState title="Loading the Redis cart..." />
        ) : cart && cart.items.length > 0 ? (
          <div className="cart-layout">
            <div className="stack-lg">
              {cart.items.map((item) => (
                <article className="cart-item" key={item.productId}>
                  <div className="cart-item-main">
                    <div className="cart-copy">
                      <h3 className="card-title">{item.name}</h3>
                      <div className="item-meta">
                        <span className="pill">{item.category}</span>
                        <span>{formatCurrency(item.price)} each</span>
                      </div>
                    </div>

                    <div className="cart-side">
                      <div className="field cart-qty-field">
                        <label htmlFor={`qty-${item.productId}`}>Qty</label>
                        <input
                          id={`qty-${item.productId}`}
                          min={1}
                          onChange={(event) =>
                            setDraftQuantities((current) => ({
                              ...current,
                              [item.productId]: Number(event.target.value),
                            }))
                          }
                          type="number"
                          value={draftQuantities[item.productId] ?? item.quantity}
                        />
                      </div>
                      <div className="cart-price-block">
                        <span className="eyebrow">Subtotal</span>
                        <strong className="price">{formatCurrency(item.subtotal)}</strong>
                      </div>
                      <div className="stack-row">
                        <button
                          className="button-outline"
                          disabled={isSubmitting}
                          onClick={() => void handleUpdate(item.productId)}
                          type="button"
                        >
                          Update
                        </button>
                        <button
                          className="button-ghost"
                          disabled={isSubmitting}
                          onClick={() => void handleRemove(item.productId)}
                          type="button"
                        >
                          Remove
                        </button>
                      </div>
                    </div>
                  </div>
                </article>
              ))}
            </div>

            <aside className="detail-panel cart-summary cart-summary-sticky">
              <p className="eyebrow">Order summary</p>
              <div className="summary-row">
                <span>Total items</span>
                <strong>{cart.totalQuantity}</strong>
              </div>
              <div className="summary-row">
                <span>Total</span>
                <strong className="price">{formatCurrency(cart.totalPrice)}</strong>
              </div>
              <button
                className="button"
                disabled={isSubmitting}
                onClick={() => void handleCheckout()}
                type="button"
              >
                {isSubmitting ? 'Placing order...' : 'Create order'}
              </button>
              <Link className="button-outline" to="/catalog">
                Back to catalog
              </Link>
              <p className="supporting-copy">
                Quantities update immediately against Redis and checkout calls
                the order API.
              </p>
            </aside>
          </div>
        ) : (
          <EmptyState
            message="Add a product from the catalog first. This page is ready for real checkout once the cart has items."
            title="The cart is empty"
          />
        )}
      </section>
    </div>
  )
}

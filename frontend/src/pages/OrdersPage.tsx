import { useEffect, useEffectEvent, useState } from 'react'

import { getOrders } from '../api/orders'
import type { Order } from '../api/contracts'
import { extractErrorMessage } from '../shared/error'
import { formatCurrency, formatDate } from '../shared/formatters'
import { EmptyState } from '../shared/ui/EmptyState'
import { LoadingState } from '../shared/ui/LoadingState'
import { SectionHeading } from '../shared/ui/SectionHeading'
import { StatusPill } from '../shared/ui/StatusPill'

export function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')

  const loadOrders = useEffectEvent(async () => {
    setIsLoading(true)
    setErrorMessage('')

    try {
      const data = await getOrders()
      setOrders(data)
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  })

  useEffect(() => {
    void loadOrders()
  }, [])

  return (
    <div className="stack-lg">
      <section className="surface stack-lg">
        <SectionHeading
          description="Order history is rendered from /api/orders and uses the same JWT session managed by the login screen."
          eyebrow="Orders"
          title="Customer order history"
        />

        {errorMessage ? <div className="message error">{errorMessage}</div> : null}

        {isLoading ? (
          <LoadingState title="Loading your orders..." />
        ) : orders.length > 0 ? (
          <div className="order-grid">
            {orders.map((order) => (
              <article className="order-card" key={order.id}>
                <div className="summary-row">
                  <div className="stack">
                    <p className="eyebrow">Order #{order.id}</p>
                    <h3 className="card-title">{formatCurrency(order.totalPrice)}</h3>
                  </div>
                  <StatusPill value={order.status} />
                </div>

                <div className="order-meta">
                  <span>Placed {formatDate(order.createdAt)}</span>
                  <span>{order.items.length} line items</span>
                </div>

                <div className="stack">
                  {order.items.map((item) => (
                    <div className="summary-row" key={`${order.id}-${item.productId}`}>
                      <span>
                        {item.productName} × {item.quantity}
                      </span>
                      <strong>{formatCurrency(item.subtotal)}</strong>
                    </div>
                  ))}
                </div>
              </article>
            ))}
          </div>
        ) : (
          <EmptyState
            message="Create one order from the cart and it will show up here."
            title="No orders yet"
          />
        )}
      </section>
    </div>
  )
}

import { useEffect, useState } from 'react'

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

  useEffect(() => {
    const loadOrders = async () => {
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
    }

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
          <div className="order-list">
            {orders.map((order) => (
              <article className="order-card" key={order.id}>
                <div className="order-summary-panel">
                  <div className="stack">
                    <div className="order-summary-header">
                      <p className="eyebrow">Order #{order.id}</p>
                      <StatusPill value={order.status} />
                    </div>

                    <div className="stack">
                      <h3 className="card-title">{formatCurrency(order.totalPrice)}</h3>
                      <div className="order-meta-line">
                        <span className="signal">Placed {formatDate(order.createdAt)}</span>
                        <span className="signal">{order.items.length} items</span>
                      </div>
                    </div>
                  </div>
                </div>

                <div className="order-detail-panel">
                  {order.items.map((item) => (
                    <div className="order-item-row" key={`${order.id}-${item.productId}`}>
                      <div className="order-item-copy">
                        <span className="order-item-name">{item.productName}</span>
                        <span className="supporting-copy">Quantity {item.quantity}</span>
                      </div>
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

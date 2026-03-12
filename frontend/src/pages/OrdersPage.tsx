import { useCallback, useEffect, useState } from 'react'

import {
  createOrderSupportTicket,
  createRefundRequest,
  getOrderRefundRequests,
  getOrders,
  getOrderShipments,
  getOrderSupportTickets,
} from '../api/orders'
import type {
  Order,
  RefundRequest,
  Shipment,
  SupportTicket,
  SupportTicketInput,
} from '../api/contracts'
import { extractErrorMessage } from '../shared/error'
import { formatCurrency, formatDate, formatDateTime } from '../shared/formatters'
import { patchIndexedValue, setIndexedValue } from '../shared/state'
import { EmptyState } from '../shared/ui/EmptyState'
import { LoadingState } from '../shared/ui/LoadingState'
import { SectionHeading } from '../shared/ui/SectionHeading'
import { StatusPill } from '../shared/ui/StatusPill'

const refundableStatuses = new Set(['SHIPPED', 'COMPLETED'])
function createDefaultTicketDraft(): SupportTicketInput {
  return {
    category: 'DELIVERY',
    priority: 'MEDIUM',
    subject: '',
    customerMessage: '',
  }
}

export function OrdersPage() {
  const [orders, setOrders] = useState<Order[]>([])
  const [expandedOrderIds, setExpandedOrderIds] = useState<Record<number, boolean>>({})
  const [refundDrafts, setRefundDrafts] = useState<Record<number, string>>({})
  const [ticketDrafts, setTicketDrafts] = useState<
    Record<number, SupportTicketInput>
  >({})
  const [refundsByOrder, setRefundsByOrder] = useState<Record<number, RefundRequest[]>>(
    {},
  )
  const [shipmentsByOrder, setShipmentsByOrder] = useState<Record<number, Shipment[]>>(
    {},
  )
  const [supportTicketsByOrder, setSupportTicketsByOrder] = useState<
    Record<number, SupportTicket[]>
  >({})
  const [detailLoadingByOrder, setDetailLoadingByOrder] = useState<Record<number, boolean>>(
    {},
  )
  const [submittingRefundOrderId, setSubmittingRefundOrderId] = useState<number | null>(
    null,
  )
  const [submittingTicketOrderId, setSubmittingTicketOrderId] = useState<number | null>(
    null,
  )
  const [isLoading, setIsLoading] = useState(true)
  const [message, setMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')

  const loadOrders = useCallback(async () => {
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
  }, [])

  useEffect(() => {
    void loadOrders()
  }, [loadOrders])

  const loadOrderOperations = useCallback(async (orderId: number) => {
    setDetailLoadingByOrder((current) => setIndexedValue(current, orderId, true))

    try {
      const [refunds, shipments, supportTickets] = await Promise.all([
        getOrderRefundRequests(orderId),
        getOrderShipments(orderId),
        getOrderSupportTickets(orderId),
      ])

      setRefundsByOrder((current) => ({ ...current, [orderId]: refunds }))
      setShipmentsByOrder((current) => ({ ...current, [orderId]: shipments }))
      setSupportTicketsByOrder((current) => ({
        ...current,
        [orderId]: supportTickets,
      }))
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setDetailLoadingByOrder((current) => setIndexedValue(current, orderId, false))
    }
  }, [])

  function handleToggleOrder(orderId: number) {
    const shouldOpen = !expandedOrderIds[orderId]

    setExpandedOrderIds((current) => setIndexedValue(current, orderId, shouldOpen))

    if (
      shouldOpen &&
      refundsByOrder[orderId] === undefined &&
      shipmentsByOrder[orderId] === undefined &&
      supportTicketsByOrder[orderId] === undefined
    ) {
      void loadOrderOperations(orderId)
    }
  }

  async function handleRefundSubmit(orderId: number) {
    const reason = refundDrafts[orderId]?.trim()

    if (!reason) {
      setErrorMessage('Enter a refund reason before sending the request.')
      return
    }

    setSubmittingRefundOrderId(orderId)
    setMessage('')
    setErrorMessage('')

    try {
      const refundRequest = await createRefundRequest(orderId, { reason })
      setRefundDrafts((current) => setIndexedValue(current, orderId, ''))
      setMessage(`Refund request ${refundRequest.id} submitted for ${refundRequest.orderNo}.`)
      await loadOrderOperations(orderId)
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setSubmittingRefundOrderId(null)
    }
  }

  async function handleSupportTicketSubmit(orderId: number) {
    const ticketDraft = ticketDrafts[orderId] ?? createDefaultTicketDraft()
    if (!ticketDraft.subject.trim() || !ticketDraft.customerMessage.trim()) {
      setErrorMessage('Enter a ticket subject and message before submitting.')
      return
    }

    setSubmittingTicketOrderId(orderId)
    setMessage('')
    setErrorMessage('')

    try {
      const supportTicket = await createOrderSupportTicket(orderId, {
        ...ticketDraft,
        subject: ticketDraft.subject.trim(),
        customerMessage: ticketDraft.customerMessage.trim(),
      })
      setTicketDrafts((current) =>
        setIndexedValue(current, orderId, createDefaultTicketDraft()),
      )
      setMessage(`Support ticket ${supportTicket.ticketNo} created for ${supportTicket.orderNo}.`)
      await loadOrderOperations(orderId)
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setSubmittingTicketOrderId(null)
    }
  }

  function updateTicketDraft(
    orderId: number,
    patch: Partial<SupportTicketInput>,
  ) {
    setTicketDrafts((current) =>
      patchIndexedValue(current, orderId, createDefaultTicketDraft(), patch),
    )
  }

  function updateRefundDraft(orderId: number, value: string) {
    setRefundDrafts((current) => setIndexedValue(current, orderId, value))
  }

  return (
    <div className="stack-lg">
      <section className="surface stack-lg">
        <SectionHeading
          description="Customers can track delivery records, raise refund requests, and open service tickets from the same order workspace."
          eyebrow="Orders"
          title="Customer order history and after-sales desk"
        />

        {message ? <div className="message">{message}</div> : null}
        {errorMessage ? <div className="message error">{errorMessage}</div> : null}

        {isLoading ? (
          <LoadingState title="Loading your orders..." />
        ) : orders.length > 0 ? (
          <div className="order-list">
            {orders.map((order) => {
              const shipments = shipmentsByOrder[order.id] ?? []
              const refundRequests = refundsByOrder[order.id] ?? []
              const supportTickets = supportTicketsByOrder[order.id] ?? []
              const canRequestRefund =
                refundableStatuses.has(order.status) &&
                !refundRequests.some((request) => request.refundStatus !== 'REJECTED')
              const ticketDraft = ticketDrafts[order.id] ?? createDefaultTicketDraft()

              return (
                <article className="order-card enterprise-order-card" key={order.id}>
                  <div className="order-summary-panel">
                    <div className="stack">
                      <div className="order-summary-header">
                        <div className="stack">
                          <p className="eyebrow">Order {order.orderNo}</p>
                          <h3 className="card-title">{formatCurrency(order.totalPrice)}</h3>
                        </div>
                        <StatusPill value={order.status} />
                      </div>

                      <div className="order-meta-line">
                        <span className="signal">Placed {formatDate(order.createdAt)}</span>
                        <span className="signal">{order.items.length} line items</span>
                      </div>

                      {order.statusNote ? (
                        <p className="supporting-copy">Latest note: {order.statusNote}</p>
                      ) : null}

                      {order.shippingAddress ? (
                        <div className="info-card">
                          <p className="eyebrow">Delivery snapshot</p>
                          <strong>{order.shippingAddress.receiverName}</strong>
                          <span className="supporting-copy">
                            {order.shippingAddress.phone}
                          </span>
                          <span className="supporting-copy">
                            {order.shippingAddress.line1}
                            {order.shippingAddress.line2
                              ? `, ${order.shippingAddress.line2}`
                              : ''}
                          </span>
                          <span className="supporting-copy">
                            {order.shippingAddress.city}, {order.shippingAddress.province}{' '}
                            {order.shippingAddress.postalCode}
                          </span>
                        </div>
                      ) : null}

                      <button
                        className="button-outline"
                        onClick={() => handleToggleOrder(order.id)}
                        type="button"
                      >
                        {expandedOrderIds[order.id]
                          ? 'Hide service details'
                          : 'Show service details'}
                      </button>
                    </div>
                  </div>

                  <div className="order-detail-panel">
                    <div className="metric-grid order-metric-grid">
                      <div className="info-card">
                        <p className="eyebrow">Subtotal</p>
                        <strong>{formatCurrency(order.subtotalAmount)}</strong>
                      </div>
                      <div className="info-card">
                        <p className="eyebrow">Tax</p>
                        <strong>{formatCurrency(order.taxAmount)}</strong>
                      </div>
                      <div className="info-card">
                        <p className="eyebrow">Shipping</p>
                        <strong>{formatCurrency(order.shippingAmount)}</strong>
                      </div>
                    </div>

                    <div className="stack">
                      {order.items.map((item) => (
                        <div className="order-item-row" key={`${order.id}-${item.productId}`}>
                          <div className="order-item-copy">
                            <span className="order-item-name">{item.productName}</span>
                            <span className="supporting-copy">
                              {item.sku ? `${item.sku} · ` : ''}Quantity {item.quantity}
                            </span>
                          </div>
                          <strong>{formatCurrency(item.subtotal)}</strong>
                        </div>
                      ))}
                    </div>

                    {expandedOrderIds[order.id] ? (
                      detailLoadingByOrder[order.id] ? (
                        <LoadingState title="Loading service events..." />
                      ) : (
                        <div className="service-grid">
                          <section className="info-card stack">
                            <div className="toolbar">
                              <div>
                                <p className="eyebrow">Shipment records</p>
                                <h4 className="card-title">Fulfillment visibility</h4>
                              </div>
                              <span className="signal">{shipments.length} records</span>
                            </div>

                            {shipments.length > 0 ? (
                              <div className="stack">
                                {shipments.map((shipment) => (
                                  <div className="detail-row" key={shipment.id}>
                                    <div className="stack">
                                      <strong>{shipment.shipmentNo}</strong>
                                      <span className="supporting-copy">
                                        {shipment.carrierCode} · {shipment.trackingNo}
                                      </span>
                                      <span className="supporting-copy">
                                        Updated {formatDateTime(shipment.updatedAt)}
                                      </span>
                                    </div>
                                    <StatusPill value={shipment.shipmentStatus} />
                                  </div>
                                ))}
                              </div>
                            ) : (
                              <p className="supporting-copy">
                                No shipment record has been created yet.
                              </p>
                            )}
                          </section>

                          <section className="info-card stack">
                            <div className="toolbar">
                              <div>
                                <p className="eyebrow">Refund requests</p>
                                <h4 className="card-title">Finance and returns</h4>
                              </div>
                              <span className="signal">{refundRequests.length} requests</span>
                            </div>

                            {refundRequests.length > 0 ? (
                              <div className="stack">
                                {refundRequests.map((request) => (
                                  <div className="detail-row" key={request.id}>
                                    <div className="stack">
                                      <strong>Request #{request.id}</strong>
                                      <span className="supporting-copy">{request.reason}</span>
                                      <span className="supporting-copy">
                                        Submitted {formatDateTime(request.requestedAt)}
                                      </span>
                                      {request.reviewNote ? (
                                        <span className="supporting-copy">
                                          Review: {request.reviewNote}
                                        </span>
                                      ) : null}
                                    </div>
                                    <StatusPill value={request.refundStatus} />
                                  </div>
                                ))}
                              </div>
                            ) : (
                              <p className="supporting-copy">
                                No refund requests on this order yet.
                              </p>
                            )}

                            {canRequestRefund ? (
                              <div className="stack refund-form">
                                <div className="field">
                                  <label htmlFor={`refund-${order.id}`}>Refund reason</label>
                                  <textarea
                                    id={`refund-${order.id}`}
                                    onChange={(event) =>
                                      updateRefundDraft(order.id, event.target.value)
                                    }
                                    placeholder="Explain the issue for support and finance review."
                                    value={refundDrafts[order.id] ?? ''}
                                  />
                                </div>
                                <button
                                  className="button"
                                  disabled={submittingRefundOrderId === order.id}
                                  onClick={() => void handleRefundSubmit(order.id)}
                                  type="button"
                                >
                                  {submittingRefundOrderId === order.id
                                    ? 'Submitting refund...'
                                    : 'Request refund'}
                                </button>
                              </div>
                            ) : null}
                          </section>

                          <section className="info-card stack">
                            <div className="toolbar">
                              <div>
                                <p className="eyebrow">Support tickets</p>
                                <h4 className="card-title">Service desk</h4>
                              </div>
                              <span className="signal">{supportTickets.length} cases</span>
                            </div>

                            {supportTickets.length > 0 ? (
                              <div className="stack">
                                {supportTickets.map((supportTicket) => (
                                  <div className="detail-row" key={supportTicket.id}>
                                    <div className="stack">
                                      <strong>{supportTicket.ticketNo}</strong>
                                      <span className="supporting-copy">
                                        {supportTicket.subject}
                                      </span>
                                      <span className="supporting-copy">
                                        {supportTicket.category} · {supportTicket.priority}
                                      </span>
                                      {supportTicket.latestNote ? (
                                        <span className="supporting-copy">
                                          Latest note: {supportTicket.latestNote}
                                        </span>
                                      ) : null}
                                      {supportTicket.assignedTeam ? (
                                        <span className="supporting-copy">
                                          Assigned to {supportTicket.assignedTeam}
                                          {supportTicket.assignedToUsername
                                            ? ` · ${supportTicket.assignedToUsername}`
                                            : ''}
                                        </span>
                                      ) : null}
                                    </div>
                                    <StatusPill value={supportTicket.ticketStatus} />
                                  </div>
                                ))}
                              </div>
                            ) : (
                              <p className="supporting-copy">
                                No support tickets have been opened for this order.
                              </p>
                            )}

                            <div className="stack refund-form">
                              <div className="form-columns">
                                <div className="field">
                                  <label htmlFor={`ticket-category-${order.id}`}>Category</label>
                                  <select
                                    id={`ticket-category-${order.id}`}
                                    onChange={(event) =>
                                      updateTicketDraft(order.id, {
                                        category: event.target.value,
                                      })
                                    }
                                    value={ticketDraft.category}
                                  >
                                    <option value="DELIVERY">DELIVERY</option>
                                    <option value="DAMAGE">DAMAGE</option>
                                    <option value="BILLING">BILLING</option>
                                    <option value="RETURN">RETURN</option>
                                    <option value="OTHER">OTHER</option>
                                  </select>
                                </div>
                                <div className="field">
                                  <label htmlFor={`ticket-priority-${order.id}`}>Priority</label>
                                  <select
                                    id={`ticket-priority-${order.id}`}
                                    onChange={(event) =>
                                      updateTicketDraft(order.id, {
                                        priority: event.target.value as SupportTicketInput['priority'],
                                      })
                                    }
                                    value={ticketDraft.priority}
                                  >
                                    <option value="LOW">LOW</option>
                                    <option value="MEDIUM">MEDIUM</option>
                                    <option value="HIGH">HIGH</option>
                                    <option value="URGENT">URGENT</option>
                                  </select>
                                </div>
                              </div>

                              <div className="field">
                                <label htmlFor={`ticket-subject-${order.id}`}>Subject</label>
                                <input
                                  id={`ticket-subject-${order.id}`}
                                  onChange={(event) =>
                                    updateTicketDraft(order.id, {
                                      subject: event.target.value,
                                    })
                                  }
                                  placeholder="Summarize the issue for support."
                                  value={ticketDraft.subject}
                                />
                              </div>

                              <div className="field">
                                <label htmlFor={`ticket-message-${order.id}`}>Details</label>
                                <textarea
                                  id={`ticket-message-${order.id}`}
                                  onChange={(event) =>
                                    updateTicketDraft(order.id, {
                                      customerMessage: event.target.value,
                                    })
                                  }
                                  placeholder="Describe what happened and what you need next."
                                  value={ticketDraft.customerMessage}
                                />
                              </div>

                              <button
                                className="button"
                                disabled={submittingTicketOrderId === order.id}
                                onClick={() => void handleSupportTicketSubmit(order.id)}
                                type="button"
                              >
                                {submittingTicketOrderId === order.id
                                  ? 'Submitting case...'
                                  : 'Open support case'}
                              </button>
                            </div>
                          </section>
                        </div>
                      )
                    ) : null}
                  </div>
                </article>
              )
            })}
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

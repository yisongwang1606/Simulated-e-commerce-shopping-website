import { type FormEvent, useCallback, useEffect, useRef, useState } from 'react'

import {
  assignOrderTag,
  createProduct,
  getAdminRefundRequests,
  getAdminSupportTickets,
  getOrderTagCatalog,
  getRefundSummary,
  removeOrderTag,
  reviewRefundRequest,
  searchAdminOrders,
  updateSupportTicket,
} from '../api/admin'
import { getProducts } from '../api/products'
import type {
  Order,
  OrderTag,
  PagedResponse,
  Product,
  ProductPayload,
  RefundRequest,
  RefundSummary,
  SupportTicket,
  SupportTicketUpdateInput,
} from '../api/contracts'
import { extractErrorMessage } from '../shared/error'
import { formatCurrency, formatDate, formatDateTime } from '../shared/formatters'
import { LoadingState } from '../shared/ui/LoadingState'
import { SectionHeading } from '../shared/ui/SectionHeading'
import { StatusPill } from '../shared/ui/StatusPill'

const initialProductForm: ProductPayload = {
  sku: '',
  name: '',
  brand: '',
  price: 89.99,
  costPrice: 48.5,
  stock: 10,
  safetyStock: 3,
  category: 'Electronics',
  status: 'ACTIVE',
  taxClass: 'STANDARD',
  weightKg: 0.8,
  leadTimeDays: 3,
  featured: true,
  description: '',
}

const initialOrderFilters = {
  status: '',
  customer: '',
  dateFrom: '',
  dateTo: '',
}

const initialSupportFilters = {
  status: '',
  priority: '',
  assignedTeam: '',
}

export function AdminPage() {
  const [orderPage, setOrderPage] = useState<PagedResponse<Order> | null>(null)
  const [refundPage, setRefundPage] = useState<PagedResponse<RefundRequest> | null>(
    null,
  )
  const [supportTicketPage, setSupportTicketPage] = useState<
    PagedResponse<SupportTicket> | null
  >(null)
  const [refundSummary, setRefundSummary] = useState<RefundSummary | null>(null)
  const [orderTagCatalog, setOrderTagCatalog] = useState<OrderTag[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [form, setForm] = useState<ProductPayload>(initialProductForm)
  const [orderFilters, setOrderFilters] = useState(initialOrderFilters)
  const [supportFilters, setSupportFilters] = useState(initialSupportFilters)
  const [refundStatusFilter, setRefundStatusFilter] = useState('')
  const [refundReviewNotes, setRefundReviewNotes] = useState<Record<number, string>>(
    {},
  )
  const [selectedTagByOrder, setSelectedTagByOrder] = useState<Record<number, string>>(
    {},
  )
  const [supportTicketDrafts, setSupportTicketDrafts] = useState<
    Record<number, SupportTicketUpdateInput>
  >({})
  const [isLoading, setIsLoading] = useState(true)
  const [isRefreshing, setIsRefreshing] = useState(false)
  const [isSavingProduct, setIsSavingProduct] = useState(false)
  const [reviewingRefundId, setReviewingRefundId] = useState<number | null>(null)
  const [updatingTicketId, setUpdatingTicketId] = useState<number | null>(null)
  const [taggingOrderId, setTaggingOrderId] = useState<number | null>(null)
  const [message, setMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const orderFiltersRef = useRef(initialOrderFilters)
  const supportFiltersRef = useRef(initialSupportFilters)
  const refundStatusFilterRef = useRef('')
  const orderPageRef = useRef(0)
  const refundPageRef = useRef(0)
  const supportTicketPageRef = useRef(0)

  useEffect(() => {
    orderFiltersRef.current = orderFilters
  }, [orderFilters])

  useEffect(() => {
    supportFiltersRef.current = supportFilters
  }, [supportFilters])

  useEffect(() => {
    refundStatusFilterRef.current = refundStatusFilter
  }, [refundStatusFilter])

  useEffect(() => {
    orderPageRef.current = orderPage?.page ?? 0
  }, [orderPage])

  useEffect(() => {
    refundPageRef.current = refundPage?.page ?? 0
  }, [refundPage])

  useEffect(() => {
    supportTicketPageRef.current = supportTicketPage?.page ?? 0
  }, [supportTicketPage])

  const loadAdminData = useCallback(
    async (options?: {
      orderPage?: number
      refundPage?: number
      supportPage?: number
      filters?: typeof initialOrderFilters
      refundStatus?: string
      supportFilters?: typeof initialSupportFilters
      keepSkeleton?: boolean
    }) => {
      if (options?.keepSkeleton) {
        setIsLoading(true)
      } else {
        setIsRefreshing(true)
      }
      setErrorMessage('')

      try {
        const activeOrderFilters = options?.filters ?? orderFiltersRef.current
        const activeRefundStatus =
          options?.refundStatus ?? refundStatusFilterRef.current
        const activeSupportFilters =
          options?.supportFilters ?? supportFiltersRef.current

        const [ordersData, productData, refundData, summaryData, tagsData, supportData] =
          await Promise.all([
            searchAdminOrders({
              ...activeOrderFilters,
              page: options?.orderPage ?? orderPageRef.current,
              size: 8,
            }),
            getProducts({ page: 0, size: 6 }),
            getAdminRefundRequests({
              status: activeRefundStatus || undefined,
              page: options?.refundPage ?? refundPageRef.current,
              size: 6,
            }),
            getRefundSummary(activeOrderFilters.dateFrom, activeOrderFilters.dateTo),
            getOrderTagCatalog(),
            getAdminSupportTickets({
              ...activeSupportFilters,
              page: options?.supportPage ?? supportTicketPageRef.current,
              size: 6,
            }),
          ])

        setOrderPage(ordersData)
        setProducts(productData.items)
        setRefundPage(refundData)
        setRefundSummary(summaryData)
        setOrderTagCatalog(tagsData)
        setSupportTicketPage(supportData)
      } catch (error) {
        setErrorMessage(extractErrorMessage(error))
      } finally {
        setIsLoading(false)
        setIsRefreshing(false)
      }
    },
    [],
  )

  useEffect(() => {
    void loadAdminData({ keepSkeleton: true })
  }, [loadAdminData])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsSavingProduct(true)
    setMessage('')
    setErrorMessage('')

    try {
      const createdProduct = await createProduct(form)
      setMessage(`Created product ${createdProduct.sku} successfully.`)
      setForm(initialProductForm)
      await loadAdminData()
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setIsSavingProduct(false)
    }
  }

  async function handleRefundDecision(
    refundRequestId: number,
    decision: 'APPROVED' | 'REJECTED',
  ) {
    setReviewingRefundId(refundRequestId)
    setMessage('')
    setErrorMessage('')

    try {
      const reviewedRefund = await reviewRefundRequest(refundRequestId, {
        decision,
        reviewNote: refundReviewNotes[refundRequestId]?.trim() || undefined,
      })
      setMessage(
        `Refund request ${reviewedRefund.id} marked as ${reviewedRefund.refundStatus}.`,
      )
      await loadAdminData()
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setReviewingRefundId(null)
    }
  }

  async function handleAssignTag(orderId: number) {
    const selectedTagId = Number(selectedTagByOrder[orderId])

    if (!selectedTagId) {
      setErrorMessage('Select an operational tag before assigning it.')
      return
    }

    setTaggingOrderId(orderId)
    setMessage('')
    setErrorMessage('')

    try {
      await assignOrderTag(orderId, selectedTagId)
      setMessage(`Order tag assigned to order ${orderId}.`)
      await loadAdminData()
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setTaggingOrderId(null)
    }
  }

  async function handleRemoveTag(orderId: number, orderTagId: number) {
    setTaggingOrderId(orderId)
    setMessage('')
    setErrorMessage('')

    try {
      await removeOrderTag(orderId, orderTagId)
      setMessage(`Order tag removed from order ${orderId}.`)
      await loadAdminData()
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setTaggingOrderId(null)
    }
  }

  function updateSupportDraft(
    ticket: SupportTicket,
    patch: Partial<SupportTicketUpdateInput>,
  ) {
    const baseDraft: SupportTicketUpdateInput = {
      status: ticket.ticketStatus as SupportTicketUpdateInput['status'],
      assignedTeam: ticket.assignedTeam ?? '',
      assignedToUsername: ticket.assignedToUsername ?? '',
      latestNote: ticket.latestNote ?? '',
      resolutionNote: ticket.resolutionNote ?? '',
    }

    setSupportTicketDrafts((current) => ({
      ...current,
      [ticket.id]: {
        ...baseDraft,
        ...current[ticket.id],
        ...patch,
      },
    }))
  }

  async function handleSupportTicketUpdate(ticket: SupportTicket) {
    const payload = supportTicketDrafts[ticket.id] ?? {
      status: ticket.ticketStatus as SupportTicketUpdateInput['status'],
      assignedTeam: ticket.assignedTeam ?? '',
      assignedToUsername: ticket.assignedToUsername ?? '',
      latestNote: ticket.latestNote ?? '',
      resolutionNote: ticket.resolutionNote ?? '',
    }

    setUpdatingTicketId(ticket.id)
    setMessage('')
    setErrorMessage('')

    try {
      const updatedTicket = await updateSupportTicket(ticket.id, payload)
      setMessage(`Support ticket ${updatedTicket.ticketNo} updated successfully.`)
      await loadAdminData()
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setUpdatingTicketId(null)
    }
  }

  function renderPageOptions(totalPages: number) {
    return Array.from({ length: totalPages }, (_, index) => (
      <option key={index} value={index}>
        {index + 1}
      </option>
    ))
  }

  function renderProductSection() {
    return (
      <div className="surface stack-lg">
        <SectionHeading
          description="Create products against the enterprise payload, including SKU, merchandising flags, and operational thresholds."
          eyebrow="Product intake"
          title="Add a sellable catalog item"
        />

        <form className="form-grid compact" onSubmit={(event) => void handleSubmit(event)}>
          <div className="form-columns">
            <div className="field">
              <label htmlFor="product-sku">SKU</label>
              <input
                id="product-sku"
                onChange={(event) =>
                  setForm((current) => ({ ...current, sku: event.target.value }))
                }
                value={form.sku}
              />
            </div>
            <div className="field">
              <label htmlFor="product-name">Name</label>
              <input
                id="product-name"
                onChange={(event) =>
                  setForm((current) => ({ ...current, name: event.target.value }))
                }
                required
                value={form.name}
              />
            </div>
            <div className="field">
              <label htmlFor="product-brand">Brand</label>
              <input
                id="product-brand"
                onChange={(event) =>
                  setForm((current) => ({ ...current, brand: event.target.value }))
                }
                value={form.brand}
              />
            </div>
            <div className="field">
              <label htmlFor="product-category">Category</label>
              <input
                id="product-category"
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    category: event.target.value,
                  }))
                }
                required
                value={form.category}
              />
            </div>
            <div className="field">
              <label htmlFor="product-price">Price</label>
              <input
                id="product-price"
                min={0.01}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    price: Number(event.target.value),
                  }))
                }
                required
                step="0.01"
                type="number"
                value={form.price}
              />
            </div>
            <div className="field">
              <label htmlFor="product-cost-price">Cost price</label>
              <input
                id="product-cost-price"
                min={0}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    costPrice: Number(event.target.value),
                  }))
                }
                step="0.01"
                type="number"
                value={form.costPrice ?? 0}
              />
            </div>
            <div className="field">
              <label htmlFor="product-stock">Stock</label>
              <input
                id="product-stock"
                min={0}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    stock: Number(event.target.value),
                  }))
                }
                required
                type="number"
                value={form.stock}
              />
            </div>
            <div className="field">
              <label htmlFor="product-safety-stock">Safety stock</label>
              <input
                id="product-safety-stock"
                min={0}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    safetyStock: Number(event.target.value),
                  }))
                }
                type="number"
                value={form.safetyStock ?? 0}
              />
            </div>
            <div className="field">
              <label htmlFor="product-weight">Weight (kg)</label>
              <input
                id="product-weight"
                min={0}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    weightKg: Number(event.target.value),
                  }))
                }
                step="0.01"
                type="number"
                value={form.weightKg ?? 0}
              />
            </div>
            <div className="field">
              <label htmlFor="product-lead-time">Lead time (days)</label>
              <input
                id="product-lead-time"
                min={0}
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    leadTimeDays: Number(event.target.value),
                  }))
                }
                type="number"
                value={form.leadTimeDays ?? 0}
              />
            </div>
            <div className="field">
              <label htmlFor="product-status">Status</label>
              <select
                id="product-status"
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    status: event.target.value,
                  }))
                }
                value={form.status}
              >
                <option value="ACTIVE">ACTIVE</option>
                <option value="INACTIVE">INACTIVE</option>
              </select>
            </div>
            <div className="field">
              <label htmlFor="product-tax-class">Tax class</label>
              <select
                id="product-tax-class"
                onChange={(event) =>
                  setForm((current) => ({
                    ...current,
                    taxClass: event.target.value,
                  }))
                }
                value={form.taxClass}
              >
                <option value="STANDARD">STANDARD</option>
                <option value="ZERO_RATED">ZERO_RATED</option>
              </select>
            </div>
          </div>

          <div className="field">
            <label htmlFor="product-description">Description</label>
            <textarea
              id="product-description"
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  description: event.target.value,
                }))
              }
              required
              value={form.description}
            />
          </div>

          <label className="checkbox-row" htmlFor="product-featured">
            <input
              checked={form.featured ?? false}
              id="product-featured"
              onChange={(event) =>
                setForm((current) => ({
                  ...current,
                  featured: event.target.checked,
                }))
              }
              type="checkbox"
            />
            <span>Feature this product on merchandising surfaces</span>
          </label>

          <div className="card-actions">
            <button className="button" disabled={isSavingProduct} type="submit">
              {isSavingProduct ? 'Creating...' : 'Create product'}
            </button>
            <span className="signal">Admin JWT required</span>
          </div>
        </form>

        <div className="stack">
          <div className="toolbar">
            <div>
              <p className="eyebrow">Recent catalog sample</p>
              <h3 className="card-title">Freshly searchable products</h3>
            </div>
            {isRefreshing ? <span className="signal">Refreshing...</span> : null}
          </div>

          <div className="product-grid">
            {products.map((product) => (
              <article className="product-card" key={product.id}>
                <div className="stack">
                  <div className="item-meta">
                    <span className="pill">{product.category}</span>
                    <StatusPill value={product.status} />
                  </div>
                  <h3 className="product-title">{product.name}</h3>
                  <p className="section-copy">
                    {product.sku}
                    {product.brand ? ` · ${product.brand}` : ''}
                  </p>
                  <p className="section-copy">{product.description}</p>
                </div>
                <div className="summary-row">
                  <span>
                    {product.stock} in stock · safety {product.safetyStock}
                  </span>
                  <strong>{formatCurrency(product.price)}</strong>
                </div>
              </article>
            ))}
          </div>
        </div>
      </div>
    )
  }

  function renderOrderDesk() {
    return (
      <div className="table-card stack-lg">
        <SectionHeading
          description="Filter by customer, lifecycle status, and created date to find operational exceptions quickly, then tag the matching orders."
          eyebrow="Order desk"
          title="Search and triage the order book"
        />

        <div className="form-columns">
          <div className="field">
            <label htmlFor="order-customer">Customer or order</label>
            <input
              id="order-customer"
              onChange={(event) =>
                setOrderFilters((current) => ({
                  ...current,
                  customer: event.target.value,
                }))
              }
              placeholder="Order no, username, or email"
              value={orderFilters.customer}
            />
          </div>
          <div className="field">
            <label htmlFor="order-status-filter">Status</label>
            <select
              id="order-status-filter"
              onChange={(event) =>
                setOrderFilters((current) => ({
                  ...current,
                  status: event.target.value,
                }))
              }
              value={orderFilters.status}
            >
              <option value="">All statuses</option>
              <option value="CREATED">CREATED</option>
              <option value="PAYMENT_PENDING">PAYMENT_PENDING</option>
              <option value="PAID">PAID</option>
              <option value="ALLOCATED">ALLOCATED</option>
              <option value="SHIPPED">SHIPPED</option>
              <option value="COMPLETED">COMPLETED</option>
              <option value="REFUND_PENDING">REFUND_PENDING</option>
              <option value="REFUNDED">REFUNDED</option>
              <option value="CANCELLED">CANCELLED</option>
            </select>
          </div>
          <div className="field">
            <label htmlFor="order-date-from">Date from</label>
            <input
              id="order-date-from"
              onChange={(event) =>
                setOrderFilters((current) => ({
                  ...current,
                  dateFrom: event.target.value,
                }))
              }
              type="date"
              value={orderFilters.dateFrom}
            />
          </div>
          <div className="field">
            <label htmlFor="order-date-to">Date to</label>
            <input
              id="order-date-to"
              onChange={(event) =>
                setOrderFilters((current) => ({
                  ...current,
                  dateTo: event.target.value,
                }))
              }
              type="date"
              value={orderFilters.dateTo}
            />
          </div>
        </div>

        <div className="toolbar">
          <div className="stack-row">
            <button
              className="button"
              onClick={() => void loadAdminData({ orderPage: 0 })}
              type="button"
            >
              Apply filters
            </button>
            <button
              className="button-ghost"
              onClick={() => {
                setOrderFilters(initialOrderFilters)
                void loadAdminData({
                  orderPage: 0,
                  filters: initialOrderFilters,
                })
              }}
              type="button"
            >
              Reset
            </button>
          </div>
          <span className="signal">{orderPage?.totalElements ?? 0} matching orders</span>
        </div>

        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Order</th>
                <th>Customer</th>
                <th>Status</th>
                <th>Total</th>
                <th>Tags</th>
                <th>Placed</th>
              </tr>
            </thead>
            <tbody>
              {orderPage?.items.map((order) => (
                <tr key={order.id}>
                  <td>
                    <strong>{order.orderNo}</strong>
                  </td>
                  <td>
                    <div className="stack">
                      <span>{order.username}</span>
                      <span className="supporting-copy">
                        {order.shippingAddress?.city ?? 'No address'}
                      </span>
                    </div>
                  </td>
                  <td>
                    <StatusPill value={order.status} />
                  </td>
                  <td>{formatCurrency(order.totalPrice)}</td>
                  <td>
                    <div className="stack">
                      <div className="chip-row">
                        {order.tags.map((tag) => (
                          <button
                            className={`tag-chip ${tag.tone}`}
                            key={tag.id}
                            onClick={() => void handleRemoveTag(order.id, tag.id)}
                            type="button"
                          >
                            {tag.displayName}
                          </button>
                        ))}
                      </div>
                      <div className="stack-row">
                        <select
                          onChange={(event) =>
                            setSelectedTagByOrder((current) => ({
                              ...current,
                              [order.id]: event.target.value,
                            }))
                          }
                          value={selectedTagByOrder[order.id] ?? ''}
                        >
                          <option value="">Assign tag</option>
                          {orderTagCatalog.map((tag) => (
                            <option key={tag.id} value={tag.id}>
                              {tag.displayName}
                            </option>
                          ))}
                        </select>
                        <button
                          className="button-outline"
                          disabled={taggingOrderId === order.id}
                          onClick={() => void handleAssignTag(order.id)}
                          type="button"
                        >
                          Add
                        </button>
                      </div>
                    </div>
                  </td>
                  <td>{formatDate(order.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        {orderPage && orderPage.totalPages > 0 ? (
          <div className="toolbar">
            <button
              className="button-outline"
              disabled={orderPage.page === 0 || isRefreshing}
              onClick={() =>
                void loadAdminData({ orderPage: Math.max(orderPage.page - 1, 0) })
              }
              type="button"
            >
              Previous
            </button>
            <div className="page-jump">
              <label htmlFor="admin-order-page">Page</label>
              <select
                id="admin-order-page"
                onChange={(event) =>
                  void loadAdminData({
                    orderPage: Number(event.target.value),
                  })
                }
                value={orderPage.page}
              >
                {renderPageOptions(orderPage.totalPages)}
              </select>
            </div>
            <button
              className="button-outline"
              disabled={orderPage.page >= orderPage.totalPages - 1 || isRefreshing}
              onClick={() =>
                void loadAdminData({
                  orderPage: Math.min(orderPage.page + 1, orderPage.totalPages - 1),
                })
              }
              type="button"
            >
              Next
            </button>
          </div>
        ) : null}
      </div>
    )
  }

  function renderRefundDesk() {
    return (
      <div className="table-card stack-lg">
        <SectionHeading
          description="Approve or reject customer refund requests without leaving the admin console."
          eyebrow="Refund desk"
          title="Review pending refund cases"
        />

        <div className="toolbar">
          <div className="field field-inline">
            <label htmlFor="refund-status-filter">Status</label>
            <select
              id="refund-status-filter"
              onChange={(event) => {
                setRefundStatusFilter(event.target.value)
                void loadAdminData({
                  refundPage: 0,
                  refundStatus: event.target.value,
                })
              }}
              value={refundStatusFilter}
            >
              <option value="">All refunds</option>
              <option value="REQUESTED">REQUESTED</option>
              <option value="APPROVED">APPROVED</option>
              <option value="REJECTED">REJECTED</option>
              <option value="SETTLED">SETTLED</option>
            </select>
          </div>
          <span className="signal">{refundPage?.totalElements ?? 0} refund cases</span>
        </div>

        <div className="stack">
          {refundPage?.items.map((refund) => (
            <article className="info-card stack" key={refund.id}>
              <div className="order-summary-header">
                <div className="stack">
                  <strong>{refund.orderNo}</strong>
                  <span className="supporting-copy">
                    Requested by {refund.requestedByUsername} on{' '}
                    {formatDateTime(refund.requestedAt)}
                  </span>
                </div>
                <StatusPill value={refund.refundStatus} />
              </div>

              <p className="section-copy">{refund.reason}</p>

              {refund.reviewNote ? (
                <p className="supporting-copy">
                  Latest review note: {refund.reviewNote}
                </p>
              ) : null}

              {refund.refundStatus === 'REQUESTED' ? (
                <>
                  <div className="field">
                    <label htmlFor={`refund-review-${refund.id}`}>Review note</label>
                    <textarea
                      id={`refund-review-${refund.id}`}
                      onChange={(event) =>
                        setRefundReviewNotes((current) => ({
                          ...current,
                          [refund.id]: event.target.value,
                        }))
                      }
                      placeholder="Record the review outcome for finance and support."
                      value={refundReviewNotes[refund.id] ?? ''}
                    />
                  </div>
                  <div className="stack-row">
                    <button
                      className="button"
                      disabled={reviewingRefundId === refund.id}
                      onClick={() => void handleRefundDecision(refund.id, 'APPROVED')}
                      type="button"
                    >
                      {reviewingRefundId === refund.id ? 'Saving...' : 'Approve'}
                    </button>
                    <button
                      className="button-ghost"
                      disabled={reviewingRefundId === refund.id}
                      onClick={() => void handleRefundDecision(refund.id, 'REJECTED')}
                      type="button"
                    >
                      Reject
                    </button>
                  </div>
                </>
              ) : (
                <span className="supporting-copy">
                  Reviewed by {refund.reviewedByUsername ?? 'system'}
                  {refund.reviewedAt ? ` on ${formatDateTime(refund.reviewedAt)}` : ''}
                </span>
              )}
            </article>
          ))}
        </div>

        {refundPage && refundPage.totalPages > 0 ? (
          <div className="toolbar">
            <button
              className="button-outline"
              disabled={refundPage.page === 0 || isRefreshing}
              onClick={() =>
                void loadAdminData({
                  refundPage: Math.max(refundPage.page - 1, 0),
                })
              }
              type="button"
            >
              Previous
            </button>
            <div className="page-jump">
              <label htmlFor="admin-refund-page">Page</label>
              <select
                id="admin-refund-page"
                onChange={(event) =>
                  void loadAdminData({
                    refundPage: Number(event.target.value),
                  })
                }
                value={refundPage.page}
              >
                {renderPageOptions(refundPage.totalPages)}
              </select>
            </div>
            <button
              className="button-outline"
              disabled={refundPage.page >= refundPage.totalPages - 1 || isRefreshing}
              onClick={() =>
                void loadAdminData({
                  refundPage: Math.min(refundPage.page + 1, refundPage.totalPages - 1),
                })
              }
              type="button"
            >
              Next
            </button>
          </div>
        ) : null}
      </div>
    )
  }

  function renderSupportDesk() {
    return (
      <div className="table-card stack-lg">
        <SectionHeading
          description="This queue is for service operations: assignment, escalation, and resolution notes."
          eyebrow="Support desk"
          title="Customer support tickets"
        />

        <div className="form-columns">
          <div className="field">
            <label htmlFor="support-status">Status</label>
            <select
              id="support-status"
              onChange={(event) =>
                setSupportFilters((current) => ({
                  ...current,
                  status: event.target.value,
                }))
              }
              value={supportFilters.status}
            >
              <option value="">All statuses</option>
              <option value="OPEN">OPEN</option>
              <option value="IN_PROGRESS">IN_PROGRESS</option>
              <option value="WAITING_ON_CUSTOMER">WAITING_ON_CUSTOMER</option>
              <option value="RESOLVED">RESOLVED</option>
              <option value="CLOSED">CLOSED</option>
            </select>
          </div>
          <div className="field">
            <label htmlFor="support-priority">Priority</label>
            <select
              id="support-priority"
              onChange={(event) =>
                setSupportFilters((current) => ({
                  ...current,
                  priority: event.target.value,
                }))
              }
              value={supportFilters.priority}
            >
              <option value="">All priorities</option>
              <option value="LOW">LOW</option>
              <option value="MEDIUM">MEDIUM</option>
              <option value="HIGH">HIGH</option>
              <option value="URGENT">URGENT</option>
            </select>
          </div>
          <div className="field">
            <label htmlFor="support-team">Assigned team</label>
            <input
              id="support-team"
              onChange={(event) =>
                setSupportFilters((current) => ({
                  ...current,
                  assignedTeam: event.target.value,
                }))
              }
              placeholder="Customer Support"
              value={supportFilters.assignedTeam}
            />
          </div>
          <div className="field">
            <label htmlFor="support-apply">Refresh queue</label>
            <button
              className="button-outline"
              id="support-apply"
              onClick={() => void loadAdminData({ supportPage: 0 })}
              type="button"
            >
              Apply support filters
            </button>
          </div>
        </div>

        <div className="stack">
          {supportTicketPage?.items.map((ticket) => {
            const ticketDraft = supportTicketDrafts[ticket.id] ?? {
              status: ticket.ticketStatus as SupportTicketUpdateInput['status'],
              assignedTeam: ticket.assignedTeam ?? '',
              assignedToUsername: ticket.assignedToUsername ?? '',
              latestNote: ticket.latestNote ?? '',
              resolutionNote: ticket.resolutionNote ?? '',
            }

            return (
              <article className="info-card stack" key={ticket.id}>
                <div className="order-summary-header">
                  <div className="stack">
                    <strong>{ticket.ticketNo}</strong>
                    <span className="supporting-copy">
                      {ticket.orderNo} · {ticket.requestedByUsername}
                    </span>
                  </div>
                  <div className="stack-row">
                    <StatusPill value={ticket.ticketStatus} />
                    <StatusPill value={ticket.priority} />
                  </div>
                </div>

                <p className="section-copy">{ticket.subject}</p>
                <p className="supporting-copy">{ticket.customerMessage}</p>

                <div className="form-columns">
                  <div className="field">
                    <label htmlFor={`ticket-status-${ticket.id}`}>Status</label>
                    <select
                      id={`ticket-status-${ticket.id}`}
                      onChange={(event) =>
                        updateSupportDraft(ticket, {
                          status: event.target.value as SupportTicketUpdateInput['status'],
                        })
                      }
                      value={ticketDraft.status}
                    >
                      <option value="OPEN">OPEN</option>
                      <option value="IN_PROGRESS">IN_PROGRESS</option>
                      <option value="WAITING_ON_CUSTOMER">WAITING_ON_CUSTOMER</option>
                      <option value="RESOLVED">RESOLVED</option>
                      <option value="CLOSED">CLOSED</option>
                    </select>
                  </div>
                  <div className="field">
                    <label htmlFor={`ticket-team-${ticket.id}`}>Assigned team</label>
                    <input
                      id={`ticket-team-${ticket.id}`}
                      onChange={(event) =>
                        updateSupportDraft(ticket, {
                          assignedTeam: event.target.value,
                        })
                      }
                      value={ticketDraft.assignedTeam ?? ''}
                    />
                  </div>
                  <div className="field">
                    <label htmlFor={`ticket-agent-${ticket.id}`}>Assignee</label>
                    <input
                      id={`ticket-agent-${ticket.id}`}
                      onChange={(event) =>
                        updateSupportDraft(ticket, {
                          assignedToUsername: event.target.value,
                        })
                      }
                      value={ticketDraft.assignedToUsername ?? ''}
                    />
                  </div>
                  <div className="field">
                    <label htmlFor={`ticket-latest-note-${ticket.id}`}>Latest note</label>
                    <input
                      id={`ticket-latest-note-${ticket.id}`}
                      onChange={(event) =>
                        updateSupportDraft(ticket, {
                          latestNote: event.target.value,
                        })
                      }
                      value={ticketDraft.latestNote ?? ''}
                    />
                  </div>
                </div>

                <div className="field">
                  <label htmlFor={`ticket-resolution-${ticket.id}`}>Resolution note</label>
                  <textarea
                    id={`ticket-resolution-${ticket.id}`}
                    onChange={(event) =>
                      updateSupportDraft(ticket, {
                        resolutionNote: event.target.value,
                      })
                    }
                    value={ticketDraft.resolutionNote ?? ''}
                  />
                </div>

                <div className="card-actions">
                  <button
                    className="button"
                    disabled={updatingTicketId === ticket.id}
                    onClick={() => void handleSupportTicketUpdate(ticket)}
                    type="button"
                  >
                    {updatingTicketId === ticket.id ? 'Saving...' : 'Update ticket'}
                  </button>
                  <span className="supporting-copy">
                    Last changed {formatDateTime(ticket.updatedAt)}
                  </span>
                </div>
              </article>
            )
          })}
        </div>

        {supportTicketPage && supportTicketPage.totalPages > 0 ? (
          <div className="toolbar">
            <button
              className="button-outline"
              disabled={supportTicketPage.page === 0 || isRefreshing}
              onClick={() =>
                void loadAdminData({
                  supportPage: Math.max(supportTicketPage.page - 1, 0),
                })
              }
              type="button"
            >
              Previous
            </button>
            <div className="page-jump">
              <label htmlFor="admin-support-page">Page</label>
              <select
                id="admin-support-page"
                onChange={(event) =>
                  void loadAdminData({
                    supportPage: Number(event.target.value),
                  })
                }
                value={supportTicketPage.page}
              >
                {renderPageOptions(supportTicketPage.totalPages)}
              </select>
            </div>
            <button
              className="button-outline"
              disabled={
                supportTicketPage.page >= supportTicketPage.totalPages - 1 ||
                isRefreshing
              }
              onClick={() =>
                void loadAdminData({
                  supportPage: Math.min(
                    supportTicketPage.page + 1,
                    supportTicketPage.totalPages - 1,
                  ),
                })
              }
              type="button"
            >
              Next
            </button>
          </div>
        ) : null}
      </div>
    )
  }

  if (isLoading) {
    return <LoadingState title="Loading admin control data..." />
  }

  return (
    <div className="stack-lg">
      <section className="surface stack-lg">
        <SectionHeading
          description="This admin console now handles enterprise-style catalog intake, order triage, refund analytics, and support ticket operations."
          eyebrow="Admin"
          title="Commerce operations control tower"
        />

        {message ? <div className="message">{message}</div> : null}
        {errorMessage ? <div className="message error">{errorMessage}</div> : null}

        {refundSummary ? (
          <div className="metric-grid">
            <article className="stat-card">
              <p className="eyebrow">Refund pipeline</p>
              <strong>{refundSummary.totalRequests}</strong>
              <span className="supporting-copy">Total refund requests</span>
            </article>
            <article className="stat-card">
              <p className="eyebrow">Open workload</p>
              <strong>{refundSummary.requestedCount + refundSummary.approvedCount}</strong>
              <span className="supporting-copy">
                Requested or approved refunds still in flight
              </span>
            </article>
            <article className="stat-card">
              <p className="eyebrow">Requested amount</p>
              <strong>{formatCurrency(refundSummary.requestedAmount)}</strong>
              <span className="supporting-copy">Customer claims awaiting settlement</span>
            </article>
            <article className="stat-card">
              <p className="eyebrow">Settled amount</p>
              <strong>{formatCurrency(refundSummary.settledAmount)}</strong>
              <span className="supporting-copy">Confirmed refunds already settled</span>
            </article>
          </div>
        ) : null}

        <div className="admin-grid">
          {renderProductSection()}
          <div className="stack-lg">
            {renderOrderDesk()}
            {renderRefundDesk()}
            {renderSupportDesk()}
          </div>
        </div>
      </section>
    </div>
  )
}

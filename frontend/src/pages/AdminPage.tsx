import { type FormEvent, useCallback, useEffect, useRef, useState } from 'react'

import {
  assignOrderTag,
  createProduct,
  getAdminDashboardSummary,
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
  AdminDashboardSummary,
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
import {
  formatCurrency,
  formatDate,
  formatDateTime,
  formatInteger,
} from '../shared/formatters'
import { patchIndexedValue, setIndexedValue, setObjectField } from '../shared/state'
import { LoadingState } from '../shared/ui/LoadingState'
import { PaginationControls } from '../shared/ui/PaginationControls'
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

type OrderFilters = typeof initialOrderFilters
type SupportFilters = typeof initialSupportFilters

function buildSupportTicketDraft(ticket: SupportTicket): SupportTicketUpdateInput {
  return {
    status: ticket.ticketStatus as SupportTicketUpdateInput['status'],
    assignedTeam: ticket.assignedTeam ?? '',
    assignedToUsername: ticket.assignedToUsername ?? '',
    latestNote: ticket.latestNote ?? '',
    resolutionNote: ticket.resolutionNote ?? '',
  }
}

function resolveBreakdownWidth(count: number, metrics: Array<{ count: number }>): string {
  const max = Math.max(...metrics.map((entry) => entry.count), 1)
  return `${Math.max(16, Math.round((count / max) * 100))}%`
}

export function AdminPage() {
  const [orderPage, setOrderPage] = useState<PagedResponse<Order> | null>(null)
  const [refundPage, setRefundPage] = useState<PagedResponse<RefundRequest> | null>(
    null,
  )
  const [supportTicketPage, setSupportTicketPage] = useState<
    PagedResponse<SupportTicket> | null
  >(null)
  const [dashboardSummary, setDashboardSummary] =
    useState<AdminDashboardSummary | null>(null)
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

        const [
          dashboardData,
          ordersData,
          productData,
          refundData,
          summaryData,
          tagsData,
          supportData,
        ] =
          await Promise.all([
            getAdminDashboardSummary(),
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

        setDashboardSummary(dashboardData)
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

  function updateProductField<K extends keyof ProductPayload>(
    field: K,
    value: ProductPayload[K],
  ) {
    setForm((current) => setObjectField(current, field, value))
  }

  function updateOrderFilter<K extends keyof OrderFilters>(
    field: K,
    value: OrderFilters[K],
  ) {
    setOrderFilters((current) => setObjectField(current, field, value))
  }

  function updateSupportFilter<K extends keyof SupportFilters>(
    field: K,
    value: SupportFilters[K],
  ) {
    setSupportFilters((current) => setObjectField(current, field, value))
  }

  function updateRefundReviewNote(refundRequestId: number, note: string) {
    setRefundReviewNotes((current) => setIndexedValue(current, refundRequestId, note))
  }

  function updateSelectedTag(orderId: number, tagId: string) {
    setSelectedTagByOrder((current) => setIndexedValue(current, orderId, tagId))
  }

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
    setSupportTicketDrafts((current) =>
      patchIndexedValue(current, ticket.id, buildSupportTicketDraft(ticket), patch),
    )
  }

  async function handleSupportTicketUpdate(ticket: SupportTicket) {
    const payload = supportTicketDrafts[ticket.id] ?? buildSupportTicketDraft(ticket)

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
                onChange={(event) => updateProductField('sku', event.target.value)}
                value={form.sku}
              />
            </div>
            <div className="field">
              <label htmlFor="product-name">Name</label>
              <input
                id="product-name"
                onChange={(event) => updateProductField('name', event.target.value)}
                required
                value={form.name}
              />
            </div>
            <div className="field">
              <label htmlFor="product-brand">Brand</label>
              <input
                id="product-brand"
                onChange={(event) => updateProductField('brand', event.target.value)}
                value={form.brand}
              />
            </div>
            <div className="field">
              <label htmlFor="product-category">Category</label>
              <input
                id="product-category"
                onChange={(event) => updateProductField('category', event.target.value)}
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
                  updateProductField('price', Number(event.target.value))
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
                  updateProductField('costPrice', Number(event.target.value))
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
                  updateProductField('stock', Number(event.target.value))
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
                  updateProductField('safetyStock', Number(event.target.value))
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
                  updateProductField('weightKg', Number(event.target.value))
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
                  updateProductField('leadTimeDays', Number(event.target.value))
                }
                type="number"
                value={form.leadTimeDays ?? 0}
              />
            </div>
            <div className="field">
              <label htmlFor="product-status">Status</label>
              <select
                id="product-status"
                onChange={(event) => updateProductField('status', event.target.value)}
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
                onChange={(event) => updateProductField('taxClass', event.target.value)}
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
              onChange={(event) => updateProductField('description', event.target.value)}
              required
              value={form.description}
            />
          </div>

          <label className="checkbox-row" htmlFor="product-featured">
            <input
              checked={form.featured ?? false}
              id="product-featured"
              onChange={(event) => updateProductField('featured', event.target.checked)}
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
              onChange={(event) => updateOrderFilter('customer', event.target.value)}
              placeholder="Order no, username, or email"
              value={orderFilters.customer}
            />
          </div>
          <div className="field">
            <label htmlFor="order-status-filter">Status</label>
            <select
              id="order-status-filter"
              onChange={(event) => updateOrderFilter('status', event.target.value)}
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
              onChange={(event) => updateOrderFilter('dateFrom', event.target.value)}
              type="date"
              value={orderFilters.dateFrom}
            />
          </div>
          <div className="field">
            <label htmlFor="order-date-to">Date to</label>
            <input
              id="order-date-to"
              onChange={(event) => updateOrderFilter('dateTo', event.target.value)}
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
                          onChange={(event) => updateSelectedTag(order.id, event.target.value)}
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
          <PaginationControls
            disabled={isRefreshing}
            onPageChange={(page) => void loadAdminData({ orderPage: page })}
            page={orderPage.page}
            selectId="admin-order-page"
            totalPages={orderPage.totalPages}
          />
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
                        updateRefundReviewNote(refund.id, event.target.value)
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
          <PaginationControls
            disabled={isRefreshing}
            onPageChange={(page) => void loadAdminData({ refundPage: page })}
            page={refundPage.page}
            selectId="admin-refund-page"
            totalPages={refundPage.totalPages}
          />
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
              onChange={(event) => updateSupportFilter('status', event.target.value)}
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
              onChange={(event) => updateSupportFilter('priority', event.target.value)}
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
                updateSupportFilter('assignedTeam', event.target.value)
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
            const ticketDraft =
              supportTicketDrafts[ticket.id] ?? buildSupportTicketDraft(ticket)

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
          <PaginationControls
            disabled={isRefreshing}
            onPageChange={(page) => void loadAdminData({ supportPage: page })}
            page={supportTicketPage.page}
            selectId="admin-support-page"
            totalPages={supportTicketPage.totalPages}
          />
        ) : null}
      </div>
    )
  }

  if (isLoading) {
    return <LoadingState title="Loading admin control data..." />
  }

  const orderFlowVisible =
    dashboardSummary?.orderStatusBreakdown.filter((metric) => metric.count > 0) ?? []
  const supportFlowVisible =
    dashboardSummary?.supportStatusBreakdown.filter((metric) => metric.count > 0) ?? []
  const openOperationalLoad =
    (dashboardSummary?.activeRefundCases ?? 0) +
    (dashboardSummary?.openSupportTickets ?? 0)

  return (
    <div className="stack-lg">
      <section className="hero admin-hero">
        <div className="hero-grid admin-hero-grid">
          <div className="stack-lg">
            <div className="hero-copy-block admin-hero-copy">
              <p className="eyebrow">Admin command centre</p>
              <h1 className="hero-title">
                Operations, service, and catalog work in one enterprise desk.
              </h1>
              <p className="hero-copy">
                The admin console now acts like a real commerce control room:
                live queue pressure, low-stock watchlists, refund exposure, and
                searchable order operations feed the same workspace.
              </p>
            </div>
            <div className="hero-actions">
              <span className="signal">
                {formatInteger(dashboardSummary?.ordersCreatedToday ?? 0)} orders created
                today
              </span>
              <span className="signal">
                {formatInteger(openOperationalLoad)} active service cases
              </span>
              <span className="signal">
                {formatInteger(dashboardSummary?.lowStockProducts ?? 0)} low-stock alerts
              </span>
            </div>
          </div>

          <div className="admin-hero-side">
            <article className="admin-spotlight-card">
              <p className="eyebrow">30-day captured revenue</p>
              <strong>{formatCurrency(dashboardSummary?.capturedRevenue30Days ?? 0)}</strong>
              <span className="supporting-copy">
                Rolling paid and fulfilled order value across the last 30 days.
              </span>
            </article>
            <article className="admin-spotlight-card">
              <p className="eyebrow">Average order value</p>
              <strong>{formatCurrency(dashboardSummary?.averageOrderValue30Days ?? 0)}</strong>
              <span className="supporting-copy">
                Useful for checking whether promotions and support costs stay in balance.
              </span>
            </article>
          </div>
        </div>
      </section>

      {message ? <div className="message">{message}</div> : null}
      {errorMessage ? <div className="message error">{errorMessage}</div> : null}

      {dashboardSummary ? (
        <section className="surface stack-lg">
          <SectionHeading
            description="The dashboard keeps current business pressure, service queue health, and stock exposure visible before you touch filters or forms."
            eyebrow="Operations overview"
            title="Daily health snapshot"
          />

          <div className="metric-grid dashboard-summary-grid">
            <article className="stat-card stat-card-strong">
              <p className="eyebrow">Order load</p>
              <strong>{formatInteger(dashboardSummary.totalOrders)}</strong>
              <span className="supporting-copy">All recorded orders in the platform</span>
            </article>
            <article className="stat-card">
              <p className="eyebrow">Fulfillment in flight</p>
              <strong>{formatInteger(dashboardSummary.fulfillmentInFlight)}</strong>
              <span className="supporting-copy">Orders still moving through payment or delivery</span>
            </article>
            <article className="stat-card">
              <p className="eyebrow">Open refunds</p>
              <strong>{formatInteger(dashboardSummary.activeRefundCases)}</strong>
              <span className="supporting-copy">Refund cases still requiring operational action</span>
            </article>
            <article className="stat-card">
              <p className="eyebrow">Support backlog</p>
              <strong>{formatInteger(dashboardSummary.openSupportTickets)}</strong>
              <span className="supporting-copy">Customer service tickets not yet resolved</span>
            </article>
            <article className="stat-card">
              <p className="eyebrow">Urgent tickets</p>
              <strong>{formatInteger(dashboardSummary.urgentSupportTickets)}</strong>
              <span className="supporting-copy">Cases escalated for immediate service response</span>
            </article>
            <article className="stat-card">
              <p className="eyebrow">Active catalog</p>
              <strong>{formatInteger(dashboardSummary.activeCatalogProducts)}</strong>
              <span className="supporting-copy">
                Sellable items, with {formatInteger(dashboardSummary.featuredProducts)} featured
              </span>
            </article>
          </div>

          <div className="dashboard-board-grid">
            <article className="surface stack dashboard-panel">
              <div className="dashboard-panel-header">
                <div>
                  <p className="eyebrow">Order flow</p>
                  <h3 className="card-title">Lifecycle pressure</h3>
                </div>
                <span className="signal">
                  {formatInteger(orderFlowVisible.length)} active statuses
                </span>
              </div>

              <div className="dashboard-breakdown-list">
                {orderFlowVisible.map((metric) => (
                  <div className="dashboard-breakdown-row" key={metric.code}>
                    <div className="dashboard-breakdown-copy">
                      <strong>{metric.label}</strong>
                      <span className="supporting-copy">
                        {formatInteger(metric.count)} orders
                      </span>
                    </div>
                    <div className="dashboard-breakdown-bar">
                      <span
                        style={{
                          width: resolveBreakdownWidth(metric.count, orderFlowVisible),
                        }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </article>

            <article className="surface stack dashboard-panel">
              <div className="dashboard-panel-header">
                <div>
                  <p className="eyebrow">Service queue</p>
                  <h3 className="card-title">Support and refunds</h3>
                </div>
                <span className="signal">
                  {formatInteger(openOperationalLoad)} unresolved cases
                </span>
              </div>

              <div className="dashboard-stack-grid">
                <div className="info-card">
                  <p className="eyebrow">Refund request exposure</p>
                  <strong>{formatCurrency(refundSummary?.requestedAmount ?? 0)}</strong>
                  <span className="supporting-copy">
                    Awaiting review or settlement across the active refund pipeline.
                  </span>
                </div>
                <div className="info-card">
                  <p className="eyebrow">Refunds settled</p>
                  <strong>{formatCurrency(refundSummary?.settledAmount ?? 0)}</strong>
                  <span className="supporting-copy">
                    Confirmed finance outcome already recorded back into the order flow.
                  </span>
                </div>
              </div>

              <div className="dashboard-breakdown-list compact">
                {supportFlowVisible.map((metric) => (
                  <div className="dashboard-breakdown-row" key={metric.code}>
                    <div className="dashboard-breakdown-copy">
                      <strong>{metric.label}</strong>
                      <span className="supporting-copy">
                        {formatInteger(metric.count)} tickets
                      </span>
                    </div>
                    <div className="dashboard-breakdown-bar support-tone">
                      <span
                        style={{
                          width: resolveBreakdownWidth(metric.count, supportFlowVisible),
                        }}
                      />
                    </div>
                  </div>
                ))}
              </div>
            </article>

            <article className="surface stack dashboard-panel">
              <div className="dashboard-panel-header">
                <div>
                  <p className="eyebrow">Inventory risk</p>
                  <h3 className="card-title">Low-stock watchlist</h3>
                </div>
                <span className="signal">
                  {formatInteger(dashboardSummary.lowStockProducts)} flagged SKUs
                </span>
              </div>

              {dashboardSummary.lowStockAlerts.length > 0 ? (
                <div className="watchlist">
                  {dashboardSummary.lowStockAlerts.map((alert) => (
                    <article className="watchlist-item" key={alert.productId}>
                      <div className="watchlist-main">
                        <strong>{alert.name}</strong>
                        <span className="supporting-copy">
                          {alert.sku} | {alert.category}
                        </span>
                      </div>
                      <div className="watchlist-metric">
                        <span>{alert.stock} on hand</span>
                        <strong>{alert.shortage} below safety</strong>
                      </div>
                    </article>
                  ))}
                </div>
              ) : (
                <div className="info-card">
                  <strong>No immediate stock pressure</strong>
                  <span className="supporting-copy">
                    Active sellable products are currently above their safety stock thresholds.
                  </span>
                </div>
              )}
            </article>
          </div>
        </section>
      ) : null}

      <section className="surface stack-lg">
        <SectionHeading
          description="Use the workbench for day-to-day execution: search orders, review refunds, keep the support queue moving, and update the sellable catalog."
          eyebrow="Operations workbench"
          title="Queue handling and catalog control"
        />

        <div className="admin-workbench-grid">
          <div className="stack-lg">
            {renderOrderDesk()}
            <div className="admin-secondary-grid">
              {renderRefundDesk()}
              {renderSupportDesk()}
            </div>
          </div>
          {renderProductSection()}
        </div>
      </section>
    </div>
  )
}

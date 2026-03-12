import { type FormEvent, useCallback, useEffect, useRef, useState } from 'react'

import {
  createProduct,
  getAdminRefundRequests,
  reviewRefundRequest,
  searchAdminOrders,
} from '../api/admin'
import { getProducts } from '../api/products'
import type {
  Order,
  PagedResponse,
  Product,
  ProductPayload,
  RefundRequest,
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

export function AdminPage() {
  const [orderPage, setOrderPage] = useState<PagedResponse<Order> | null>(null)
  const [refundPage, setRefundPage] = useState<PagedResponse<RefundRequest> | null>(
    null,
  )
  const [products, setProducts] = useState<Product[]>([])
  const [form, setForm] = useState<ProductPayload>(initialProductForm)
  const [orderFilters, setOrderFilters] = useState(initialOrderFilters)
  const [refundStatusFilter, setRefundStatusFilter] = useState('')
  const [refundReviewNotes, setRefundReviewNotes] = useState<Record<number, string>>(
    {},
  )
  const [isLoading, setIsLoading] = useState(true)
  const [isRefreshing, setIsRefreshing] = useState(false)
  const [isSavingProduct, setIsSavingProduct] = useState(false)
  const [reviewingRefundId, setReviewingRefundId] = useState<number | null>(null)
  const [message, setMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const orderFiltersRef = useRef(initialOrderFilters)
  const refundStatusFilterRef = useRef('')
  const orderPageRef = useRef(0)
  const refundPageRef = useRef(0)

  useEffect(() => {
    orderFiltersRef.current = orderFilters
  }, [orderFilters])

  useEffect(() => {
    refundStatusFilterRef.current = refundStatusFilter
  }, [refundStatusFilter])

  useEffect(() => {
    orderPageRef.current = orderPage?.page ?? 0
  }, [orderPage])

  useEffect(() => {
    refundPageRef.current = refundPage?.page ?? 0
  }, [refundPage])

  const loadAdminData = useCallback(async (options?: {
    orderPage?: number
    refundPage?: number
    filters?: typeof initialOrderFilters
    refundStatus?: string
    keepSkeleton?: boolean
  }) => {
    if (options?.keepSkeleton) {
      setIsLoading(true)
    } else {
      setIsRefreshing(true)
    }
    setErrorMessage('')

    try {
      const [ordersData, productData, refundData] = await Promise.all([
        searchAdminOrders({
          ...(options?.filters ?? orderFiltersRef.current),
          page: options?.orderPage ?? orderPageRef.current,
          size: 8,
        }),
        getProducts({ page: 0, size: 6 }),
        getAdminRefundRequests({
          status: (options?.refundStatus ?? refundStatusFilterRef.current) || undefined,
          page: options?.refundPage ?? refundPageRef.current,
          size: 6,
        }),
      ])

      setOrderPage(ordersData)
      setProducts(productData.items)
      setRefundPage(refundData)
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setIsLoading(false)
      setIsRefreshing(false)
    }
  }, [])

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

  function renderPageOptions(totalPages: number) {
    return Array.from({ length: totalPages }, (_, index) => (
      <option key={index} value={index}>
        {index + 1}
      </option>
    ))
  }

  if (isLoading) {
    return <LoadingState title="Loading admin control data..." />
  }

  return (
    <div className="stack-lg">
      <section className="surface stack-lg">
        <SectionHeading
          description="Admin operations now cover enterprise-style product intake, order filtering, and refund review without leaving the dashboard."
          eyebrow="Admin"
          title="Catalog operations and service desk"
        />

        {message ? <div className="message">{message}</div> : null}
        {errorMessage ? <div className="message error">{errorMessage}</div> : null}

        <div className="admin-grid">
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

          <div className="stack-lg">
            <div className="table-card stack-lg">
              <SectionHeading
                description="Filter by customer, lifecycle status, and created date to find operational exceptions quickly."
                eyebrow="Order desk"
                title="Search the order book"
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
                <span className="signal">
                  {orderPage?.totalElements ?? 0} matching orders
                </span>
              </div>

              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>Order</th>
                      <th>Customer</th>
                      <th>Status</th>
                      <th>Total</th>
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
                    disabled={
                      orderPage.page >= orderPage.totalPages - 1 || isRefreshing
                    }
                    onClick={() =>
                      void loadAdminData({
                        orderPage: Math.min(
                          orderPage.page + 1,
                          orderPage.totalPages - 1,
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
                <span className="signal">
                  {refundPage?.totalElements ?? 0} refund cases
                </span>
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
                            onClick={() =>
                              void handleRefundDecision(refund.id, 'APPROVED')
                            }
                            type="button"
                          >
                            {reviewingRefundId === refund.id
                              ? 'Saving...'
                              : 'Approve'}
                          </button>
                          <button
                            className="button-ghost"
                            disabled={reviewingRefundId === refund.id}
                            onClick={() =>
                              void handleRefundDecision(refund.id, 'REJECTED')
                            }
                            type="button"
                          >
                            Reject
                          </button>
                        </div>
                      </>
                    ) : (
                      <span className="supporting-copy">
                        Reviewed by {refund.reviewedByUsername ?? 'system'}
                        {refund.reviewedAt
                          ? ` on ${formatDateTime(refund.reviewedAt)}`
                          : ''}
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
                    disabled={
                      refundPage.page >= refundPage.totalPages - 1 || isRefreshing
                    }
                    onClick={() =>
                      void loadAdminData({
                        refundPage: Math.min(
                          refundPage.page + 1,
                          refundPage.totalPages - 1,
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
          </div>
        </div>
      </section>
    </div>
  )
}

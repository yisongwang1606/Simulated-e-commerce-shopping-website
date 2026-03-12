import { useEffect, useState } from 'react'

import { createProduct, getAdminOrders } from '../api/admin'
import { getProducts } from '../api/products'
import type { Order, Product, ProductPayload } from '../api/contracts'
import { extractErrorMessage } from '../shared/error'
import { formatCurrency, formatDate } from '../shared/formatters'
import { LoadingState } from '../shared/ui/LoadingState'
import { SectionHeading } from '../shared/ui/SectionHeading'
import { StatusPill } from '../shared/ui/StatusPill'

const initialForm: ProductPayload = {
  name: '',
  price: 89.99,
  stock: 10,
  category: 'Electronics',
  description: '',
}

export function AdminPage() {
  const [orders, setOrders] = useState<Order[]>([])
  const [products, setProducts] = useState<Product[]>([])
  const [form, setForm] = useState<ProductPayload>(initialForm)
  const [isLoading, setIsLoading] = useState(true)
  const [isSaving, setIsSaving] = useState(false)
  const [message, setMessage] = useState('')
  const [errorMessage, setErrorMessage] = useState('')

  async function fetchAdminData() {
    setIsLoading(true)
    setErrorMessage('')

    try {
      const [orderData, productData] = await Promise.all([
        getAdminOrders(),
        getProducts({ page: 0, size: 6 }),
      ])

      setOrders(orderData)
      setProducts(productData.items)
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  }

  useEffect(() => {
    void fetchAdminData()
  }, [])

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsSaving(true)
    setMessage('')
    setErrorMessage('')

    try {
      const createdProduct = await createProduct(form)
      setMessage(`Created product #${createdProduct.id} successfully.`)
      setForm(initialForm)
      await fetchAdminData()
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setIsSaving(false)
    }
  }

  if (isLoading) {
    return <LoadingState title="Loading admin control data..." />
  }

  return (
    <div className="stack-lg">
      <section className="surface stack-lg">
        <SectionHeading
          description="This route is guarded by the ADMIN role and uses your /api/admin endpoints."
          eyebrow="Admin"
          title="Catalog ops and order oversight"
        />

        {message ? <div className="message">{message}</div> : null}
        {errorMessage ? <div className="message error">{errorMessage}</div> : null}

        <div className="admin-grid">
          <div className="surface stack-lg">
            <SectionHeading
              description="Create products directly against the backend catalog API."
              eyebrow="Product intake"
              title="Add a new product"
            />

            <form className="form-grid compact" onSubmit={(event) => void handleSubmit(event)}>
              <div className="form-columns">
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

              <div className="card-actions">
                <button className="button" disabled={isSaving} type="submit">
                  {isSaving ? 'Creating...' : 'Create product'}
                </button>
                <span className="signal">Admin JWT required</span>
              </div>
            </form>

            <div className="stack">
              <p className="eyebrow">Recent catalog sample</p>
              <div className="product-grid">
                {products.map((product) => (
                  <article className="product-card" key={product.id}>
                    <div className="stack">
                      <span className="pill">{product.category}</span>
                      <h3 className="product-title">{product.name}</h3>
                      <p className="section-copy">{product.description}</p>
                    </div>
                    <div className="summary-row">
                      <span>{product.stock} in stock</span>
                      <strong>{formatCurrency(product.price)}</strong>
                    </div>
                  </article>
                ))}
              </div>
            </div>
          </div>

          <div className="table-card stack-lg">
            <SectionHeading
              description="Operational readout from /api/admin/orders for quick order review."
              eyebrow="Order desk"
              title="Latest orders"
            />

            <div className="table-scroll">
              <table>
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>User</th>
                    <th>Status</th>
                    <th>Total</th>
                    <th>Placed</th>
                  </tr>
                </thead>
                <tbody>
                  {orders.map((order) => (
                    <tr key={order.id}>
                      <td>#{order.id}</td>
                      <td>{order.username}</td>
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
          </div>
        </div>
      </section>
    </div>
  )
}

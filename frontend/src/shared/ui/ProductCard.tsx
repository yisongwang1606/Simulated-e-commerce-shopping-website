import { Link } from 'react-router-dom'

import type { Product } from '../../api/contracts'
import { formatCurrency } from '../formatters'

interface ProductCardProps {
  product: Product
}

export function ProductCard({ product }: ProductCardProps) {
  const stockLabel =
    product.stock <= product.safetyStock ? 'Low stock watch' : 'Stock healthy'

  return (
    <article className="product-card">
      <div className="stack">
        <div className="product-meta">
          <span className="pill">{product.category}</span>
          {product.featured ? <span className="signal">Featured</span> : null}
        </div>
        <h3 className="product-title">{product.name}</h3>
        <p className="product-subtitle">
          {product.sku}
          {product.brand ? ` | ${product.brand}` : ''}
        </p>
        <p className="section-copy">{product.description}</p>
      </div>

      <div className="product-card-footer">
        <div className="stack">
          <span className="price">{formatCurrency(product.price)}</span>
          <span
            className={
              product.stock <= product.safetyStock
                ? 'inventory-warning'
                : 'supporting-copy'
            }
          >
            {product.stock} in stock | {stockLabel}
          </span>
        </div>
        <Link className="button-outline" to={`/products/${product.id}`}>
          View details
        </Link>
      </div>
    </article>
  )
}

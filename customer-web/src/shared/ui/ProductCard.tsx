import { Link } from 'react-router-dom'

import type { Product } from '../../api/contracts'
import { formatCurrency } from '../formatters'

interface ProductCardProps {
  product: Product
}

export function ProductCard({ product }: ProductCardProps) {
  const shippingLabel = product.leadTimeDays <= 5 ? 'Fast dispatch' : `${product.leadTimeDays} day lead time`
  const stockTone =
    product.stock <= product.safetyStock ? 'market-stock-warning' : 'market-stock-ok'

  return (
    <article className="product-card market-product-card">
      <div className="market-product-visual">
        <span className="pill">{product.category}</span>
        {product.featured ? <span className="signal">Featured pick</span> : null}
      </div>

      <div className="stack">
        <h3 className="product-title market-product-title">{product.name}</h3>
        <p className="product-subtitle market-product-subtitle">
          {product.brand}
          {product.sku ? ` | ${product.sku}` : ''}
        </p>
        <p className="section-copy market-product-copy">{product.description}</p>
      </div>

      <div className="market-product-footer">
        <div className="stack">
          <span className="price">{formatCurrency(product.price)}</span>
          <span className={stockTone}>
            {product.stock} in stock | {shippingLabel}
          </span>
        </div>
        <Link className="button-outline compact" to={`/products/${product.id}`}>
          View details
        </Link>
      </div>
    </article>
  )
}

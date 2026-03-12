import { Link } from 'react-router-dom'

import type { Product } from '../../api/contracts'
import { formatCurrency } from '../formatters'

interface ProductCardProps {
  product: Product
}

export function ProductCard({ product }: ProductCardProps) {
  return (
    <article className="product-card">
      <div className="stack">
        <div className="product-meta">
          <span className="pill">{product.category}</span>
          <span>{product.stock} in stock</span>
        </div>
        <h3 className="product-title">{product.name}</h3>
        <p className="section-copy">{product.description}</p>
      </div>

      <div className="card-actions">
        <span className="price">{formatCurrency(product.price)}</span>
        <Link className="button-outline" to={`/products/${product.id}`}>
          View details
        </Link>
      </div>
    </article>
  )
}

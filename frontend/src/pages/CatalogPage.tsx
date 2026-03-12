import {
  startTransition,
  useDeferredValue,
  useEffect,
  useEffectEvent,
  useState,
} from 'react'

import { getCategories, getProducts } from '../api/products'
import type { PagedResponse, Product } from '../api/contracts'
import { extractErrorMessage } from '../shared/error'
import { EmptyState } from '../shared/ui/EmptyState'
import { LoadingState } from '../shared/ui/LoadingState'
import { ProductCard } from '../shared/ui/ProductCard'
import { SectionHeading } from '../shared/ui/SectionHeading'

export function CatalogPage() {
  const [products, setProducts] = useState<PagedResponse<Product> | null>(null)
  const [categories, setCategories] = useState<string[]>([])
  const [keyword, setKeyword] = useState('')
  const [category, setCategory] = useState('')
  const [page, setPage] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [errorMessage, setErrorMessage] = useState('')
  const deferredKeyword = useDeferredValue(keyword)

  const loadCategories = useEffectEvent(async () => {
    try {
      const data = await getCategories()
      setCategories(data)
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    }
  })

  const loadProducts = useEffectEvent(async () => {
    setIsLoading(true)
    setErrorMessage('')

    try {
      const data = await getProducts({
        keyword: deferredKeyword,
        category,
        page,
        size: 9,
      })

      setProducts(data)
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setIsLoading(false)
    }
  })

  useEffect(() => {
    void loadCategories()
  }, [])

  useEffect(() => {
    void loadProducts()
  }, [category, deferredKeyword, page])

  return (
    <div className="stack-lg">
      <section className="surface stack-lg">
        <SectionHeading
          description="The catalog page talks directly to /api/products with search, category filtering, and paging."
          eyebrow="Catalog"
          title="Live product browsing"
        />

        <div className="toolbar">
          <div className="field" style={{ minWidth: 'min(100%, 320px)' }}>
            <label htmlFor="catalog-search">Keyword search</label>
            <input
              id="catalog-search"
              onChange={(event) => {
                const nextValue = event.target.value
                startTransition(() => {
                  setKeyword(nextValue)
                  setPage(0)
                })
              }}
              placeholder="Search by product name or description"
              value={keyword}
            />
          </div>
          <span className="signal">
            {products?.totalElements ?? 0} items available
          </span>
        </div>

        <div className="catalog-grid">
          <aside className="surface stack">
            <p className="eyebrow">Categories</p>
            <div className="chip-row">
              <button
                className={category === '' ? 'chip active' : 'chip'}
                onClick={() => {
                  startTransition(() => {
                    setCategory('')
                    setPage(0)
                  })
                }}
                type="button"
              >
                All
              </button>
              {categories.map((entry) => (
                <button
                  className={category === entry ? 'chip active' : 'chip'}
                  key={entry}
                  onClick={() => {
                    startTransition(() => {
                      setCategory(entry)
                      setPage(0)
                    })
                  }}
                  type="button"
                >
                  {entry}
                </button>
              ))}
            </div>
          </aside>

          <div className="stack-lg">
            {errorMessage ? <div className="message error">{errorMessage}</div> : null}

            {isLoading ? (
              <LoadingState title="Refreshing the product grid..." />
            ) : products && products.items.length > 0 ? (
              <>
                <div className="product-grid">
                  {products.items.map((product) => (
                    <ProductCard key={product.id} product={product} />
                  ))}
                </div>

                <div className="pagination">
                  <button
                    className="button-outline"
                    disabled={page === 0}
                    onClick={() => {
                      startTransition(() => setPage((currentPage) => currentPage - 1))
                    }}
                    type="button"
                  >
                    Previous
                  </button>
                  <span className="signal">
                    Page {products.page + 1} of {Math.max(products.totalPages, 1)}
                  </span>
                  <button
                    className="button-outline"
                    disabled={page + 1 >= products.totalPages}
                    onClick={() => {
                      startTransition(() => setPage((currentPage) => currentPage + 1))
                    }}
                    type="button"
                  >
                    Next
                  </button>
                </div>
              </>
            ) : (
              <EmptyState
                message="Try a different keyword or category filter. The query is already hitting the backend."
                title="No products matched the current filter"
              />
            )}
          </div>
        </div>
      </section>
    </div>
  )
}

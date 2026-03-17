import {
  startTransition,
  useDeferredValue,
  useEffect,
  useEffectEvent,
  useRef,
  useState,
} from 'react'
import { useSearchParams } from 'react-router-dom'

import { getCategories, getProducts } from '../api/products'
import type { PagedResponse, Product } from '../api/contracts'
import { extractErrorMessage } from '../shared/error'
import { formatInteger } from '../shared/formatters'
import { EmptyState } from '../shared/ui/EmptyState'
import { LoadingState } from '../shared/ui/LoadingState'
import { ProductCard } from '../shared/ui/ProductCard'
import { SectionHeading } from '../shared/ui/SectionHeading'

export function CatalogPage() {
  const [searchParams] = useSearchParams()
  const keywordFromRoute = searchParams.get('keyword') ?? ''
  const [products, setProducts] = useState<PagedResponse<Product> | null>(null)
  const [categories, setCategories] = useState<string[]>([])
  const [keyword, setKeyword] = useState(keywordFromRoute)
  const [category, setCategory] = useState('')
  const [page, setPage] = useState(0)
  const [isLoading, setIsLoading] = useState(true)
  const [isRefreshing, setIsRefreshing] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const pendingScrollY = useRef<number | null>(null)
  const deferredKeyword = useDeferredValue(keyword)
  const totalPages = Math.max(products?.totalPages ?? 1, 1)
  const pageItems = buildPageItems(page, totalPages)

  const loadCategories = useEffectEvent(async () => {
    try {
      const data = await getCategories()
      setCategories(data)
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    }
  })

  const loadProducts = useEffectEvent(async () => {
    const hasRenderedProducts = products !== null
    if (hasRenderedProducts) {
      setIsRefreshing(true)
    } else {
      setIsLoading(true)
    }
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
      setIsRefreshing(false)
    }
  })

  const queuePageChange = (nextPage: number) => {
    pendingScrollY.current = window.scrollY
    startTransition(() => setPage(nextPage))
  }

  useEffect(() => {
    void loadCategories()
  }, [])

  useEffect(() => {
    setKeyword(keywordFromRoute)
    setPage(0)
  }, [keywordFromRoute])

  useEffect(() => {
    void loadProducts()
  }, [category, deferredKeyword, page])

  useEffect(() => {
    if (isRefreshing || pendingScrollY.current === null) {
      return
    }

    const nextScrollY = pendingScrollY.current
    pendingScrollY.current = null
    window.scrollTo({ top: nextScrollY, behavior: 'auto' })
  }, [isRefreshing, products])

  return (
    <div className="stack-lg">
      <section className="surface stack-lg">
        <SectionHeading
          description="Browse with the same dense comparison rhythm shoppers expect from large marketplaces: quick filters, direct page jumps, and product cards built for scanning."
          eyebrow="Marketplace catalog"
          title="Find the right offer faster"
        />

        <div className="metric-grid catalog-summary-grid">
          <article className="stat-card">
            <p className="eyebrow">Visible catalog</p>
            <strong>{formatInteger(products?.totalElements ?? 0)}</strong>
            <span className="supporting-copy">Products matching the current filters</span>
          </article>
          <article className="stat-card">
            <p className="eyebrow">Category mix</p>
            <strong>{formatInteger(categories.length)}</strong>
            <span className="supporting-copy">Active product categories</span>
          </article>
          <article className="stat-card">
            <p className="eyebrow">Current page</p>
            <strong>
              {formatInteger(page + 1)} / {formatInteger(totalPages)}
            </strong>
            <span className="supporting-copy">Direct page selection is available below</span>
          </article>
        </div>

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
          {isRefreshing ? (
            <span aria-live="polite" className="signal">
              Refreshing page...
            </span>
          ) : null}
        </div>

        <div className="catalog-grid">
          <aside className="surface stack filter-rail">
            <p className="eyebrow">Categories</p>
            <h3 className="card-title">Filter the assortment</h3>
            <p className="supporting-copy">
              Narrow the grid by business category while keeping your current page
              position stable.
            </p>
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
                <div className="toolbar catalog-results-toolbar">
                  <div className="stack">
                    <p className="eyebrow">Results</p>
                    <h3 className="card-title">
                      {formatInteger(products.items.length)} products on this page
                    </h3>
                  </div>
                  <span className="signal">
                    Showing page {formatInteger(page + 1)} of {formatInteger(totalPages)}
                  </span>
                </div>

                <div className="product-grid">
                  {products.items.map((product) => (
                    <ProductCard key={product.id} product={product} />
                  ))}
                </div>

                <div className="pagination">
                  <button
                    className="button-outline"
                    disabled={page === 0 || isRefreshing}
                    onClick={() => {
                      queuePageChange(page - 1)
                    }}
                    type="button"
                  >
                    Previous
                  </button>
                  <div className="page-picker">
                    {pageItems.map((item, index) =>
                      item === 'ellipsis' ? (
                        <span className="page-gap" key={`ellipsis-${index}`}>
                          ...
                        </span>
                      ) : (
                        <button
                          className={item === page ? 'page-number active' : 'page-number'}
                          disabled={isRefreshing}
                          key={item}
                          onClick={() => {
                            queuePageChange(item)
                          }}
                          type="button"
                        >
                          {item + 1}
                        </button>
                      ),
                    )}
                  </div>
                  <div className="page-jump">
                    <label htmlFor="catalog-page-jump">Jump to</label>
                    <select
                      disabled={isRefreshing}
                      id="catalog-page-jump"
                      onChange={(event) => {
                        queuePageChange(Number(event.target.value))
                      }}
                      value={page}
                    >
                      {Array.from({ length: totalPages }, (_, index) => (
                        <option key={index} value={index}>
                          Page {index + 1}
                        </option>
                      ))}
                    </select>
                  </div>
                  <button
                    className="button-outline"
                    disabled={page + 1 >= totalPages || isRefreshing}
                    onClick={() => {
                      queuePageChange(page + 1)
                    }}
                    type="button"
                  >
                    Next
                  </button>
                </div>
              </>
            ) : (
              <EmptyState
                message="Try a different keyword or category filter."
                title="No products matched the current filter"
              />
            )}
          </div>
        </div>
      </section>
    </div>
  )
}

function buildPageItems(
  currentPage: number,
  totalPages: number,
): Array<number | 'ellipsis'> {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, index) => index)
  }

  const pages = new Set<number>([0, totalPages - 1, currentPage])
  pages.add(Math.max(currentPage - 1, 0))
  pages.add(Math.min(currentPage + 1, totalPages - 1))

  const orderedPages = [...pages].sort((left, right) => left - right)
  const items: Array<number | 'ellipsis'> = []

  orderedPages.forEach((pageNumber, index) => {
    const previous = orderedPages[index - 1]

    if (index > 0 && previous !== undefined && pageNumber - previous > 1) {
      items.push('ellipsis')
    }

    items.push(pageNumber)
  })

  return items
}

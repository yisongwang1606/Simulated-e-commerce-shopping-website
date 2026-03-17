import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'

import { logout } from '../../api/auth'
import { getCategories, getProducts } from '../../api/products'
import type { Product } from '../../api/contracts'
import { useSessionStore } from '../../store/sessionStore'

interface SearchSuggestion {
  id: string
  label: string
  description: string
  type: 'category' | 'product'
  keyword?: string
  productId?: number
}

export function StorefrontShell() {
  const navigate = useNavigate()
  const token = useSessionStore((state) => state.token)
  const user = useSessionStore((state) => state.user)
  const clearSession = useSessionStore((state) => state.clearSession)
  const [keyword, setKeyword] = useState('')
  const [categories, setCategories] = useState<string[]>([])
  const [suggestions, setSuggestions] = useState<SearchSuggestion[]>([])
  const [isSuggestionsOpen, setIsSuggestionsOpen] = useState(false)
  const [isSuggesting, setIsSuggesting] = useState(false)
  const [highlightedIndex, setHighlightedIndex] = useState(0)
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const closeTimerRef = useRef<number | null>(null)

  const hasTypedKeyword = useMemo(() => keyword.trim().length > 1, [keyword])

  useEffect(() => {
    void (async () => {
      try {
        setCategories(await getCategories())
      } catch {
        setCategories([])
      }
    })()
  }, [])

  useEffect(() => {
    const normalizedKeyword = keyword.trim()

    if (normalizedKeyword.length <= 1) {
      setSuggestions([])
      setIsSuggesting(false)
      setHighlightedIndex(0)
      return
    }

    const timeoutId = window.setTimeout(async () => {
      setIsSuggesting(true)

      try {
        const [productPage] = await Promise.all([
          getProducts({ keyword: normalizedKeyword, page: 0, size: 5 }),
        ])

        const lowerKeyword = normalizedKeyword.toLowerCase()
        const categorySuggestions = categories
          .filter((category) => category.toLowerCase().includes(lowerKeyword))
          .slice(0, 3)
          .map<SearchSuggestion>((category) => ({
            id: `category:${category}`,
            label: category,
            description: 'Category',
            type: 'category',
            keyword: category,
          }))

        const productSuggestions = productPage.items.map<SearchSuggestion>(
          (product: Product) => ({
            id: `product:${product.id}`,
            label: product.name,
            description: product.brand || product.category,
            type: 'product',
            productId: product.id,
          }),
        )

        setSuggestions([...categorySuggestions, ...productSuggestions].slice(0, 8))
        setHighlightedIndex(0)
      } catch {
        setSuggestions([])
      } finally {
        setIsSuggesting(false)
      }
    }, 180)

    return () => {
      window.clearTimeout(timeoutId)
    }
  }, [categories, keyword])

  useEffect(() => {
    return () => {
      if (closeTimerRef.current !== null) {
        window.clearTimeout(closeTimerRef.current)
      }
    }
  }, [])

  async function handleLogout() {
    setIsLoggingOut(true)

    try {
      if (token) {
        await logout()
      }
    } catch {
      // Prefer clearing the local session over blocking the shopper on logout.
    } finally {
      clearSession()
      setIsLoggingOut(false)
      navigate('/')
    }
  }

  function handleSearchSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (isSuggestionsOpen && suggestions[highlightedIndex]) {
      handleSuggestionSelect(suggestions[highlightedIndex])
      return
    }

    const normalizedKeyword = keyword.trim()
    const suffix = normalizedKeyword ? `?keyword=${encodeURIComponent(normalizedKeyword)}` : ''
    setIsSuggestionsOpen(false)
    navigate(`/catalog${suffix}`)
  }

  function handleSuggestionSelect(suggestion: SearchSuggestion) {
    setIsSuggestionsOpen(false)

    if (suggestion.type === 'product' && suggestion.productId) {
      setKeyword(suggestion.label)
      navigate(`/products/${suggestion.productId}`)
      return
    }

    const nextKeyword = suggestion.keyword ?? suggestion.label
    setKeyword(nextKeyword)
    navigate(`/catalog?keyword=${encodeURIComponent(nextKeyword)}`)
  }

  function handleSearchFocus() {
    if (closeTimerRef.current !== null) {
      window.clearTimeout(closeTimerRef.current)
      closeTimerRef.current = null
    }

    if (hasTypedKeyword) {
      setIsSuggestionsOpen(true)
    }
  }

  function handleSearchBlur() {
    closeTimerRef.current = window.setTimeout(() => {
      setIsSuggestionsOpen(false)
    }, 160)
  }

  function handleSearchKeyDown(event: React.KeyboardEvent<HTMLInputElement>) {
    if (!isSuggestionsOpen || suggestions.length === 0) {
      return
    }

    if (event.key === 'ArrowDown') {
      event.preventDefault()
      setHighlightedIndex((current) => (current + 1) % suggestions.length)
      return
    }

    if (event.key === 'ArrowUp') {
      event.preventDefault()
      setHighlightedIndex((current) =>
        current === 0 ? suggestions.length - 1 : current - 1,
      )
      return
    }

    if (event.key === 'Escape') {
      setIsSuggestionsOpen(false)
    }
  }

  return (
    <div className="storefront-shell">
      <header className="market-utility-bar">
        <div className="market-utility-inner">
          <span>Delivering across Canada</span>
          <div className="market-utility-links">
            <span>{user ? `Signed in as ${user.username}` : 'Guest browsing'}</span>
            <a href="http://127.0.0.1:4174/login" rel="noreferrer" target="_blank">
              Admin portal
            </a>
          </div>
        </div>
      </header>

      <header className="market-header">
        <div className="market-header-inner">
          <Link className="market-brand" to="/">
            <span className="market-brand-badge">NL</span>
            <div className="market-brand-copy">
              <strong>Northline Market</strong>
              <span>Fast catalog, cart, checkout, and after-sales</span>
            </div>
          </Link>

          <form className="market-search" onSubmit={handleSearchSubmit}>
            <span className="market-search-scope">All</span>
            <div className="market-search-shell">
              <input
                aria-label="Search the storefront"
                onBlur={handleSearchBlur}
                onChange={(event) => {
                  const nextKeyword = event.target.value
                  setKeyword(nextKeyword)
                  setIsSuggestionsOpen(nextKeyword.trim().length > 1)
                }}
                onFocus={handleSearchFocus}
                onKeyDown={handleSearchKeyDown}
                placeholder="Search products, brands, or categories"
                value={keyword}
              />

              {isSuggestionsOpen ? (
                <div className="market-search-dropdown">
                  {isSuggesting ? (
                    <div className="market-search-empty">Looking for matches...</div>
                  ) : suggestions.length > 0 ? (
                    suggestions.map((suggestion, index) => (
                      <button
                        className={
                          index === highlightedIndex
                            ? 'market-search-suggestion active'
                            : 'market-search-suggestion'
                        }
                        key={suggestion.id}
                        onClick={() => handleSuggestionSelect(suggestion)}
                        type="button"
                      >
                        <span className="market-search-suggestion-copy">
                          <strong>{suggestion.label}</strong>
                          <span>{suggestion.description}</span>
                        </span>
                        <span className="market-search-suggestion-type">
                          {suggestion.type === 'product' ? 'Product' : 'Category'}
                        </span>
                      </button>
                    ))
                  ) : (
                    <div className="market-search-empty">No matching suggestions yet</div>
                  )}
                </div>
              ) : null}
            </div>
            <button className="market-search-button" type="submit">
              Search
            </button>
          </form>

          <div className="market-account-box">
            <span className="eyebrow">{user ? 'Signed in' : 'Hello, shopper'}</span>
            {token ? (
              <button
                className="button-outline compact"
                disabled={isLoggingOut}
                onClick={() => void handleLogout()}
                type="button"
              >
                {isLoggingOut ? 'Signing out...' : 'Logout'}
              </button>
            ) : (
              <Link className="button-outline compact" to="/login">
                Sign in
              </Link>
            )}
          </div>
        </div>
      </header>

      <nav className="market-nav">
        <div className="market-nav-inner">
          <NavLink className={({ isActive }) => (isActive ? 'market-link active' : 'market-link')} end to="/">
            Home
          </NavLink>
          <NavLink className={({ isActive }) => (isActive ? 'market-link active' : 'market-link')} to="/catalog">
            Catalog
          </NavLink>
          <NavLink className={({ isActive }) => (isActive ? 'market-link active' : 'market-link')} to="/cart">
            Cart
          </NavLink>
          <NavLink className={({ isActive }) => (isActive ? 'market-link active' : 'market-link')} to="/orders">
            Orders
          </NavLink>
          <span className="market-link market-link-static">Deals</span>
          <span className="market-link market-link-static">New arrivals</span>
        </div>
      </nav>

      <main className="market-main">
        <Outlet />
      </main>

      <footer className="market-footer">
        <div className="market-footer-inner">
          <p>Northline Market brings product discovery, checkout, and after-sales service into one customer account.</p>
          <p>Customer app on `4173` | Admin app on `4174`</p>
        </div>
      </footer>
    </div>
  )
}

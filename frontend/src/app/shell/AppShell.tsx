import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'

import { logout } from '../../api/auth'
import { apiBaseUrl } from '../../api/client'
import { isAdmin, useSessionStore } from '../../store/sessionStore'

export function AppShell() {
  const navigate = useNavigate()
  const token = useSessionStore((state) => state.token)
  const user = useSessionStore((state) => state.user)
  const clearSession = useSessionStore((state) => state.clearSession)
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const apiLabel = apiBaseUrl || 'same-origin proxy'

  async function handleLogout() {
    setIsLoggingOut(true)

    try {
      if (token) {
        await logout()
      }
    } catch {
      // Clearing local session is more important than surfacing logout revocation failures.
    } finally {
      clearSession()
      setIsLoggingOut(false)
      navigate('/')
    }
  }

  return (
    <div className="page-shell">
      <header className="topbar">
        <div className="topbar-card">
          <div className="brand-row">
            <div className="brand-mark">
              <span className="brand-badge">EC</span>
              <div className="brand-copy">
                <span className="brand-title">Northline Commerce Suite</span>
                <span className="brand-subtitle">
                  Storefront, service, and operations workspace for the live
                  Spring Boot platform
                </span>
              </div>
            </div>

            <div className="topbar-status">
              <span className="signal">API {apiLabel}</span>
              <span className="signal">Java 21 | MySQL 8.4 | Redis 7.4</span>
              {user ? (
                <span className={`status-pill ${user.role.toLowerCase()}`}>
                  {user.username} | {user.role}
                </span>
              ) : (
                <span className="status-pill customer">Guest mode</span>
              )}
            </div>
          </div>

          <div className="nav-row">
            <div className="nav-links">
              <NavLink
                className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
                to="/"
              >
                Home
              </NavLink>
              <NavLink
                className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
                to="/catalog"
              >
                Catalog
              </NavLink>
              <NavLink
                className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
                to="/cart"
              >
                Cart
              </NavLink>
              <NavLink
                className={({ isActive }) => (isActive ? 'nav-link active' : 'nav-link')}
                to="/orders"
              >
                Orders
              </NavLink>
              {isAdmin(user) ? (
                <NavLink
                  className={({ isActive }) =>
                    isActive ? 'nav-link active' : 'nav-link'
                  }
                  to="/admin"
                >
                  Admin
                </NavLink>
              ) : null}
            </div>

            <div className="nav-actions">
              <a
                className="ghost-link"
                href="http://127.0.0.1:8080/swagger-ui.html"
                rel="noreferrer"
                target="_blank"
              >
                Swagger UI
              </a>

              {token ? (
                <button
                  className="button-ghost"
                  disabled={isLoggingOut}
                  onClick={() => void handleLogout()}
                  type="button"
                >
                  {isLoggingOut ? 'Signing out...' : 'Logout'}
                </button>
              ) : (
                <Link className="button" to="/login">
                  Login
                </Link>
              )}
            </div>
          </div>
        </div>
      </header>

      <main className="content">
        <Outlet />
      </main>

      <footer className="footer">
        <div className="footer-row">
          <p className="footer-copy">
            Enterprise workflow baseline running on Java 21, MySQL 8.4, and
            Redis 7.4.
          </p>
          <p className="footer-copy">React 19 | Vite 7 | TypeScript 5.9</p>
        </div>
      </footer>
    </div>
  )
}

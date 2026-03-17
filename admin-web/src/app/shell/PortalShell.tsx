import { useState } from 'react'
import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'

import { logout } from '../../api/auth'
import { apiBaseUrl } from '../../api/client'
import { useSessionStore } from '../../store/sessionStore'

type RouteNavItem = {
  kind: 'route'
  label: string
  to: string
  end?: boolean
}

type AnchorNavItem = {
  kind: 'anchor'
  href: string
  label: string
}

export type PortalNavItem = RouteNavItem | AnchorNavItem

interface PortalShellProps {
  badge: string
  title: string
  subtitle: string
  navItems: PortalNavItem[]
  statusItems: string[]
  guestLabel: string
  loginPath: string
  logoutRedirect: string
  footerPrimary: string
  footerSecondary: string
  pageClassName?: string
  topbarClassName?: string
  showSwagger?: boolean
  switchLink?: {
    label: string
    to: string
  }
}

export function PortalShell({
  badge,
  title,
  subtitle,
  navItems,
  statusItems,
  guestLabel,
  loginPath,
  logoutRedirect,
  footerPrimary,
  footerSecondary,
  pageClassName,
  topbarClassName,
  showSwagger = false,
  switchLink,
}: PortalShellProps) {
  const navigate = useNavigate()
  const token = useSessionStore((state) => state.token)
  const user = useSessionStore((state) => state.user)
  const clearSession = useSessionStore((state) => state.clearSession)
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const resolvedStatusItems = statusItems.map((item) =>
    item === '{api}' ? `API ${apiBaseUrl || 'same-origin proxy'}` : item,
  )

  async function handleLogout() {
    setIsLoggingOut(true)

    try {
      if (token) {
        await logout()
      }
    } catch {
      // Local session clearing matters more than surfacing logout revocation failures.
    } finally {
      clearSession()
      setIsLoggingOut(false)
      navigate(logoutRedirect)
    }
  }

  const shellClassName = ['page-shell', pageClassName].filter(Boolean).join(' ')
  const cardClassName = ['topbar-card', topbarClassName].filter(Boolean).join(' ')

  return (
    <div className={shellClassName}>
      <header className="topbar">
        <div className={cardClassName}>
          <div className="brand-row">
            <div className="brand-mark">
              <span className="brand-badge">{badge}</span>
              <div className="brand-copy">
                <span className="brand-title">{title}</span>
                <span className="brand-subtitle">{subtitle}</span>
              </div>
            </div>

            <div className="topbar-status">
              {resolvedStatusItems.map((item) => (
                <span className="signal" key={item}>
                  {item}
                </span>
              ))}
              {user ? (
                <span className={`status-pill ${user.role.toLowerCase()}`}>
                  {user.username} | {user.role}
                </span>
              ) : (
                <span className="status-pill customer">{guestLabel}</span>
              )}
            </div>
          </div>

          <div className="nav-row">
            <div className="nav-links">
              {navItems.map((item) =>
                item.kind === 'route' ? (
                  <NavLink
                    className={({ isActive }) =>
                      isActive ? 'nav-link active' : 'nav-link'
                    }
                    end={item.end}
                    key={`${item.kind}:${item.to}`}
                    to={item.to}
                  >
                    {item.label}
                  </NavLink>
                ) : (
                  <a className="nav-link" href={item.href} key={`${item.kind}:${item.href}`}>
                    {item.label}
                  </a>
                ),
              )}
            </div>

            <div className="nav-actions">
              {switchLink ? (
                <Link className="ghost-link" to={switchLink.to}>
                  {switchLink.label}
                </Link>
              ) : null}

              {showSwagger ? (
                <a
                  className="ghost-link"
                  href="http://127.0.0.1:8080/swagger-ui.html"
                  rel="noreferrer"
                  target="_blank"
                >
                  Swagger UI
                </a>
              ) : null}

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
                <Link className="button" to={loginPath}>
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
          <p className="footer-copy">{footerPrimary}</p>
          <p className="footer-copy">{footerSecondary}</p>
        </div>
      </footer>
    </div>
  )
}

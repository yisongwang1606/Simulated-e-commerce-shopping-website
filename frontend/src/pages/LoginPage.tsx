import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'

import { login } from '../api/auth'
import type { LoginInput } from '../api/contracts'
import { extractErrorMessage } from '../shared/error'
import { setObjectField } from '../shared/state'
import { SectionHeading } from '../shared/ui/SectionHeading'
import { useSessionStore } from '../store/sessionStore'

interface LocationState {
  from?: string
}

interface LoginPageProps {
  portal?: 'customer' | 'admin'
}

const customerCredentials: LoginInput = {
  username: 'demo@ecom.local',
  password: 'Demo123!',
}

const adminCredentials: LoginInput = {
  username: 'admin@ecom.local',
  password: 'Admin123!',
}

export function LoginPage({ portal = 'customer' }: LoginPageProps) {
  const navigate = useNavigate()
  const location = useLocation()
  const setSession = useSessionStore((state) => state.setSession)
  const isAdminPortal = portal === 'admin'

  const [form, setForm] = useState<LoginInput>({
    ...(isAdminPortal ? adminCredentials : customerCredentials),
  })
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsSubmitting(true)
    setErrorMessage('')

    try {
      const payload = await login(form)
      if (isAdminPortal && payload.user.role !== 'ADMIN') {
        setErrorMessage('This sign-in portal is restricted to admin accounts.')
        return
      }

      setSession(payload)
      const state = location.state as LocationState | null
      const fallbackPath =
        payload.user.role === 'ADMIN' ? '/admin' : '/'
      navigate(state?.from ?? fallbackPath)
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className={`login-wrap ${isAdminPortal ? 'admin-login-wrap' : ''}`.trim()}>
      <section
        className={`login-card stack-lg ${isAdminPortal ? 'admin-login-card' : ''}`.trim()}
      >
        <SectionHeading
          description={
            isAdminPortal
              ? 'This admin-only sign-in unlocks the operations portal for order governance, refund review, support triage, and catalog control.'
              : 'This customer sign-in stores the JWT locally and unlocks cart, orders, delivery follow-up, and after-sales actions.'
          }
          eyebrow={isAdminPortal ? 'Admin authentication' : 'Customer authentication'}
          title={
            isAdminPortal
              ? 'Sign in to the operations portal'
              : 'Sign in to the customer storefront'
          }
        />

        {errorMessage ? <div className="message error">{errorMessage}</div> : null}

        <form className="form-grid compact" onSubmit={(event) => void handleSubmit(event)}>
          <div className="field">
            <label htmlFor="username">Username or email</label>
            <input
              id="username"
              onChange={(event) =>
                setForm((current) =>
                  setObjectField(current, 'username', event.target.value),
                )
              }
              required
              value={form.username}
            />
          </div>

          <div className="field">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              onChange={(event) =>
                setForm((current) =>
                  setObjectField(current, 'password', event.target.value),
                )
              }
              required
              type="password"
              value={form.password}
            />
          </div>

          <div className="card-actions">
            <button className="button" disabled={isSubmitting} type="submit">
              {isSubmitting ? 'Signing in...' : 'Login'}
            </button>
            {isAdminPortal ? (
              <Link className="button-outline" to="/login">
                Customer sign-in
              </Link>
            ) : (
              <Link className="button-outline" to="/catalog">
                Continue as guest
              </Link>
            )}
          </div>
        </form>

        <div className="stack">
          {isAdminPortal ? (
            <>
              <span className="signal">Admin: admin@ecom.local / Admin123!</span>
              <Link className="ghost-link portal-switch-link" to="/catalog">
                Open customer storefront
              </Link>
            </>
          ) : (
            <>
              <span className="signal">Customer: demo@ecom.local / Demo123!</span>
              <Link className="ghost-link portal-switch-link" to="/admin/login">
                Admin sign-in
              </Link>
            </>
          )}
        </div>
      </section>
    </div>
  )
}

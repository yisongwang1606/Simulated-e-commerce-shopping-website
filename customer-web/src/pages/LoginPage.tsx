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

const customerCredentials: LoginInput = {
  username: 'demo@ecom.local',
  password: 'Demo123!',
}

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const setSession = useSessionStore((state) => state.setSession)

  const [form, setForm] = useState<LoginInput>(customerCredentials)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsSubmitting(true)
    setErrorMessage('')

    try {
      const payload = await login(form)
      if (payload.user.role !== 'CUSTOMER') {
        setErrorMessage('This storefront sign-in is reserved for customer accounts.')
        return
      }

      setSession(payload)
      const state = location.state as LocationState | null
      navigate(state?.from ?? '/')
    } catch (error) {
      setErrorMessage(extractErrorMessage(error))
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="market-login-wrap">
      <section className="market-login-card stack-lg">
        <SectionHeading
          description="Use the seeded customer account to unlock cart, checkout, order history, refunds, and support."
          eyebrow="Customer sign-in"
          title="Sign in to continue shopping"
        />

        <div className="market-login-note">
          <strong>Marketplace demo account</strong>
          <span>demo@ecom.local / Demo123!</span>
        </div>

        {errorMessage ? <div className="message error">{errorMessage}</div> : null}

        <form className="form-grid compact" onSubmit={(event) => void handleSubmit(event)}>
          <div className="field">
            <label htmlFor="username">Username or email</label>
            <input
              id="username"
              onChange={(event) =>
                setForm((current) => setObjectField(current, 'username', event.target.value))
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
                setForm((current) => setObjectField(current, 'password', event.target.value))
              }
              required
              type="password"
              value={form.password}
            />
          </div>

          <div className="card-actions">
            <button className="button" disabled={isSubmitting} type="submit">
              {isSubmitting ? 'Signing in...' : 'Sign in'}
            </button>
            <Link className="button-outline" to="/catalog">
              Continue browsing
            </Link>
          </div>
        </form>

        <a
          className="ghost-link market-login-admin-link"
          href="http://127.0.0.1:4174/login"
          rel="noreferrer"
          target="_blank"
        >
          Open the admin portal
        </a>
      </section>
    </div>
  )
}

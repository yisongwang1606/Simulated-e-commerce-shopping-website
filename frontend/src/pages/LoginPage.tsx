import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'

import { login } from '../api/auth'
import type { LoginInput } from '../api/contracts'
import { useSessionStore } from '../store/sessionStore'
import { extractErrorMessage } from '../shared/error'
import { SectionHeading } from '../shared/ui/SectionHeading'

interface LocationState {
  from?: string
}

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const setSession = useSessionStore((state) => state.setSession)

  const [form, setForm] = useState<LoginInput>({
    username: 'Jack@example.com',
    password: '123456',
  })
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsSubmitting(true)
    setErrorMessage('')

    try {
      const payload = await login(form)
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
    <div className="login-wrap">
      <section className="login-card stack-lg">
        <SectionHeading
          description="This screen stores the JWT locally and unlocks cart, orders, and admin routes without any mocked auth state."
          eyebrow="Authentication"
          title="Sign in to the simulated storefront"
        />

        {errorMessage ? <div className="message error">{errorMessage}</div> : null}

        <form className="form-grid compact" onSubmit={(event) => void handleSubmit(event)}>
          <div className="field">
            <label htmlFor="username">Username or email</label>
            <input
              id="username"
              onChange={(event) =>
                setForm((current) => ({ ...current, username: event.target.value }))
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
                setForm((current) => ({ ...current, password: event.target.value }))
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
            <Link className="button-outline" to="/catalog">
              Continue as guest
            </Link>
          </div>
        </form>

        <div className="stack">
          <span className="signal">Customer: Jack@example.com / 123456</span>
          <span className="signal">Admin: Admin@example.com / 123456</span>
        </div>
      </section>
    </div>
  )
}

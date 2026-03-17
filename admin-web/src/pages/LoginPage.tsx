import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'

import { login } from '../api/auth'
import type { LoginInput } from '../api/contracts'
import { extractErrorMessage } from '../shared/error'
import { setObjectField } from '../shared/state'
import { SectionHeading } from '../shared/ui/SectionHeading'
import { useSessionStore } from '../store/sessionStore'

interface LocationState {
  from?: string
}

const adminCredentials: LoginInput = {
  username: 'admin@ecom.local',
  password: 'Admin123!',
}

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const setSession = useSessionStore((state) => state.setSession)
  const [form, setForm] = useState<LoginInput>(adminCredentials)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault()
    setIsSubmitting(true)
    setErrorMessage('')

    try {
      const payload = await login(form)
      if (payload.user.role !== 'ADMIN') {
        setErrorMessage('This portal only accepts admin accounts.')
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
    <div className="login-wrap admin-login-wrap">
      <section className="login-card stack-lg admin-login-card">
        <SectionHeading
          description="Sign in to manage orders, refunds, support tickets, and catalog operations."
          eyebrow="Admin authentication"
          title="Sign in to the operations portal"
        />

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
          </div>
        </form>

        <div className="stack">
          <span className="signal">admin@ecom.local / Admin123!</span>
          <a className="ghost-link portal-switch-link" href="http://127.0.0.1:4173/login" rel="noreferrer" target="_blank">
            Open customer storefront
          </a>
        </div>
      </section>
    </div>
  )
}

import { useEffect, useEffectEvent } from 'react'
import { RouterProvider } from 'react-router-dom'

import { fetchCurrentUser } from './api/auth'
import { router } from './app/router'
import { useSessionStore } from './store/sessionStore'

function App() {
  const token = useSessionStore((state) => state.token)
  const user = useSessionStore((state) => state.user)
  const setUser = useSessionStore((state) => state.setUser)
  const clearSession = useSessionStore((state) => state.clearSession)

  const bootstrapSession = useEffectEvent(async () => {
    if (!token || user) {
      return
    }

    try {
      const currentUser = await fetchCurrentUser()
      setUser(currentUser)
    } catch {
      clearSession()
    }
  })

  useEffect(() => {
    if (!token || user) {
      return
    }

    void bootstrapSession()
  }, [token, user])

  return <RouterProvider router={router} />
}

export default App

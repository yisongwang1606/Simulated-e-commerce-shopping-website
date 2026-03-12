import { useEffect } from 'react'
import { RouterProvider } from 'react-router-dom'

import { fetchCurrentUser } from './api/auth'
import { router } from './app/router'
import { useSessionStore } from './store/sessionStore'

function App() {
  const token = useSessionStore((state) => state.token)
  const user = useSessionStore((state) => state.user)
  const setUser = useSessionStore((state) => state.setUser)
  const clearSession = useSessionStore((state) => state.clearSession)

  useEffect(() => {
    if (!token || user) {
      return
    }

    const bootstrapSession = async () => {
      try {
        const currentUser = await fetchCurrentUser()
        setUser(currentUser)
      } catch {
        clearSession()
      }
    }

    void bootstrapSession()
  }, [clearSession, setUser, token, user])

  return <RouterProvider router={router} />
}

export default App

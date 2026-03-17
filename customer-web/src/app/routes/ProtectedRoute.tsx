import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { useSessionStore } from '../../store/sessionStore'

export function ProtectedRoute() {
  const token = useSessionStore((state) => state.token)
  const location = useLocation()

  if (!token) {
    return <Navigate replace state={{ from: location.pathname }} to="/login" />
  }

  return <Outlet />
}

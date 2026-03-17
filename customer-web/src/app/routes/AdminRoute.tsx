import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { isAdmin, useSessionStore } from '../../store/sessionStore'

export function AdminRoute() {
  const token = useSessionStore((state) => state.token)
  const user = useSessionStore((state) => state.user)
  const location = useLocation()

  if (!token) {
    return <Navigate replace state={{ from: location.pathname }} to="/admin/login" />
  }

  if (!isAdmin(user)) {
    return <Navigate replace to="/" />
  }

  return <Outlet />
}

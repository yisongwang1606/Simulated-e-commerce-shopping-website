import { createBrowserRouter } from 'react-router-dom'

import { AdminRoute } from './routes/AdminRoute'
import { AdminShell } from './shell/AdminShell'
import { AdminPage } from '../pages/AdminPage'
import { LoginPage } from '../pages/LoginPage'
import { NotFoundPage } from '../pages/NotFoundPage'

export const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/',
    element: <AdminRoute />,
    children: [
      {
        element: <AdminShell />,
        children: [
          {
            index: true,
            element: <AdminPage />,
          },
          {
            path: '*',
            element: <NotFoundPage />,
          },
        ],
      },
    ],
  },
])

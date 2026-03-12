import { createBrowserRouter } from 'react-router-dom'

import { AdminRoute } from './routes/AdminRoute'
import { ProtectedRoute } from './routes/ProtectedRoute'
import { AppShell } from './shell/AppShell'
import { AdminPage } from '../pages/AdminPage'
import { CartPage } from '../pages/CartPage'
import { CatalogPage } from '../pages/CatalogPage'
import { HomePage } from '../pages/HomePage'
import { LoginPage } from '../pages/LoginPage'
import { NotFoundPage } from '../pages/NotFoundPage'
import { OrdersPage } from '../pages/OrdersPage'
import { ProductDetailPage } from '../pages/ProductDetailPage'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <AppShell />,
    children: [
      {
        index: true,
        element: <HomePage />,
      },
      {
        path: 'catalog',
        element: <CatalogPage />,
      },
      {
        path: 'products/:productId',
        element: <ProductDetailPage />,
      },
      {
        path: 'login',
        element: <LoginPage />,
      },
      {
        element: <ProtectedRoute />,
        children: [
          {
            path: 'cart',
            element: <CartPage />,
          },
          {
            path: 'orders',
            element: <OrdersPage />,
          },
        ],
      },
      {
        element: <AdminRoute />,
        children: [
          {
            path: 'admin',
            element: <AdminPage />,
          },
        ],
      },
      {
        path: '*',
        element: <NotFoundPage />,
      },
    ],
  },
])

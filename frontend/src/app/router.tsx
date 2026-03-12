import { createBrowserRouter } from 'react-router-dom'

import { AdminRoute } from './routes/AdminRoute'
import { ProtectedRoute } from './routes/ProtectedRoute'
import { AdminShell } from './shell/AdminShell'
import { CustomerShell } from './shell/CustomerShell'
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
    element: <CustomerShell />,
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
        element: <LoginPage portal="customer" />,
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
        path: '*',
        element: <NotFoundPage />,
      },
    ],
  },
  {
    path: '/admin/login',
    element: <LoginPage portal="admin" />,
  },
  {
    path: '/admin',
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

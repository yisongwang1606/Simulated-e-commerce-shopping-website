export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface PagedResponse<T> {
  items: T[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export interface UserProfile {
  id: number
  username: string
  email: string
  role: string
  createdAt: string
}

export interface AuthPayload {
  token: string
  expiresAt: string
  user: UserProfile
}

export interface LoginInput {
  username: string
  password: string
}

export interface RegisterInput {
  username: string
  email: string
  password: string
}

export interface Product {
  id: number
  name: string
  price: number
  stock: number
  category: string
  description: string
  createdAt: string
}

export interface PopularProduct {
  product: Product
  score: number
}

export interface CartItemInput {
  productId: number
  quantity: number
}

export interface UpdateCartItemInput {
  quantity: number
}

export interface CartItem {
  productId: number
  name: string
  category: string
  price: number
  quantity: number
  subtotal: number
}

export interface Cart {
  items: CartItem[]
  totalQuantity: number
  totalPrice: number
}

export interface OrderItem {
  productId: number
  productName: string
  quantity: number
  price: number
  subtotal: number
}

export interface Order {
  id: number
  userId: number
  username: string
  totalPrice: number
  status: string
  createdAt: string
  items: OrderItem[]
}

export interface ProductPayload {
  name: string
  price: number
  stock: number
  category: string
  description: string
}

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
  sku: string
  name: string
  brand: string | null
  price: number
  stock: number
  safetyStock: number
  category: string
  status: string
  taxClass: string
  weightKg: number | null
  leadTimeDays: number
  featured: boolean
  description: string
  createdAt: string
}

export interface PopularProduct {
  product: Product
  score: number
}

export interface ProductPayload {
  sku?: string
  name: string
  brand?: string
  price: number
  costPrice?: number | null
  stock: number
  safetyStock?: number
  category: string
  status?: string
  taxClass?: string
  weightKg?: number | null
  leadTimeDays?: number
  featured?: boolean
  description: string
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

export interface CustomerAddress {
  id: number
  addressLabel: string
  receiverName: string
  phone: string
  line1: string
  line2: string | null
  city: string
  province: string
  postalCode: string
  isDefault: boolean
  createdAt: string
  updatedAt: string
}

export interface CustomerAddressInput {
  addressLabel: string
  receiverName: string
  phone: string
  line1: string
  line2: string
  city: string
  province: string
  postalCode: string
  isDefault: boolean
}

export interface OrderAddressSnapshot {
  receiverName: string
  phone: string
  line1: string
  line2: string | null
  city: string
  province: string
  postalCode: string
}

export interface OrderItem {
  productId: number
  sku: string | null
  productName: string
  quantity: number
  price: number
  subtotal: number
}

export interface Order {
  id: number
  orderNo: string
  userId: number
  username: string
  subtotalAmount: number
  taxAmount: number
  shippingAmount: number
  discountAmount: number
  totalPrice: number
  shippingAddress: OrderAddressSnapshot | null
  status: string
  statusNote: string | null
  createdAt: string
  statusUpdatedAt: string | null
  updatedAt: string | null
  items: OrderItem[]
}

export interface CreateOrderInput {
  addressId?: number
}

export interface Shipment {
  id: number
  orderId: number
  orderNo: string
  shipmentNo: string
  carrierCode: string
  trackingNo: string
  shipmentStatus: string
  statusNote: string | null
  createdAt: string
  shippedAt: string | null
  deliveredAt: string | null
  updatedAt: string
}

export interface RefundRequest {
  id: number
  orderId: number
  orderNo: string
  refundStatus: string
  reason: string
  reviewNote: string | null
  requestedByUserId: number
  requestedByUsername: string
  reviewedByUserId: number | null
  reviewedByUsername: string | null
  requestedAt: string
  reviewedAt: string | null
}

export interface RefundRequestInput {
  reason: string
}

export interface RefundReviewInput {
  decision: 'APPROVED' | 'REJECTED'
  reviewNote?: string
}

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

export interface OrderTag {
  id: number
  tagCode: string
  displayName: string
  tagGroup: string
  tone: string
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
  tags: OrderTag[]
}

export interface CreateOrderInput {
  addressId?: number
}

export interface PaymentTransaction {
  id: number
  orderId: number
  orderNo: string
  paymentMethod: string
  paymentStatus: string
  transactionRef: string
  providerCode: string
  providerEventId: string | null
  providerReference: string | null
  amount: number
  clientSecret: string | null
  note: string | null
  createdAt: string
  paidAt: string | null
  updatedAt: string
}

export interface CustomerStripePaymentIntentInput {
  note?: string
}

export interface StripePaymentReconcileInput {
  providerReference: string
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

export interface RefundSummary {
  totalRequests: number
  requestedCount: number
  approvedCount: number
  rejectedCount: number
  settledCount: number
  requestedAmount: number
  approvedAmount: number
  settledAmount: number
}

export interface DashboardMetricBreakdown {
  code: string
  label: string
  count: number
}

export interface LowStockAlert {
  productId: number
  sku: string
  name: string
  category: string
  stock: number
  safetyStock: number
  shortage: number
}

export interface AdminDashboardSummary {
  totalOrders: number
  ordersCreatedToday: number
  fulfillmentInFlight: number
  capturedRevenue30Days: number
  averageOrderValue30Days: number
  activeRefundCases: number
  openSupportTickets: number
  urgentSupportTickets: number
  activeCatalogProducts: number
  featuredProducts: number
  lowStockProducts: number
  orderStatusBreakdown: DashboardMetricBreakdown[]
  supportStatusBreakdown: DashboardMetricBreakdown[]
  lowStockAlerts: LowStockAlert[]
}

export interface SupportTicket {
  id: number
  orderId: number
  orderNo: string
  ticketNo: string
  ticketStatus: string
  priority: string
  category: string
  subject: string
  customerMessage: string
  latestNote: string | null
  assignedTeam: string | null
  assignedToUsername: string | null
  resolutionNote: string | null
  requestedByUserId: number
  requestedByUsername: string
  createdAt: string
  updatedAt: string
  resolvedAt: string | null
}

export interface SupportTicketInput {
  category: string
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT'
  subject: string
  customerMessage: string
}

export interface SupportTicketUpdateInput {
  status?: 'OPEN' | 'IN_PROGRESS' | 'WAITING_ON_CUSTOMER' | 'RESOLVED' | 'CLOSED'
  assignedTeam?: string
  assignedToUsername?: string
  latestNote?: string
  resolutionNote?: string
}

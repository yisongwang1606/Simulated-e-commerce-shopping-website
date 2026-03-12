import { PortalShell, type PortalNavItem } from './PortalShell'

const customerNavItems: PortalNavItem[] = [
  { kind: 'route', label: 'Home', to: '/', end: true },
  { kind: 'route', label: 'Catalog', to: '/catalog' },
  { kind: 'route', label: 'Cart', to: '/cart' },
  { kind: 'route', label: 'Orders', to: '/orders' },
]

export function CustomerShell() {
  return (
    <PortalShell
      badge="EC"
      footerPrimary="Customer storefront for browsing, checkout, delivery follow-up, and after-sales service."
      footerSecondary="React 19 | Vite 7 | TypeScript 5.9"
      guestLabel="Guest browsing"
      loginPath="/login"
      logoutRedirect="/"
      navItems={customerNavItems}
      pageClassName="customer-shell"
      portalLabel="Customer storefront"
      subtitle="Browse the catalog, manage the cart, place orders, and track after-sales requests."
      title="Northline Commerce Storefront"
    />
  )
}

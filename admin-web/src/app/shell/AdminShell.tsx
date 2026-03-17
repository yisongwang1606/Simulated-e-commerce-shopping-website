import { PortalShell, type PortalNavItem } from './PortalShell'

const adminNavItems: PortalNavItem[] = [
  { kind: 'anchor', label: 'Overview', href: '/#overview' },
  { kind: 'anchor', label: 'Workbench', href: '/#workbench' },
  { kind: 'anchor', label: 'Catalog', href: '/#catalog-intake' },
]

export function AdminShell() {
  return (
    <PortalShell
      badge="OPS"
      footerPrimary="Admin operations portal for order governance, refund review, support handling, and catalog control."
      footerSecondary="Internal workspace for daily commerce operations"
      guestLabel="Admin sign-in required"
      loginPath="/login"
      logoutRedirect="/login"
      navItems={adminNavItems}
      pageClassName="admin-shell"
      statusItems={[
        'Order operations',
        'Refund desk',
        'Support queue',
      ]}
      subtitle="Handle orders, returns, service requests, and catalog intake from one internal control surface."
      title="Northline Operations Portal"
      topbarClassName="admin-topbar"
    />
  )
}

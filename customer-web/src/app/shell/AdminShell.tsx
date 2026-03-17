import { PortalShell, type PortalNavItem } from './PortalShell'

const adminNavItems: PortalNavItem[] = [
  { kind: 'anchor', label: 'Overview', href: '/admin#overview' },
  { kind: 'anchor', label: 'Workbench', href: '/admin#workbench' },
  { kind: 'anchor', label: 'Catalog', href: '/admin#catalog-intake' },
]

export function AdminShell() {
  return (
    <PortalShell
      badge="OPS"
      footerPrimary="Admin operations portal for order governance, refund review, support handling, and catalog control."
      footerSecondary="Enterprise workflow baseline on Spring Boot 4 and Java 21"
      guestLabel="Admin sign-in required"
      loginPath="/admin/login"
      logoutRedirect="/admin/login"
      navItems={adminNavItems}
      pageClassName="admin-shell"
      statusItems={[
        'Admin operations',
        '{api}',
        'Java 21 | MySQL 8.4 | Redis 7.4',
      ]}
      showSwagger
      subtitle="Run the operations desk for orders, support, refunds, and merchandising without storefront actions mixed in."
      switchLink={{ label: 'Storefront', to: '/' }}
      title="Northline Operations Portal"
      topbarClassName="admin-topbar"
    />
  )
}

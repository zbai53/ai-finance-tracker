import { useState } from 'react';
import { NavLink, useLocation } from 'react-router-dom';
import type { ReactNode } from 'react';
import { useAuth } from '../context/AuthContext';

interface NavItem {
  label: string;
  path: string;
  icon: ReactNode;
}

const NAV_ITEMS: NavItem[] = [
  {
    label: 'Dashboard',
    path: '/dashboard',
    icon: (
      <svg className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
        <path d="M10.707 2.293a1 1 0 00-1.414 0l-7 7a1 1 0 001.414 1.414L4 10.414V17a1 1 0 001 1h2a1 1 0 001-1v-2a1 1 0 011-1h2a1 1 0 011 1v2a1 1 0 001 1h2a1 1 0 001-1v-6.586l.293.293a1 1 0 001.414-1.414l-7-7z" />
      </svg>
    ),
  },
  {
    label: 'Transactions',
    path: '/transactions',
    icon: (
      <svg className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
        <path fillRule="evenodd" d="M4 4a2 2 0 00-2 2v4a2 2 0 002 2V6h10a2 2 0 00-2-2H4zm2 6a2 2 0 012-2h8a2 2 0 012 2v4a2 2 0 01-2 2H8a2 2 0 01-2-2v-4zm6 4a2 2 0 100-4 2 2 0 000 4z" clipRule="evenodd" />
      </svg>
    ),
  },
];

interface AppLayoutProps {
  children: ReactNode;
}

export function AppLayout({ children }: AppLayoutProps) {
  const { logout } = useAuth();
  const location = useLocation();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  const sidebar = (
    <div className="flex h-full flex-col">
      {/* Brand */}
      <div className="flex h-16 items-center px-5">
        <span className="text-lg font-bold text-white tracking-tight">Finance Tracker</span>
      </div>

      {/* Nav */}
      <nav className="flex-1 space-y-1 px-3 py-4">
        {NAV_ITEMS.map(({ label, path, icon }) => {
          const isActive = location.pathname === path;
          return (
            <NavLink
              key={path}
              to={path}
              onClick={() => setSidebarOpen(false)}
              className={`flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${
                isActive
                  ? 'bg-white/20 text-white'
                  : 'text-blue-100 hover:bg-white/10 hover:text-white'
              }`}
            >
              {icon}
              {label}
            </NavLink>
          );
        })}
      </nav>

      {/* Logout */}
      <div className="border-t border-white/10 px-3 py-4">
        <button
          onClick={logout}
          className="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium text-blue-100 transition-colors hover:bg-white/10 hover:text-white"
        >
          <svg className="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
            <path fillRule="evenodd" d="M3 3a1 1 0 00-1 1v12a1 1 0 102 0V4a1 1 0 00-1-1zm10.293 9.293a1 1 0 001.414 1.414l3-3a1 1 0 000-1.414l-3-3a1 1 0 10-1.414 1.414L14.586 9H7a1 1 0 100 2h7.586l-1.293 1.293z" clipRule="evenodd" />
          </svg>
          Logout
        </button>
      </div>
    </div>
  );

  return (
    <div className="flex h-screen bg-gray-50">
      {/* ── Desktop sidebar (fixed) ── */}
      <aside className="hidden w-60 flex-shrink-0 bg-blue-700 lg:block">
        {sidebar}
      </aside>

      {/* ── Mobile sidebar (overlay) ── */}
      {sidebarOpen && (
        <>
          {/* Backdrop */}
          <div
            className="fixed inset-0 z-20 bg-black/40 lg:hidden"
            onClick={() => setSidebarOpen(false)}
          />
          {/* Panel */}
          <aside className="fixed inset-y-0 left-0 z-30 w-60 bg-blue-700 lg:hidden">
            {sidebar}
          </aside>
        </>
      )}

      {/* ── Main content ── */}
      <div className="flex flex-1 flex-col overflow-hidden">
        {/* Mobile topbar */}
        <header className="flex h-14 items-center border-b border-gray-200 bg-white px-4 lg:hidden">
          <button
            onClick={() => setSidebarOpen(true)}
            className="rounded-md p-1.5 text-gray-500 hover:bg-gray-100"
            aria-label="Open menu"
          >
            <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={2}>
              <path strokeLinecap="round" strokeLinejoin="round" d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
          <span className="ml-3 text-base font-semibold text-gray-700">Finance Tracker</span>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-y-auto">
          {children}
        </main>
      </div>
    </div>
  );
}

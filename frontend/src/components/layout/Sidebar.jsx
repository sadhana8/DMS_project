import { NavLink } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { useCompany } from "@/context/CompanyContext";
import { useQuery } from "@tanstack/react-query";
import { approvalsApi } from "@/api/approvals";
import { notificationsApi } from "@/api/notifications";
import clsx from "clsx";
import {
  HiOutlineHome,
  HiOutlineDocumentText,
  HiOutlineUsers,
  HiOutlineShieldCheck,
  HiOutlineBell,
  HiOutlineCog,
  HiOutlineUserGroup,
  HiOutlineKey,
  HiOutlineSearch,
  HiOutlineChevronLeft,
  HiOutlineChevronRight,
  HiOutlineOfficeBuilding,
} from "react-icons/hi";

export default function Sidebar({ open, onClose, collapsed, onToggleCollapse }) {
  const {
    user,
    isAdmin,
    isManager,
    logout,
  } = useAuth();

  const { company } = useCompany();

  const { data: pendingApprovals = 0 } = useQuery({
    queryKey: ["approval-count"],
    queryFn: approvalsApi.count,
    enabled: isAdmin(),
    refetchInterval: 60_000,
  });

  const { data: changeReqsCount } = useQuery({
    queryKey: ["change-requests-count"],
    queryFn: () =>
      import("@/api/profileChanges").then((m) =>
        m.profileChangesApi.pendingCount(),
      ),
    enabled: isManager() || isAdmin(),
    refetchInterval: 60_000,
  });
  const pendingChangeRequests = changeReqsCount?.pending ?? 0;

  const { data: unreadNotif = 0 } = useQuery({
    queryKey: ["notif-count"],
    queryFn: notificationsApi.unreadCount,
    refetchInterval: 30_000,
  });

  const navItems = [
    { to: "/dashboard", label: "Dashboard", icon: HiOutlineHome, show: true },
    {
      to: "/documents",
      label: "Documents",
      icon: HiOutlineDocumentText,
      show: true,
    },
    {
      to: "/documents/search/advanced",
      label: "Advanced search",
      icon: HiOutlineSearch,
      show: true,
    },
    {
      to: "/users",
      label: "Users",
      icon: HiOutlineUsers,
      show: isManager() || isAdmin(),
    },
    {
      to: "/hr/change-requests",
      label: "Change requests",
      icon: HiOutlineDocumentText,
      show: isManager() || isAdmin(),
      badge: pendingChangeRequests,
    },
    {
      to: "/approvals",
      label: "Approvals",
      icon: HiOutlineUserGroup,
      show: isAdmin(),
      badge: pendingApprovals,
    },
    { to: "/admin/roles", label: "Roles", icon: HiOutlineKey, show: isAdmin() },
    {
      to: "/audit",
      label: "Audit trail",
      icon: HiOutlineShieldCheck,
      show: isAdmin(),
    },
    { to: "/settings", label: "Settings", icon: HiOutlineCog, show: isAdmin() },
    {
      to: "/notifications",
      label: "Notifications",
      icon: HiOutlineBell,
      show: true,
      badge: unreadNotif,
    },
  ].filter((i) => i.show);

  const initials = user
    ? `${user.firstName?.charAt(0) ?? ""}${user.lastName?.charAt(0) ?? ""}`.toUpperCase()
    : "?";

  const companyName = company?.company_name || "DocVault";
  const logoUrl = company?.company_logo_url || "";

  return (
    <>
      {/* Mobile backdrop */}
      {open && (
        <div
          className="fixed inset-0 bg-black/40 z-30 lg:hidden"
          onClick={onClose}
        />
      )}

      <aside
        className={clsx(
          "fixed top-0 left-0 h-full bg-white border-r border-surface-200 z-40 flex flex-col transition-all duration-300 dark:bg-gray-900 dark:border-gray-800",
          "lg:relative lg:translate-x-0 lg:z-auto",
          collapsed ? "w-16" : "w-64",
          open ? "translate-x-0" : "-translate-x-full",
        )}
      >
        {/* Logo / Company */}
        <div className={clsx(
          "flex items-center border-b border-surface-100 dark:border-gray-800 relative",
          collapsed ? "justify-center px-2 py-4" : "gap-3 px-5 py-4"
        )}>
          <div className="w-8 h-8 rounded-lg overflow-hidden flex items-center justify-center flex-shrink-0 bg-primary-100">
            {logoUrl ? (
              <img src={logoUrl} alt={companyName} className="w-full h-full object-contain" />
            ) : (
              <HiOutlineOfficeBuilding className="w-5 h-5 text-primary-600" />
            )}
          </div>
          {!collapsed && (
            <div className="flex-1 min-w-0">
              <p className="font-semibold text-surface-900 dark:text-gray-100 text-sm leading-tight truncate">
                {companyName}
              </p>
            </div>
          )}

          {/* Collapse toggle — desktop only */}
          <button
            onClick={onToggleCollapse}
            className={clsx(
              "hidden lg:flex items-center justify-center w-5 h-5 rounded-full bg-white dark:bg-gray-800 border border-surface-200 dark:border-gray-700 text-surface-500 dark:text-gray-400 hover:text-surface-800 dark:hover:text-gray-100 shadow-sm transition-colors absolute -right-2.5 top-1/2 -translate-y-1/2 z-10",
            )}
            title={collapsed ? "Expand sidebar" : "Collapse sidebar"}
          >
            {collapsed
              ? <HiOutlineChevronRight className="w-3 h-3" />
              : <HiOutlineChevronLeft className="w-3 h-3" />
            }
          </button>
        </div>

        {/* Nav */}
        <nav className="flex-1 overflow-y-auto px-2 py-4 space-y-0.5">
          {navItems.map(({ to, label, icon: Icon, badge }) => (
            <NavLink
              key={to}
              to={to}
              onClick={() => onClose?.()}
              title={collapsed ? label : undefined}
              className={({ isActive }) =>
                clsx("sidebar-link", isActive && "active", collapsed && "justify-center px-2")
              }
            >
              <Icon className="w-4 h-4 flex-shrink-0" />
              {!collapsed && <span className="flex-1">{label}</span>}
              {!collapsed && badge > 0 && (
                <span className="w-5 h-5 bg-red-500 text-white rounded-full text-[10px] font-bold flex items-center justify-center flex-shrink-0">
                  {badge > 9 ? "9+" : badge}
                </span>
              )}
              {collapsed && badge > 0 && (
                <span className="absolute top-1 right-1 w-2 h-2 bg-red-500 rounded-full" />
              )}
            </NavLink>
          ))}
        </nav>

        {/* User card */}
        {!collapsed && (
          <div className="border-t border-surface-100 dark:border-gray-800 p-3">
            <NavLink
              to="/profile"
              className="flex items-center gap-3 px-3 py-2 rounded-xl hover:bg-surface-50 dark:hover:bg-gray-800 transition-colors"
            >
              <div className="w-8 h-8 rounded-full bg-primary-100 dark:bg-primary-900/40 text-primary-700 dark:text-primary-400 flex items-center justify-center text-xs font-bold flex-shrink-0">
                {initials}
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-sm font-medium text-surface-800 dark:text-gray-200 truncate">
                  {user?.firstName} {user?.lastName}
                </p>
                <p className="text-xs text-surface-400 dark:text-gray-500 truncate">{user?.email}</p>
              </div>
            </NavLink>
          </div>
        )}
        {collapsed && (
          <div className="border-t border-surface-100 dark:border-gray-800 p-2">
            <NavLink
              to="/profile"
              className="flex items-center justify-center px-2 py-2 rounded-xl hover:bg-surface-50 dark:hover:bg-gray-800 transition-colors"
              title={`${user?.firstName} ${user?.lastName}`}
            >
              <div className="w-8 h-8 rounded-full bg-primary-100 dark:bg-primary-900/40 text-primary-700 dark:text-primary-400 flex items-center justify-center text-xs font-bold">
                {initials}
              </div>
            </NavLink>
          </div>
        )}
      </aside>
    </>
  );
}

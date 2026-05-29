import { NavLink } from "react-router-dom";
import { useAuth } from "@/context/AuthContext";
import { useQuery } from "@tanstack/react-query";
import { approvalsApi } from "@/api/approvals";
import { notificationsApi } from "@/api/notifications";
import profileImage from "../../assets/image.png";
import clsx from "clsx";
import {
  HiOutlineHome,
  HiOutlineDocumentText,
  HiOutlineUsers,
  HiOutlineShieldCheck,
  HiOutlineBell,
  HiOutlineCog,
  HiOutlineChartBar,
  HiOutlineUserGroup,
  HiOutlineViewGrid,
  HiOutlineKey,
  HiOutlineSearch,
} from "react-icons/hi";

export default function Sidebar({ open, onClose }) {
  const {
    user,
    isAdmin,
    isManager,
    isEditor,
    isFinance,
    isLegal,
    isReviewer,
    hasManagerRole,
    logout,
  } = useAuth();

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
    // HR/Manager can see users directory
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
    // Admin-only items
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
    // Notifications — all users
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
          "fixed top-0 left-0 h-full w-64 bg-white border-r border-surface-200 z-40 flex flex-col transition-transform duration-300",
          "lg:relative lg:translate-x-0 lg:z-auto",
          open ? "translate-x-0" : "-translate-x-full",
        )}
      >
        {/* Logo */}
        <div className="flex items-center gap-3 px-5 py-4 border-b border-surface-100">
          <div className="w-8 h-8 rounded-full overflow-hidden flex items-center justify-center flex-shrink-0">
            <img
              src={profileImage}
              alt="Profile"
              className="w-full h-full object-cover"
            />
          </div>
          <div>
            <p className="font-semibold text-surface-900 text-sm">
              Magnus Consulting Group PVT. LTD.
            </p>
          </div>
        </div>

        {/* Nav */}
        <nav className="flex-1 overflow-y-auto px-3 py-4 space-y-0.5">
          {navItems.map(({ to, label, icon: Icon, badge }) => (
            <NavLink
              key={to}
              to={to}
              onClick={() => onClose?.()}
              className={({ isActive }) =>
                clsx("sidebar-link", isActive && "active")
              }
            >
              <Icon className="w-4 h-4 flex-shrink-0" />
              <span className="flex-1">{label}</span>
              {badge > 0 && (
                <span className="w-5 h-5 bg-red-500 text-white rounded-full text-[10px] font-bold flex items-center justify-center flex-shrink-0">
                  {badge > 9 ? "9+" : badge}
                </span>
              )}
            </NavLink>
          ))}
        </nav>

        {/* User card */}
        <div className="border-t border-surface-100 p-3">
          <NavLink
            to="/profile"
            className="flex items-center gap-3 px-3 py-2 rounded-xl hover:bg-surface-50 transition-colors"
          >
            <div className="w-8 h-8 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center text-xs font-bold flex-shrink-0">
              {initials}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-sm font-medium text-surface-800 truncate">
                {user?.firstName} {user?.lastName}
              </p>
              <p className="text-xs text-surface-400 truncate">{user?.email}</p>
            </div>
          </NavLink>
        </div>
      </aside>
    </>
  );
}

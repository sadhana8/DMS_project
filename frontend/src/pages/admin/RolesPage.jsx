import { useQuery } from '@tanstack/react-query'
import { rolesApi } from '@/api/roles'
import { getRoleBadge, getErrorMessage } from '@/utils/helpers'
import Spinner from '@/components/common/Spinner'
import {
  HiOutlineShieldCheck, HiOutlineCheck, HiOutlineX,
  HiOutlineExclamation, HiOutlineUsers,
} from 'react-icons/hi'
import clsx from 'clsx'

/**
 * Admin view of the role catalog and the role/permission matrix. Read-only:
 * roles are a fixed enum on the backend. Role *assignment* to users is done
 * from the Users page via `Users.updateRoles`.
 */
export default function RolesPage() {
  const { data: roles, isLoading: rolesLoading, error: rolesErr } = useQuery({
    queryKey: ['roles'],
    queryFn:  rolesApi.list,
  })

  const { data: matrix, isLoading: matrixLoading, error: matrixErr } = useQuery({
    queryKey: ['roles-permissions-matrix'],
    queryFn:  rolesApi.permissionsMatrix,
  })

  const loading = rolesLoading || matrixLoading
  const err     = rolesErr || matrixErr

  return (
    <div className="animate-fade-in space-y-6">
      <div>
        <div className="flex items-center gap-2 mb-1">
          <HiOutlineShieldCheck className="w-5 h-5 text-primary-600" />
          <h1 className="page-title">Roles & Permissions</h1>
        </div>
        <p className="page-subtitle">
          The system uses four organizational roles. Privilege ordering: Admin &gt; HR &gt; Account &gt; Employee.
          Assign roles to users from the Users page.
        </p>
      </div>

      {loading ? (
        <div className="flex justify-center py-16"><Spinner size="lg" /></div>
      ) : err ? (
        <div className="card p-6 text-red-600 text-sm">{getErrorMessage(err)}</div>
      ) : (
        <>
          {/* Role cards */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
            {(roles ?? []).map(r => {
              const badge = getRoleBadge(r.name)
              return (
                <div key={r.id} className="card p-5">
                  <div className="flex items-center justify-between mb-3">
                    <span className={clsx('badge', badge.color)}>{badge.label}</span>
                    <HiOutlineUsers className="w-4 h-4 text-surface-400 dark:text-gray-500" />
                  </div>
                  <h3 className="font-semibold text-surface-900 dark:text-gray-100 mb-0.5">{r.displayName}</h3>
                  <p className="text-xs text-surface-400 dark:text-gray-500 font-mono">{r.name}</p>
                  <div className="mt-4 pt-4 border-t border-surface-100 dark:border-gray-800">
                    <p className="text-xs text-surface-500 dark:text-gray-400 mb-2">Key permissions</p>
                    <ul className="space-y-1">
                      {Object.entries(r.permissions ?? {})
                        .filter(([, v]) => v === 'yes')
                        .slice(0, 4)
                        .map(([k]) => (
                          <li key={k} className="flex items-center gap-1.5 text-xs text-surface-700 dark:text-gray-300">
                            <HiOutlineCheck className="w-3.5 h-3.5 text-green-600" />
                            {prettyPermName(k)}
                          </li>
                        ))}
                    </ul>
                  </div>
                </div>
              )
            })}
          </div>

          {/* Permission matrix */}
          {matrix && (
            <div className="card overflow-hidden">
              <div className="p-5 border-b border-surface-100 dark:border-gray-800">
                <h2 className="section-title !mb-0">Permission matrix</h2>
                <p className="text-xs text-surface-500 dark:text-gray-400 mt-1">
                  Full breakdown of what each role can do. "Limited" means scoped by department or document permissions.
                </p>
              </div>
              <div className="table-wrapper !rounded-none !border-none">
                <table className="table">
                  <thead>
                    <tr>
                      <th>Capability</th>
                      {Object.keys(matrix).map(role => {
                        const b = getRoleBadge(role)
                        return (
                          <th key={role} className="text-center">
                            <span className={clsx('badge', b.color)}>{b.label}</span>
                          </th>
                        )
                      })}
                    </tr>
                  </thead>
                  <tbody>
                    {capabilityKeys(matrix).map(cap => (
                      <tr key={cap}>
                        <td className="font-medium text-surface-800 dark:text-gray-200">{prettyPermName(cap)}</td>
                        {Object.keys(matrix).map(role => (
                          <td key={role} className="text-center">
                            <PermCell value={matrix[role]?.[cap]} />
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          <div className="card p-5 bg-amber-50 border-amber-200">
            <div className="flex gap-3">
              <HiOutlineExclamation className="w-5 h-5 text-amber-600 flex-shrink-0 mt-0.5" />
              <div className="text-sm text-amber-900">
                <p className="font-medium mb-1">Roles are system-defined</p>
                <p className="text-amber-800">
                  The four roles are baked into the platform. Adding new roles requires a backend change.
                  To grant a user permissions, assign them one or more existing roles from the Users page.
                </p>
              </div>
            </div>
          </div>
        </>
      )}
    </div>
  )
}

function PermCell({ value }) {
  if (value === 'yes') return <HiOutlineCheck className="w-5 h-5 text-green-600 inline" />
  if (value === 'limited') return <span className="badge badge-yellow">Limited</span>
  return <HiOutlineX className="w-5 h-5 text-surface-300 dark:text-gray-600 inline" />
}

function prettyPermName(key) {
  return key.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
}

/** Stable order for capabilities so the matrix doesn't reorder on refetch. */
function capabilityKeys(matrix) {
  const preferred = [
    'upload_docs', 'delete_docs', 'manage_users',
    'view_salary_docs', 'view_own_docs', 'manage_roles',
    'approve_workflows', 'view_audit_logs',
  ]
  const all = new Set()
  Object.values(matrix).forEach(perms => Object.keys(perms ?? {}).forEach(k => all.add(k)))
  const ordered = preferred.filter(k => all.has(k))
  const extras  = [...all].filter(k => !preferred.includes(k)).sort()
  return [...ordered, ...extras]
}

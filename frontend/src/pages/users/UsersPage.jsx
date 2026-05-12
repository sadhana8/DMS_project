import { useState } from 'react'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { usersApi } from '@/api/users'
import { getRoleBadge, timeAgo, getErrorMessage } from '@/utils/helpers'
import { Avatar, Pagination } from '@/components/common/index'
import Spinner from '@/components/common/Spinner'
import Modal from '@/components/common/Modal'
import { useAuth } from '@/context/AuthContext'
import toast from 'react-hot-toast'
import clsx from 'clsx'
import {
  HiOutlineSearch, HiOutlineUsers, HiOutlineShieldCheck,
  HiOutlineBan, HiOutlineCheckCircle, HiOutlineArchive,
  HiOutlineRefresh, HiOutlineFilter,
} from 'react-icons/hi'

const ALL_ROLES = ['ROLE_ADMIN','ROLE_HR','ROLE_ACCOUNT','ROLE_EMPLOYEE']

export default function UsersPage() {
  const qc = useQueryClient()
  const { user: me, isAdmin } = useAuth()
  const [page,           setPage]           = useState(1)
  const [search,         setSearch]         = useState('')
  const [showDeprecated, setShowDeprecated] = useState(false)
  const [roleModal,      setRoleModal]      = useState(null)
  const [deprUser,       setDeprUser]       = useState(null)
  const [deprReason,     setDeprReason]     = useState('')
  const [deprLoading,    setDeprLoading]    = useState(false)
  const [selectedRoles,  setSelectedRoles]  = useState([])

  const { data, isLoading } = useQuery({
    queryKey: showDeprecated ? ['users-deprecated', page] : ['users', page, search],
    queryFn:  showDeprecated
      ? () => usersApi.listDeprecated({ page: page - 1, size: 10 })
      : () => usersApi.list({ page: page - 1, size: 10, search: search || undefined }),
  })

  const users      = data?.content ?? []
  const totalPages = data?.totalPages ?? 1
  const total      = data?.totalElements ?? 0

  const saveRoles = async () => {
    try {
      await usersApi.updateRoles(roleModal.id, selectedRoles)
      qc.invalidateQueries({ queryKey: ['users'] })
      toast.success('Roles updated')
      setRoleModal(null)
    } catch (e) { toast.error(getErrorMessage(e)) }
  }

  const toggleActive = async (u) => {
    try {
      u.isActive ? await usersApi.deactivate(u.id) : await usersApi.activate(u.id)
      qc.invalidateQueries({ queryKey: ['users'] })
      toast.success(`User ${u.isActive ? 'deactivated' : 'activated'}`)
    } catch (e) { toast.error(getErrorMessage(e)) }
  }

  const handleDeprecate = async () => {
    setDeprLoading(true)
    try {
      await usersApi.deprecate(deprUser.id, deprReason)
      qc.invalidateQueries({ queryKey: ['users'] })
      toast.success('User deprecated — can be restored at any time')
      setDeprUser(null); setDeprReason('')
    } catch (e) { toast.error(getErrorMessage(e)) }
    finally { setDeprLoading(false) }
  }

  const handleRestore = async (u) => {
    try {
      await usersApi.restore(u.id)
      qc.invalidateQueries({ queryKey: ['users'] })
      qc.invalidateQueries({ queryKey: ['users-deprecated'] })
      toast.success(`${u.firstName} restored successfully`)
    } catch (e) { toast.error(getErrorMessage(e)) }
  }

  return (
    <div className="animate-fade-in">
      <div className="page-header mb-5">
        <div>
          <h1 className="page-title">{showDeprecated ? 'Deprecated Users' : 'Team Members'}</h1>
          <p className="page-subtitle">{total} user{total !== 1 ? 's' : ''}</p>
        </div>
        {isAdmin() && (
          <button onClick={() => { setShowDeprecated(v => !v); setPage(1) }}
            className={clsx('btn-secondary gap-2', showDeprecated && 'border-amber-300 text-amber-700 bg-amber-50')}>
            <HiOutlineArchive className="w-4 h-4" />
            {showDeprecated ? 'Show active' : 'View deprecated'}
          </button>
        )}
      </div>

      {!showDeprecated && (
        <div className="relative mb-5 max-w-md">
          <HiOutlineSearch className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-surface-400" />
          <input value={search} onChange={e => { setSearch(e.target.value); setPage(1) }}
            placeholder="Search by name, email, username…" className="input pl-9" />
        </div>
      )}

      {isLoading ? (
        <div className="flex justify-center py-16"><Spinner size="lg" /></div>
      ) : users.length === 0 ? (
        <div className="text-center py-16 text-surface-400">
          <HiOutlineUsers className="w-12 h-12 mx-auto mb-3 text-surface-300" />
          <p>{showDeprecated ? 'No deprecated users' : 'No users found'}</p>
        </div>
      ) : (
        <div className="table-wrapper">
          <table className="table">
            <thead>
              <tr>
                <th>User</th>
                <th>Roles</th>
                <th>Status</th>
                {showDeprecated ? <th>Reason</th> : <th>Last login</th>}
                <th>Joined</th>
                {isAdmin() && <th className="text-right">Actions</th>}
              </tr>
            </thead>
            <tbody>
              {users.map(u => (
                <tr key={u.id}>
                  <td>
                    <div className="flex items-center gap-3">
                      <div className="w-8 h-8 rounded-full bg-primary-100 text-primary-700 flex items-center justify-center text-xs font-bold flex-shrink-0">
                        {u.firstName?.charAt(0)}{u.lastName?.charAt(0)}
                      </div>
                      <div>
                        <p className="text-sm font-medium text-surface-800">
                          {u.firstName} {u.lastName}
                          {u.id === me?.id && <span className="text-xs text-surface-400 ml-1">(you)</span>}
                        </p>
                        <p className="text-xs text-surface-400">{u.email}</p>
                      </div>
                    </div>
                  </td>
                  <td>
                    <div className="flex flex-wrap gap-1">
                      {(u.roles ?? []).map(r => {
                        const b = getRoleBadge(r)
                        return <span key={r} className={b.color}>{b.label}</span>
                      })}
                    </div>
                  </td>
                  <td>
                    <span className={clsx('badge', u.isActive ? 'badge-green' : 'badge-red')}>
                      {u.isActive ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  {showDeprecated
                    ? <td className="text-xs text-surface-500">{u.deprecationReason || '—'}</td>
                    : <td className="text-xs text-surface-500">{u.lastLogin ? timeAgo(u.lastLogin) : 'Never'}</td>
                  }
                  <td className="text-xs text-surface-500">{timeAgo(u.createdAt)}</td>
                  {isAdmin() && (
                    <td>
                      <div className="flex items-center justify-end gap-1">
                        {showDeprecated ? (
                          <button onClick={() => handleRestore(u)} title="Restore user"
                            className="btn-ghost p-1.5 rounded-lg text-green-600 hover:bg-green-50">
                            <HiOutlineRefresh className="w-4 h-4" />
                          </button>
                        ) : (
                          <>
                            <button onClick={() => { setRoleModal(u); setSelectedRoles(u.roles ?? []) }}
                              title="Edit roles" className="btn-ghost p-1.5 rounded-lg">
                              <HiOutlineShieldCheck className="w-4 h-4 text-primary-500" />
                            </button>
                            {u.id !== me?.id && (
                              <>
                                <button onClick={() => toggleActive(u)} title={u.isActive ? 'Deactivate' : 'Activate'}
                                  className="btn-ghost p-1.5 rounded-lg">
                                  {u.isActive
                                    ? <HiOutlineBan className="w-4 h-4 text-yellow-500" />
                                    : <HiOutlineCheckCircle className="w-4 h-4 text-green-500" />}
                                </button>
                                <button onClick={() => setDeprUser(u)} title="Deprecate user"
                                  className="btn-ghost p-1.5 rounded-lg text-amber-500 hover:bg-amber-50">
                                  <HiOutlineArchive className="w-4 h-4" />
                                </button>
                              </>
                            )}
                          </>
                        )}
                      </div>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <Pagination page={page} totalPages={totalPages} onChange={setPage} />

      {/* Role editor */}
      <Modal open={!!roleModal} onClose={() => setRoleModal(null)} title={`Edit roles — ${roleModal?.firstName}`} size="sm"
        footer={<><button className="btn-secondary" onClick={() => setRoleModal(null)}>Cancel</button><button className="btn-primary" onClick={saveRoles}>Save roles</button></>}>
        <p className="text-sm text-surface-500 mb-4">Select roles for {roleModal?.firstName} {roleModal?.lastName}</p>
        <div className="space-y-2">
          {ALL_ROLES.map(role => {
            const checked = selectedRoles.includes(role)
            return (
              <label key={role} className="flex items-center gap-3 p-3 rounded-xl cursor-pointer hover:bg-surface-50 border border-surface-100">
                <input type="checkbox" checked={checked}
                  onChange={() => setSelectedRoles(prev => checked ? prev.filter(r => r !== role) : [...prev, role])}
                  className="w-4 h-4 accent-primary-600" />
                <span className="text-sm font-medium text-surface-700">{role.replace('ROLE_', '')}</span>
              </label>
            )
          })}
        </div>
      </Modal>

      {/* Deprecate user modal */}
      {deprUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/50" onClick={() => setDeprUser(null)} />
          <div className="relative bg-white rounded-2xl shadow-modal p-6 w-full max-w-md animate-slide-up">
            <h2 className="text-lg font-semibold mb-1">Deprecate user</h2>
            <p className="text-sm text-surface-500 mb-4">
              {deprUser.firstName} {deprUser.lastName} will be blocked from logging in.
              All data is preserved and can be restored at any time.
            </p>
            <label className="label">Reason <span className="text-surface-400 font-normal">(optional)</span></label>
            <input value={deprReason} onChange={e => setDeprReason(e.target.value)}
              placeholder="e.g. Left the organisation" className="input mb-5" autoFocus />
            <div className="flex gap-3 justify-end">
              <button className="btn-secondary" onClick={() => { setDeprUser(null); setDeprReason('') }}>Cancel</button>
              <button onClick={handleDeprecate} disabled={deprLoading}
                className="btn bg-amber-600 text-white hover:bg-amber-700 shadow-sm">
                {deprLoading ? 'Deprecating…' : 'Deprecate user'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

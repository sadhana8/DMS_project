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
  HiOutlineRefresh, HiOutlineFilter, HiOutlineUserAdd, HiOutlineX,
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

  // Admin create
  const [createOpen,    setCreateOpen]    = useState(false)
  const [createForm,    setCreateForm]    = useState({
    username:'', email:'', firstName:'', lastName:'', phoneNumber:'',
    department: 'OTHER', roles: ['ROLE_EMPLOYEE'],
  })
  const [createBusy,    setCreateBusy]    = useState(false)
  // Terminate
  const [termUser,      setTermUser]      = useState(null)
  const [termReason,    setTermReason]    = useState('')
  const [termBusy,      setTermBusy]      = useState(false)

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

  const handleCreate = async () => {
    setCreateBusy(true)
    try {
      await usersApi.adminCreate(createForm)
      qc.invalidateQueries({ queryKey: ['users'] })
      toast.success(`Account created — temp password emailed to ${createForm.email}`)
      setCreateOpen(false)
      setCreateForm({ username:'', email:'', firstName:'', lastName:'', phoneNumber:'',
                      department: 'OTHER', roles: ['ROLE_EMPLOYEE'] })
    } catch (e) { toast.error(getErrorMessage(e)) }
    finally { setCreateBusy(false) }
  }

  const handleTerminate = async () => {
    if (!termReason || termReason.trim().length < 5) {
      toast.error('A reason of at least 5 characters is required to terminate')
      return
    }
    setTermBusy(true)
    try {
      await usersApi.terminate(termUser.id, termReason.trim())
      qc.invalidateQueries({ queryKey: ['users'] })
      toast.success('User terminated — access revoked immediately')
      setTermUser(null); setTermReason('')
    } catch (e) { toast.error(getErrorMessage(e)) }
    finally { setTermBusy(false) }
  }

  return (
    <div className="animate-fade-in">
      <div className="page-header mb-5">
        <div>
          <h1 className="page-title">{showDeprecated ? 'Deprecated Users' : 'Team Members'}</h1>
          <p className="page-subtitle">{total} user{total !== 1 ? 's' : ''}</p>
        </div>
        {isAdmin() && (
          <div className="flex items-center gap-2">
            {!showDeprecated && (
              <button onClick={() => setCreateOpen(true)} className="btn-primary gap-2">
                <HiOutlineUserAdd className="w-4 h-4" /> Create user
              </button>
            )}
            <button onClick={() => { setShowDeprecated(v => !v); setPage(1) }}
              className={clsx('btn-secondary gap-2', showDeprecated && 'border-amber-300 text-amber-700 bg-amber-50')}>
              <HiOutlineArchive className="w-4 h-4" />
              {showDeprecated ? 'Show active' : 'View deprecated'}
            </button>
          </div>
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
                <th>Department</th>
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
                    <span className="badge bg-surface-100 text-surface-700">
                      {(u.department || 'OTHER').charAt(0) + (u.department || 'OTHER').slice(1).toLowerCase()}
                    </span>
                  </td>
                  <td>
                    {u.terminatedAt ? (
                      <span className="badge badge-red" title={u.terminationReason ?? ''}>Terminated</span>
                    ) : u.resignationEffectiveDate ? (
                      <span className="badge badge-amber" title={`Effective ${new Date(u.resignationEffectiveDate).toLocaleDateString()}`}>Resigning</span>
                    ) : (
                      <span className={clsx('badge', u.isActive ? 'badge-green' : 'badge-red')}>
                        {u.isActive ? 'Active' : 'Inactive'}
                      </span>
                    )}
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
                                <button onClick={() => setTermUser(u)} title="Terminate user (immediate)"
                                  className="btn-ghost p-1.5 rounded-lg text-red-500 hover:bg-red-50">
                                  <HiOutlineX className="w-4 h-4" />
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

      {/* Create user modal */}
      <Modal open={createOpen} onClose={() => setCreateOpen(false)}
        title="Create user"
        size="md"
        footer={<>
          <button className="btn-secondary" onClick={() => setCreateOpen(false)}>Cancel</button>
          <button className="btn-primary" disabled={createBusy} onClick={handleCreate}>
            {createBusy ? 'Creating…' : 'Create & email password'}
          </button>
        </>}>
        <p className="text-sm text-surface-500 mb-4">
          A strong random password will be generated and emailed to the user.
          They'll be required to change it the first time they log in.
        </p>
        <div className="grid grid-cols-2 gap-3">
          <div>
            <label className="label text-xs">First name</label>
            <input className="input" value={createForm.firstName}
              onChange={e => setCreateForm(f => ({ ...f, firstName: e.target.value }))} />
          </div>
          <div>
            <label className="label text-xs">Last name</label>
            <input className="input" value={createForm.lastName}
              onChange={e => setCreateForm(f => ({ ...f, lastName: e.target.value }))} />
          </div>
        </div>
        <div className="mt-3">
          <label className="label text-xs">Username</label>
          <input className="input" value={createForm.username}
            onChange={e => setCreateForm(f => ({ ...f, username: e.target.value }))} />
        </div>
        <div className="mt-3">
          <label className="label text-xs">Email — must be a real address</label>
          <input type="email" className="input" placeholder="user@company.com"
            value={createForm.email}
            onChange={e => setCreateForm(f => ({ ...f, email: e.target.value }))} />
        </div>
        <div className="mt-3">
          <label className="label text-xs">Phone <span className="text-surface-400 font-normal">(optional)</span></label>
          <input className="input" value={createForm.phoneNumber}
            onChange={e => setCreateForm(f => ({ ...f, phoneNumber: e.target.value }))} />
        </div>
        <div className="mt-3">
          <label className="label text-xs">Department</label>
          <select className="input"
            value={createForm.department}
            onChange={e => setCreateForm(f => ({ ...f, department: e.target.value }))}>
            <option value="HR">HR</option>
            <option value="ACCOUNT">Account</option>
            <option value="ENGINEERING">Engineering</option>
            <option value="SALES">Sales</option>
            <option value="OPERATIONS">Operations</option>
            <option value="OTHER">Other</option>
          </select>
        </div>
        <div className="mt-3">
          <label className="label text-xs">Roles</label>
          <div className="flex flex-wrap gap-2 mt-1">
            {ALL_ROLES.map(role => {
              const checked = createForm.roles.includes(role)
              return (
                <button key={role} type="button"
                  onClick={() => setCreateForm(f => ({
                    ...f,
                    roles: checked ? f.roles.filter(r => r !== role) : [...f.roles, role],
                  }))}
                  className={clsx('px-3 py-1.5 rounded-lg text-xs font-medium border',
                    checked
                      ? 'bg-primary-600 text-white border-primary-600'
                      : 'bg-white text-surface-600 border-surface-200 hover:border-primary-300')}>
                  {role.replace('ROLE_','')}
                </button>
              )
            })}
          </div>
          <p className="text-xs text-surface-400 mt-1">Defaults to Employee if none selected.</p>
        </div>
      </Modal>

      {/* Terminate user modal */}
      {termUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/50" onClick={() => setTermUser(null)} />
          <div className="relative bg-white rounded-2xl shadow-modal p-6 w-full max-w-md animate-slide-up">
            <div className="flex items-center gap-2 mb-1">
              <div className="w-8 h-8 rounded-lg bg-red-100 text-red-700 flex items-center justify-center">
                <HiOutlineX className="w-4 h-4" />
              </div>
              <h2 className="text-lg font-semibold">Terminate user — immediate</h2>
            </div>
            <p className="text-sm text-surface-500 mb-4">
              <strong>{termUser.firstName} {termUser.lastName}</strong> will be logged out
              within seconds and cannot log in again. The reason is recorded in the audit
              trail and emailed to the user.
            </p>
            <label className="label">Reason <span className="text-red-500">*</span> (5–500 chars)</label>
            <textarea value={termReason} onChange={e => setTermReason(e.target.value)}
              placeholder="e.g. Policy violation. Specifically: …"
              rows={3} className="input mb-5" autoFocus />
            <div className="flex gap-3 justify-end">
              <button className="btn-secondary" onClick={() => { setTermUser(null); setTermReason('') }}>Cancel</button>
              <button onClick={handleTerminate} disabled={termBusy}
                className="btn bg-red-600 text-white hover:bg-red-700 shadow-sm">
                {termBusy ? 'Terminating…' : 'Terminate user'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
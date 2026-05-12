import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { documentsApi } from '@/api/documents'
import { usersApi } from '@/api/users'
import { getRoleBadge, getErrorMessage } from '@/utils/helpers'
import Modal from '@/components/common/Modal'
import { Avatar } from '@/components/common/index'
import Spinner from '@/components/common/Spinner'
import toast from 'react-hot-toast'
import { HiOutlineTrash, HiOutlinePlus } from 'react-icons/hi'

const PERMISSIONS = ['VIEW','DOWNLOAD','EDIT','ADMIN']
const PERM_COLORS = { VIEW: 'badge-gray', DOWNLOAD: 'badge-blue', EDIT: 'badge-yellow', ADMIN: 'badge-red' }

export default function ShareModal({ open, onClose, document }) {
  const qc = useQueryClient()
  const [email,  setEmail]  = useState('')
  const [perm,   setPerm]   = useState('VIEW')
  const [adding, setAdding] = useState(false)

  const { data: permissions = [], isLoading } = useQuery({
    queryKey: ['doc-permissions', document?.id],
    queryFn:  () => documentsApi.getPermissions(document.id),
    enabled:  !!document?.id && open,
  })

  const share = async () => {
    if (!email.trim()) return
    setAdding(true)
    try {
      await documentsApi.shareDocument(document.id, { email: email.trim(), permission: perm })
      qc.invalidateQueries({ queryKey: ['doc-permissions', document.id] })
      setEmail('')
      toast.success('Access granted!')
    } catch (e) { toast.error(getErrorMessage(e)) }
    finally { setAdding(false) }
  }

  const removeAccess = async (userId) => {
    try {
      await documentsApi.removePermission(document.id, userId)
      qc.invalidateQueries({ queryKey: ['doc-permissions', document.id] })
      toast.success('Access removed')
    } catch (e) { toast.error(getErrorMessage(e)) }
  }

  const changePermission = async (userId, newPerm) => {
    try {
      await documentsApi.updatePermission(document.id, userId, newPerm)
      qc.invalidateQueries({ queryKey: ['doc-permissions', document.id] })
    } catch (e) { toast.error(getErrorMessage(e)) }
  }

  return (
    <Modal open={open} onClose={onClose} title={`Share "${document?.title}"`} size="md">
      {/* Add user */}
      <div className="mb-5">
        <p className="text-sm font-medium text-surface-700 mb-2">Add people</p>
        <div className="flex gap-2">
          <input
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && share()}
            placeholder="Enter email address"
            type="email"
            className="input flex-1"
          />
          <select value={perm} onChange={(e) => setPerm(e.target.value)} className="input w-36">
            {PERMISSIONS.map(p => <option key={p} value={p}>{p.charAt(0) + p.slice(1).toLowerCase()}</option>)}
          </select>
          <button onClick={share} disabled={adding || !email.trim()} className="btn-primary px-3">
            {adding ? <Spinner size="sm" /> : <HiOutlinePlus className="w-5 h-5" />}
          </button>
        </div>
      </div>

      {/* People with access */}
      <div>
        <p className="text-sm font-medium text-surface-700 mb-3">People with access</p>
        {isLoading ? (
          <div className="flex justify-center py-6"><Spinner /></div>
        ) : permissions.length === 0 ? (
          <p className="text-sm text-surface-400 text-center py-6">No shared access yet</p>
        ) : (
          <div className="space-y-2">
            {permissions.map((p) => (
              <div key={p.id} className="flex items-center gap-3 p-3 rounded-xl bg-surface-50">
                <Avatar user={p.user} size="sm" />
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-surface-800 truncate">{p.user?.firstName} {p.user?.lastName}</p>
                  <p className="text-xs text-surface-400 truncate">{p.user?.email}</p>
                </div>
                <select
                  value={p.permission}
                  onChange={(e) => changePermission(p.user.id, e.target.value)}
                  className="text-xs border border-surface-200 rounded-lg px-2 py-1 bg-white text-surface-700 focus:outline-none focus:ring-1 focus:ring-primary-500"
                >
                  {PERMISSIONS.map(pm => <option key={pm} value={pm}>{pm.charAt(0) + pm.slice(1).toLowerCase()}</option>)}
                </select>
                <button onClick={() => removeAccess(p.user.id)} className="btn-ghost p-1.5 rounded-lg text-red-400 hover:text-red-600 hover:bg-red-50">
                  <HiOutlineTrash className="w-4 h-4" />
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </Modal>
  )
}

import { useCompany } from '@/context/CompanyContext'
import { HiOutlineMenu } from 'react-icons/hi'

export default function Header({ onMenuClick }) {
  const { company } = useCompany()

  const appVersion = company?.app_version || ''

  return (
    <header className="flex items-center gap-3 px-4 lg:px-6 h-14 bg-white dark:bg-gray-900 border-b border-surface-200 dark:border-gray-800 flex-shrink-0 z-20">
      <button
        onClick={onMenuClick}
        className="btn-ghost p-2 rounded-lg lg:hidden flex-shrink-0"
      >
        <HiOutlineMenu className="w-5 h-5" />
      </button>

      <div className="flex-1" />

      <div className="flex items-center gap-2 flex-shrink-0">
        {appVersion && (
          <span className="hidden sm:inline-flex text-[10px] font-mono text-surface-300 dark:text-gray-600 px-2 py-0.5 rounded-full border border-surface-200 dark:border-gray-700">
            v{appVersion}
          </span>
        )}
      </div>
    </header>
  )
}
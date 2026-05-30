import { createContext, useContext, useState, useEffect } from 'react'
import { settingsApi } from '@/api/settings'

const CompanyContext = createContext(null)

export function CompanyProvider({ children }) {
  const [company, setCompany] = useState({
    company_name: 'DocVault',
    company_logo_url: '',
    app_name: 'DocVault',
    app_version: '1.0.0',
    company_email: '',
    company_phone: '',
    company_website: '',
  })

  useEffect(() => {
    settingsApi.getPublicCompany()
      .then(data => setCompany(prev => ({ ...prev, ...data })))
      .catch(() => {}) // silently fallback to defaults
  }, [])

  const refresh = () => {
    settingsApi.getPublicCompany()
      .then(data => setCompany(prev => ({ ...prev, ...data })))
      .catch(() => {})
  }

  return (
    <CompanyContext.Provider value={{ company, refresh }}>
      {children}
    </CompanyContext.Provider>
  )
}

export const useCompany = () => {
  const ctx = useContext(CompanyContext)
  if (!ctx) throw new Error('useCompany must be used within CompanyProvider')
  return ctx
}

import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getMe } from '../api/auth'
import { useAuth } from '../hooks/useAuth'

/**
 * Landing page after GitHub redirects back to /auth/callback.
 * Spring has already established the session at this point.
 * We just confirm the session by calling /api/auth/me and redirect to dashboard.
 */
export default function CallbackPage() {
  const navigate = useNavigate()
  const { setUser } = useAuth()

  useEffect(() => {
    getMe()
      .then(user => {
        setUser(user)
        navigate('/dashboard', { replace: true })
      })
      .catch(() => {
        navigate('/login?error=auth_failed', { replace: true })
      })
  }, [])

  return (
    <div style={{
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      height: '100vh',
      background: 'var(--bg-0)',
      gap: '20px',
    }}>
      <div style={{
        width: '48px', height: '48px',
        border: '3px solid var(--bg-3)',
        borderTopColor: 'var(--accent)',
        borderRadius: '50%',
        animation: 'spin 0.8s linear infinite',
      }} />
      <p style={{ color: 'var(--text-secondary)', fontSize: '16px' }}>
        Completing sign-in…
      </p>
    </div>
  )
}

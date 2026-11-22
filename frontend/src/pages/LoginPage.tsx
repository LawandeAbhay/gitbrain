import { useEffect } from 'react'
import './LoginPage.css'

export default function LoginPage() {
  // If user directly hits /login, the GitHub OAuth flow starts here
  const handleLogin = () => {
    window.location.href = '/oauth2/authorization/github'
  }

  return (
    <div className="login-page">
      {/* Animated background blobs */}
      <div className="bg-blob blob-1" />
      <div className="bg-blob blob-2" />
      <div className="bg-blob blob-3" />

      <div className="login-container">
        <div className="login-logo">
          <svg width="36" height="36" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 2L2 7l10 5 10-5-10-5z" stroke="white" strokeWidth="2" strokeLinejoin="round"/>
            <path d="M2 17l10 5 10-5" stroke="white" strokeWidth="2" strokeLinejoin="round"/>
            <path d="M2 12l10 5 10-5" stroke="white" strokeWidth="2" strokeLinejoin="round"/>
          </svg>
        </div>

        <h1 className="login-title">GitBrain</h1>
        <p className="login-tagline">
          Chat with any GitHub repository.<br />
          Understand code instantly with AI.
        </p>

        <div className="login-features">
          {[
            { icon: '⚡', text: 'Connect your GitHub account' },
            { icon: '🔍', text: 'Index any repository with one click' },
            { icon: '💬', text: 'Ask questions about the codebase' },
          ].map(f => (
            <div key={f.text} className="login-feature">
              <span className="feature-icon">{f.icon}</span>
              <span>{f.text}</span>
            </div>
          ))}
        </div>

        <button
          id="github-login-btn"
          className="btn btn-primary btn-lg login-btn"
          onClick={handleLogin}
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
            <path d="M12 0C5.37 0 0 5.37 0 12c0 5.31 3.435 9.795 8.205 11.385.6.105.825-.255.825-.57 0-.285-.015-1.23-.015-2.235-3.015.555-3.795-.735-4.035-1.41-.135-.345-.72-1.41-1.23-1.695-.42-.225-1.02-.78-.015-.795.945-.015 1.62.87 1.845 1.23 1.08 1.815 2.805 1.305 3.495.99.105-.78.42-1.305.765-1.605-2.67-.3-5.46-1.335-5.46-5.925 0-1.305.465-2.385 1.23-3.225-.12-.3-.54-1.53.12-3.18 0 0 1.005-.315 3.3 1.23.96-.27 1.98-.405 3-.405s2.04.135 3 .405c2.295-1.56 3.3-1.23 3.3-1.23.66 1.65.24 2.88.12 3.18.765.84 1.23 1.905 1.23 3.225 0 4.605-2.805 5.625-5.475 5.925.435.375.81 1.095.81 2.22 0 1.605-.015 2.895-.015 3.3 0 .315.225.69.825.57A12.02 12.02 0 0 0 24 12c0-6.63-5.37-12-12-12z"/>
          </svg>
          Continue with GitHub
        </button>

        <p className="login-footer">
          We only request <strong>read access</strong> to your repositories.
          Your data stays private.
        </p>
      </div>
    </div>
  )
}

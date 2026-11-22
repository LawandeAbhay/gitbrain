import axios from 'axios'

/**
 * Central Axios instance.
 * withCredentials: true is mandatory — the backend uses session cookies (GITBRAIN_SESSION).
 * All API errors bubble up to individual hooks/pages for context-specific handling.
 */
const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/json' },
})

export default api

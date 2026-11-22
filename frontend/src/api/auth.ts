import api from './client'

export interface User {
  id: string
  githubId: number
  githubUsername: string
  displayName: string
  avatarUrl: string
  createdAt: string
}

export const getMe = (): Promise<User> =>
  api.get('/auth/me').then(r => r.data)

export const logout = (): Promise<void> =>
  api.post('/auth/logout').then(() => undefined)

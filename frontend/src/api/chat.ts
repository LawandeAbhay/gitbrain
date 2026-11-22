import api from './client'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
}

export interface ChatResponse {
  answer: string
  sources: string[]
}

export const sendMessage = (
  repoId: string,
  message: string,
  history: ChatMessage[]
): Promise<ChatResponse> =>
  api
    .post(`/repos/${repoId}/chat`, { message, history })
    .then(r => r.data.data)

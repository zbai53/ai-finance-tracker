import http from './http';
import type { Result } from '../types';

export interface AiConversation {
  id: number;
  userId: number;
  role: 'user' | 'assistant';
  message: string;
  createdAt: string;
}

export const getAiHistory = (): Promise<AiConversation[]> =>
  http.get<Result<AiConversation[]>>('/api/ai/history')
    .then(r => r.data.data);

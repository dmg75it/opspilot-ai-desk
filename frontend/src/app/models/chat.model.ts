export interface ChatSession {
  id: string;
  ticketId: string;
  createdAt: string;
}

export interface ChatMessage {
  id: string;
  sessionId: string;
  role: 'SYSTEM' | 'USER' | 'ASSISTANT';
  content: string;
  model?: string;
  tokenEstimate?: number;
  error: boolean;
  errorMessage?: string;
  createdAt: string;
}

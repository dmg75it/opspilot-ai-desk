export interface ChatSession {
  id: number;
  ticketId: number;
  createdAt: string;
}

export interface ChatMessage {
  id: number;
  sessionId: number;
  role: 'SYSTEM' | 'USER' | 'ASSISTANT';
  content: string;
  model?: string;
  tokenCount?: number;
  errorFlag: boolean;
  errorMessage?: string;
  createdAt: string;
}

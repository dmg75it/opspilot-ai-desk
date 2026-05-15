export type MessageRole = 'SYSTEM' | 'USER' | 'ASSISTANT';

export interface ChatSession {
  id: string;
  ticketId: string;
  createdAt: string;
  updatedAt: string;
}

export interface ChatMessage {
  id: string;
  sessionId: string;
  role: MessageRole;
  content: string;
  model?: string;
  tokenEstimate?: number;
  createdAt: string;
  errorFlag: boolean;
  errorMessage?: string;
}

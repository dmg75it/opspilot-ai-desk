export interface ChatMessage {
  id: string;
  role: 'SYSTEM' | 'USER' | 'ASSISTANT';
  content: string;
  model?: string;
  promptTokens?: number;
  completionTokens?: number;
  createdAt: string;
  error: boolean;
  errorMessage?: string;
}

export interface ChatSession {
  id: string;
  ticketId: string;
  createdAt: string;
  messages: ChatMessage[];
}

export interface AiActionResponse {
  content: string;
  success: boolean;
  error?: string;
}

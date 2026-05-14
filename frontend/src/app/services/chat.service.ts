import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ChatMessage, ChatSession } from '../models/chat.model';
import { TicketNote } from '../models/ticket.model';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly base = environment.apiUrl;

  constructor(private http: HttpClient) {}

  getOrCreateSession(ticketId: string): Observable<ChatSession> {
    return this.http.post<ChatSession>(`${this.base}/tickets/${ticketId}/chat/session`, {});
  }

  sendMessage(sessionId: string, content: string): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(`${this.base}/chat/sessions/${sessionId}/messages`, { content });
  }

  getMessages(sessionId: string): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.base}/chat/sessions/${sessionId}/messages`);
  }

  generateSummary(ticketId: string): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(`${this.base}/tickets/${ticketId}/chat/summary`, {});
  }

  suggestReply(ticketId: string): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(`${this.base}/tickets/${ticketId}/chat/suggest-reply`, {});
  }

  applyAsSummary(ticketId: string, messageId: string): Observable<TicketNote> {
    return this.http.post<TicketNote>(`${this.base}/tickets/${ticketId}/notes/from-ai/${messageId}`, {});
  }
}

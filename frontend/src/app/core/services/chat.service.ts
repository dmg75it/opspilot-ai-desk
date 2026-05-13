import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ChatSession, ChatMessage } from '../models/chat.model';
import { TicketNote } from '../models/ticket.model';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private http = inject(HttpClient);

  private base(ticketId: number): string {
    return `${environment.apiUrl}/api/tickets/${ticketId}/chat`;
  }

  getSession(ticketId: number): Observable<ChatSession> {
    return this.http.get<ChatSession>(`${this.base(ticketId)}/session`);
  }

  sendMessage(ticketId: number, content: string): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(`${this.base(ticketId)}/messages`, { content });
  }

  getMessages(ticketId: number): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.base(ticketId)}/messages`);
  }

  summarize(ticketId: number): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(`${this.base(ticketId)}/summarize`, {});
  }

  suggestReply(ticketId: number): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(`${this.base(ticketId)}/suggest-reply`, {});
  }

  applySummary(ticketId: number, messageId: number): Observable<TicketNote> {
    return this.http.post<TicketNote>(`${this.base(ticketId)}/apply-summary/${messageId}`, {});
  }
}

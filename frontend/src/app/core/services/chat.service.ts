import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ChatMessage, ChatSession } from '../models/chat.model';
import { TicketNote } from '../models/ticket.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class ChatService {
  private http = inject(HttpClient);

  private base(ticketId: string): string {
    return `${environment.apiUrl}/tickets/${ticketId}/chat`;
  }

  getOrCreateSession(ticketId: string): Observable<ChatSession> {
    return this.http.get<ChatSession>(`${this.base(ticketId)}/session`);
  }

  listMessages(ticketId: string): Observable<ChatMessage[]> {
    return this.http.get<ChatMessage[]>(`${this.base(ticketId)}/messages`);
  }

  sendMessage(ticketId: string, content: string): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(`${this.base(ticketId)}/messages`, { content });
  }

  generateSummary(ticketId: string): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(`${this.base(ticketId)}/summary`, {});
  }

  generateSuggestedReply(ticketId: string): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(`${this.base(ticketId)}/suggested-reply`, {});
  }

  applyAsNote(ticketId: string, messageId: string): Observable<TicketNote> {
    return this.http.post<TicketNote>(
      `${this.base(ticketId)}/messages/${messageId}/apply-as-note`, {});
  }
}

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ChatSession, ChatMessage, AiActionResponse } from '../models/chat.model';
import { Note } from '../models/ticket.model';

@Injectable({ providedIn: 'root' })
export class AiService {
  constructor(private http: HttpClient) {}

  private base(ticketId: string) { return `${environment.apiBaseUrl}/tickets/${ticketId}/ai`; }

  getSession(ticketId: string): Observable<ChatSession> {
    return this.http.get<ChatSession>(`${this.base(ticketId)}/session`);
  }

  sendMessage(ticketId: string, content: string): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(`${this.base(ticketId)}/messages`, { content });
  }

  generateSummary(ticketId: string): Observable<AiActionResponse> {
    return this.http.post<AiActionResponse>(`${this.base(ticketId)}/summary`, {});
  }

  generateSuggestedReply(ticketId: string): Observable<AiActionResponse> {
    return this.http.post<AiActionResponse>(`${this.base(ticketId)}/suggested-reply`, {});
  }

  applySummaryAsNote(ticketId: string, content: string): Observable<Note> {
    return this.http.post<Note>(`${this.base(ticketId)}/apply-summary`, { content });
  }
}

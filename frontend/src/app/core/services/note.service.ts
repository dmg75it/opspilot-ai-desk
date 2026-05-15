import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Note } from '../models/ticket.model';

@Injectable({ providedIn: 'root' })
export class NoteService {
  constructor(private http: HttpClient) {}

  listNotes(ticketId: string): Observable<Note[]> {
    return this.http.get<Note[]>(`${environment.apiBaseUrl}/tickets/${ticketId}/notes`);
  }

  addNote(ticketId: string, body: string): Observable<Note> {
    return this.http.post<Note>(`${environment.apiBaseUrl}/tickets/${ticketId}/notes`, { body });
  }
}

import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  CreateTicketRequest, Ticket, TicketNote, TicketPage, UpdateTicketRequest
} from '../models/ticket.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class TicketService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/tickets`;

  list(page = 0, size = 20, status?: string, priority?: string): Observable<TicketPage> {
    let params = new HttpParams().set('page', page).set('size', size).set('sort', 'createdAt,desc');
    if (status) params = params.set('status', status);
    if (priority) params = params.set('priority', priority);
    return this.http.get<TicketPage>(this.base, { params });
  }

  getById(id: string): Observable<Ticket> {
    return this.http.get<Ticket>(`${this.base}/${id}`);
  }

  create(request: CreateTicketRequest): Observable<Ticket> {
    return this.http.post<Ticket>(this.base, request);
  }

  update(id: string, request: UpdateTicketRequest): Observable<Ticket> {
    return this.http.patch<Ticket>(`${this.base}/${id}`, request);
  }

  changeStatus(id: string, status: string): Observable<Ticket> {
    return this.http.patch<Ticket>(`${this.base}/${id}/status`, { status });
  }

  assign(id: string, operatorId: string | null): Observable<Ticket> {
    return this.http.patch<Ticket>(`${this.base}/${id}/assign`, { operatorId });
  }

  addNote(id: string, body: string): Observable<TicketNote> {
    return this.http.post<TicketNote>(`${this.base}/${id}/notes`, { body });
  }

  listNotes(id: string): Observable<TicketNote[]> {
    return this.http.get<TicketNote[]>(`${this.base}/${id}/notes`);
  }

  close(id: string): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.base}/${id}/close`, {});
  }
}

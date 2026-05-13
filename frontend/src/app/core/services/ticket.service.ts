import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Ticket, TicketNote, TicketStatus, TicketPriority, TicketCategory,
  CreateTicketRequest, UpdateTicketRequest, ChangeStatusRequest, AssignTicketRequest
} from '../models/ticket.model';
import { Page } from '../models/page.model';

export interface TicketFilters {
  status?: TicketStatus;
  priority?: TicketPriority;
  category?: TicketCategory;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class TicketService {
  private http = inject(HttpClient);
  private base = `${environment.apiUrl}/api/tickets`;

  list(filters: TicketFilters = {}): Observable<Page<Ticket>> {
    let params = new HttpParams();
    if (filters.status) params = params.set('status', filters.status);
    if (filters.priority) params = params.set('priority', filters.priority);
    if (filters.category) params = params.set('category', filters.category);
    params = params.set('page', (filters.page ?? 0).toString());
    params = params.set('size', (filters.size ?? 20).toString());
    return this.http.get<Page<Ticket>>(this.base, { params });
  }

  get(id: number): Observable<Ticket> {
    return this.http.get<Ticket>(`${this.base}/${id}`);
  }

  create(req: CreateTicketRequest): Observable<Ticket> {
    return this.http.post<Ticket>(this.base, req);
  }

  update(id: number, req: UpdateTicketRequest): Observable<Ticket> {
    return this.http.put<Ticket>(`${this.base}/${id}`, req);
  }

  changeStatus(id: number, req: ChangeStatusRequest): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.base}/${id}/status`, req);
  }

  assign(id: number, req: AssignTicketRequest): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.base}/${id}/assign`, req);
  }

  close(id: number): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.base}/${id}/close`, {});
  }

  getNotes(id: number): Observable<TicketNote[]> {
    return this.http.get<TicketNote[]>(`${this.base}/${id}/notes`);
  }

  addNote(id: number, body: string): Observable<TicketNote> {
    return this.http.post<TicketNote>(`${this.base}/${id}/notes`, { body });
  }
}

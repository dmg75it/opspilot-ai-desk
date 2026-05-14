import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { PageResponse, Ticket, TicketCreateRequest, TicketNote, TicketSummary, TicketUpdateRequest } from '../models/ticket.model';

@Injectable({ providedIn: 'root' })
export class TicketService {
  private readonly base = `${environment.apiUrl}/tickets`;

  constructor(private http: HttpClient) {}

  create(req: TicketCreateRequest): Observable<Ticket> {
    return this.http.post<Ticket>(this.base, req);
  }

  list(params: { status?: string; priority?: string; category?: string; assignedTo?: string; page?: number; size?: number }): Observable<PageResponse<TicketSummary>> {
    let httpParams = new HttpParams();
    if (params.status) httpParams = httpParams.set('status', params.status);
    if (params.priority) httpParams = httpParams.set('priority', params.priority);
    if (params.category) httpParams = httpParams.set('category', params.category);
    if (params.assignedTo) httpParams = httpParams.set('assignedTo', params.assignedTo);
    if (params.page != null) httpParams = httpParams.set('page', params.page);
    if (params.size != null) httpParams = httpParams.set('size', params.size);
    return this.http.get<PageResponse<TicketSummary>>(this.base, { params: httpParams });
  }

  getById(id: string): Observable<Ticket> {
    return this.http.get<Ticket>(`${this.base}/${id}`);
  }

  update(id: string, req: TicketUpdateRequest): Observable<Ticket> {
    return this.http.put<Ticket>(`${this.base}/${id}`, req);
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

  getNotes(id: string): Observable<TicketNote[]> {
    return this.http.get<TicketNote[]>(`${this.base}/${id}/notes`);
  }

  getAudit(id: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.base}/${id}/audit`);
  }
}

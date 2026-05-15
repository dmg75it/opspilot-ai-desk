import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Ticket, Page, CreateTicketRequest, ChangeStatusRequest } from '../models/ticket.model';

@Injectable({ providedIn: 'root' })
export class TicketService {
  private base = `${environment.apiBaseUrl}/tickets`;

  constructor(private http: HttpClient) {}

  list(page = 0, size = 20, sort = 'createdAt', dir = 'desc'): Observable<Page<Ticket>> {
    const params = new HttpParams()
      .set('page', page).set('size', size).set('sort', sort).set('dir', dir);
    return this.http.get<Page<Ticket>>(this.base, { params });
  }

  getById(id: string): Observable<Ticket> {
    return this.http.get<Ticket>(`${this.base}/${id}`);
  }

  create(req: CreateTicketRequest): Observable<Ticket> {
    return this.http.post<Ticket>(this.base, req);
  }

  update(id: string, req: Partial<CreateTicketRequest>): Observable<Ticket> {
    return this.http.patch<Ticket>(`${this.base}/${id}`, req);
  }

  changeStatus(id: string, req: ChangeStatusRequest): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.base}/${id}/status`, req);
  }

  assign(id: string, assigneeId: string | null): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.base}/${id}/assign`, { assigneeId });
  }
}

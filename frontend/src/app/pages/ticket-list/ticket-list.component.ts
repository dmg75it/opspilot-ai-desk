import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { TicketService } from '../../services/ticket.service';
import { TicketSummary, PageResponse } from '../../models/ticket.model';

@Component({
  selector: 'app-ticket-list',
  standalone: true,
  imports: [RouterLink, CommonModule, ReactiveFormsModule],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>Tickets</h1>
        <a routerLink="/tickets/new" class="btn-primary">+ New Ticket</a>
      </div>

      <div class="filters-bar">
        <form [formGroup]="filterForm" (ngSubmit)="applyFilters()">
          <select formControlName="status"><option value="">All Status</option>
            <option *ngFor="let s of statuses" [value]="s">{{ s }}</option>
          </select>
          <select formControlName="priority"><option value="">All Priority</option>
            <option *ngFor="let p of priorities" [value]="p">{{ p }}</option>
          </select>
          <select formControlName="category"><option value="">All Category</option>
            <option *ngFor="let c of categories" [value]="c">{{ c }}</option>
          </select>
          <button type="submit" class="btn-filter">Filter</button>
          <button type="button" (click)="resetFilters()" class="btn-reset">Reset</button>
        </form>
      </div>

      @if (loading) { <div class="loading">Loading...</div> }
      @if (error) { <div class="alert-error">{{ error }}</div> }

      <div class="table-card">
        <table class="table">
          <thead>
            <tr>
              <th>Title</th>
              <th>Status</th>
              <th>Priority</th>
              <th>Category</th>
              <th>Assigned To</th>
              <th>Updated</th>
            </tr>
          </thead>
          <tbody>
            @for (t of tickets; track t.id) {
              <tr [routerLink]="['/tickets', t.id]" class="clickable-row">
                <td>
                  <div class="ticket-title">{{ t.title }}</div>
                  @if (t.externalRef) { <div class="ticket-ref">{{ t.externalRef }}</div> }
                </td>
                <td><span class="status-badge status-{{ t.status.toLowerCase() }}">{{ t.status }}</span></td>
                <td><span class="priority-badge priority-{{ t.priority.toLowerCase() }}">{{ t.priority }}</span></td>
                <td>{{ t.category }}</td>
                <td>{{ t.assignedToName || '—' }}</td>
                <td>{{ t.updatedAt | date:'short' }}</td>
              </tr>
            }
            @if (tickets.length === 0 && !loading) {
              <tr><td colspan="6" class="empty-row">No tickets found.</td></tr>
            }
          </tbody>
        </table>
      </div>

      @if (totalPages > 1) {
        <div class="pagination">
          <button [disabled]="page === 0" (click)="changePage(page - 1)">Prev</button>
          <span>Page {{ page + 1 }} of {{ totalPages }}</span>
          <button [disabled]="page >= totalPages - 1" (click)="changePage(page + 1)">Next</button>
        </div>
      }
    </div>
  `,
  styles: [`
    .page-container { padding: 32px; }
    .page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 24px; }
    .page-header h1 { font-size: 28px; color: #1a1f36; margin: 0; }
    .btn-primary { padding: 10px 20px; background: #6366f1; color: white; text-decoration: none; border-radius: 8px; font-weight: 600; }
    .filters-bar { background: white; border-radius: 12px; padding: 16px; margin-bottom: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
    .filters-bar form { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; }
    .filters-bar select { padding: 8px 12px; border: 1px solid #d1d5db; border-radius: 6px; font-size: 14px; }
    .btn-filter { padding: 8px 16px; background: #6366f1; color: white; border: none; border-radius: 6px; cursor: pointer; font-weight: 600; }
    .btn-reset { padding: 8px 16px; background: #f1f5f9; color: #374151; border: none; border-radius: 6px; cursor: pointer; }
    .loading { padding: 20px; color: #6366f1; }
    .alert-error { background: #fee2e2; color: #dc2626; padding: 12px 16px; border-radius: 8px; margin-bottom: 20px; }
    .table-card { background: white; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); overflow: hidden; }
    .table { width: 100%; border-collapse: collapse; font-size: 14px; }
    .table th { text-align: left; padding: 12px 16px; border-bottom: 2px solid #e2e8f0; color: #64748b; font-weight: 600; font-size: 12px; text-transform: uppercase; background: #f8fafc; }
    .table td { padding: 14px 16px; border-bottom: 1px solid #f1f5f9; }
    .clickable-row { cursor: pointer; }
    .clickable-row:hover td { background: #f8fafc; }
    .ticket-title { font-weight: 500; color: #1a1f36; }
    .ticket-ref { font-size: 11px; color: #94a3b8; margin-top: 2px; }
    .empty-row { text-align: center; color: #94a3b8; padding: 40px; }
    .status-badge, .priority-badge { display: inline-block; padding: 3px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; text-transform: uppercase; }
    .status-new { background: #dbeafe; color: #1d4ed8; }
    .status-in_progress { background: #fef9c3; color: #854d0e; }
    .status-waiting_for_customer { background: #fce7f3; color: #9d174d; }
    .status-resolved { background: #dcfce7; color: #15803d; }
    .status-closed { background: #f1f5f9; color: #64748b; }
    .priority-low { background: #dcfce7; color: #15803d; }
    .priority-medium { background: #fef9c3; color: #854d0e; }
    .priority-high { background: #fed7aa; color: #9a3412; }
    .priority-critical { background: #fee2e2; color: #dc2626; }
    .pagination { display: flex; align-items: center; justify-content: center; gap: 16px; padding: 20px; }
    .pagination button { padding: 6px 16px; border: 1px solid #d1d5db; border-radius: 6px; cursor: pointer; background: white; }
    .pagination button:disabled { opacity: 0.4; cursor: not-allowed; }
  `]
})
export class TicketListComponent implements OnInit {
  private ticketService = inject(TicketService);
  private fb = inject(FormBuilder);

  tickets: TicketSummary[] = [];
  loading = false;
  error = '';
  page = 0;
  totalPages = 0;
  statuses = ['NEW', 'IN_PROGRESS', 'WAITING_FOR_CUSTOMER', 'RESOLVED', 'CLOSED'];
  priorities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  categories = ['DELIVERY', 'PICKUP', 'DOCUMENTATION', 'CUSTOMER', 'SYSTEM', 'OTHER'];

  filterForm = this.fb.group({ status: [''], priority: [''], category: [''] });

  ngOnInit() { this.loadTickets(); }

  loadTickets() {
    this.loading = true;
    const f = this.filterForm.value;
    this.ticketService.list({
      status: f.status || undefined,
      priority: f.priority || undefined,
      category: f.category || undefined,
      page: this.page, size: 20
    }).subscribe({
      next: (res: PageResponse<TicketSummary>) => {
        this.tickets = res.content;
        this.totalPages = res.totalPages;
        this.loading = false;
      },
      error: () => { this.error = 'Failed to load tickets'; this.loading = false; }
    });
  }

  applyFilters() { this.page = 0; this.loadTickets(); }
  resetFilters() { this.filterForm.reset({ status: '', priority: '', category: '' }); this.page = 0; this.loadTickets(); }
  changePage(p: number) { this.page = p; this.loadTickets(); }
}

import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { DashboardService } from '../../services/dashboard.service';
import { DashboardData } from '../../models/dashboard.model';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, CommonModule],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>Dashboard</h1>
        <p>Welcome back, {{ auth.currentUser()?.fullName }}</p>
      </div>

      @if (loading) { <div class="loading">Loading...</div> }
      @if (error) { <div class="alert-error">{{ error }}</div> }

      @if (data) {
        <div class="stats-grid">
          <div class="stat-card">
            <div class="stat-label">AI Interactions Today</div>
            <div class="stat-value primary">{{ data.aiInteractionsToday }}</div>
          </div>
          @for (entry of getStatusEntries(); track entry[0]) {
            <div class="stat-card">
              <div class="stat-label">{{ entry[0] | titlecase }}</div>
              <div class="stat-value" [class]="'status-' + entry[0].toLowerCase()">{{ entry[1] }}</div>
            </div>
          }
        </div>

        <div class="section-grid">
          <div class="section-card">
            <h2>By Priority</h2>
            <div class="priority-list">
              @for (entry of getPriorityEntries(); track entry[0]) {
                <div class="priority-item">
                  <span class="priority-badge" [class]="'priority-' + entry[0].toLowerCase()">{{ entry[0] }}</span>
                  <span class="priority-count">{{ entry[1] }}</span>
                </div>
              }
            </div>
          </div>

          <div class="section-card">
            <h2>My Open Tickets</h2>
            @if (data.myOpenTickets.length === 0) {
              <p class="empty">No open tickets assigned to you.</p>
            }
            @for (t of data.myOpenTickets; track t.id) {
              <div class="ticket-row" [routerLink]="['/tickets', t.id]">
                <span class="ticket-title">{{ t.title }}</span>
                <span class="status-badge" [class]="'status-' + t.status.toLowerCase()">{{ t.status }}</span>
              </div>
            }
          </div>
        </div>

        <div class="section-card">
          <h2>Recently Updated</h2>
          <table class="table">
            <thead>
              <tr><th>Title</th><th>Status</th><th>Priority</th><th>Updated</th></tr>
            </thead>
            <tbody>
              @for (t of data.recentlyUpdated; track t.id) {
                <tr [routerLink]="['/tickets', t.id]" class="clickable-row">
                  <td>{{ t.title }}</td>
                  <td><span class="status-badge" [class]="'status-' + t.status.toLowerCase()">{{ t.status }}</span></td>
                  <td><span class="priority-badge" [class]="'priority-' + t.priority.toLowerCase()">{{ t.priority }}</span></td>
                  <td>{{ t.updatedAt | date:'short' }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      }
    </div>
  `,
  styles: [`
    .page-container { padding: 32px; }
    .page-header { margin-bottom: 32px; }
    .page-header h1 { font-size: 28px; color: #1a1f36; margin: 0 0 4px; }
    .page-header p { color: #64748b; margin: 0; }
    .loading { padding: 20px; color: #6366f1; }
    .alert-error { background: #fee2e2; color: #dc2626; padding: 12px 16px; border-radius: 8px; margin-bottom: 20px; }
    .stats-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 16px; margin-bottom: 24px; }
    .stat-card { background: white; border-radius: 12px; padding: 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
    .stat-label { font-size: 12px; color: #64748b; text-transform: uppercase; font-weight: 600; letter-spacing: 0.5px; margin-bottom: 8px; }
    .stat-value { font-size: 28px; font-weight: 700; color: #1a1f36; }
    .stat-value.primary { color: #6366f1; }
    .section-grid { display: grid; grid-template-columns: 1fr 2fr; gap: 20px; margin-bottom: 20px; }
    .section-card { background: white; border-radius: 12px; padding: 24px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
    .section-card h2 { font-size: 16px; color: #1a1f36; margin: 0 0 16px; }
    .priority-list { display: flex; flex-direction: column; gap: 8px; }
    .priority-item { display: flex; justify-content: space-between; align-items: center; }
    .priority-count { font-weight: 700; color: #1a1f36; }
    .ticket-row { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid #f1f5f9; cursor: pointer; }
    .ticket-row:hover { background: #f8fafc; }
    .ticket-title { font-size: 14px; color: #374151; }
    .empty { color: #94a3b8; font-size: 14px; }
    .table { width: 100%; border-collapse: collapse; font-size: 14px; }
    .table th { text-align: left; padding: 8px 12px; border-bottom: 2px solid #e2e8f0; color: #64748b; font-weight: 600; font-size: 12px; text-transform: uppercase; }
    .table td { padding: 12px; border-bottom: 1px solid #f1f5f9; }
    .clickable-row { cursor: pointer; }
    .clickable-row:hover td { background: #f8fafc; }
    .status-badge { display: inline-block; padding: 2px 8px; border-radius: 20px; font-size: 11px; font-weight: 600; text-transform: uppercase; }
    .status-new { background: #dbeafe; color: #1d4ed8; }
    .status-in_progress { background: #fef9c3; color: #854d0e; }
    .status-waiting_for_customer { background: #fce7f3; color: #9d174d; }
    .status-resolved { background: #dcfce7; color: #15803d; }
    .status-closed { background: #f1f5f9; color: #64748b; }
    .priority-badge { display: inline-block; padding: 2px 8px; border-radius: 20px; font-size: 11px; font-weight: 600; text-transform: uppercase; }
    .priority-low { background: #dcfce7; color: #15803d; }
    .priority-medium { background: #fef9c3; color: #854d0e; }
    .priority-high { background: #fed7aa; color: #9a3412; }
    .priority-critical { background: #fee2e2; color: #dc2626; }
  `]
})
export class DashboardComponent implements OnInit {
  auth = inject(AuthService);
  private dashboardService = inject(DashboardService);
  data: DashboardData | null = null;
  loading = false;
  error = '';

  ngOnInit() {
    this.loading = true;
    this.dashboardService.getDashboard().subscribe({
      next: d => { this.data = d; this.loading = false; },
      error: () => { this.error = 'Failed to load dashboard'; this.loading = false; }
    });
  }

  getStatusEntries(): [string, number][] {
    return Object.entries(this.data?.ticketsByStatus || {});
  }

  getPriorityEntries(): [string, number][] {
    return Object.entries(this.data?.ticketsByPriority || {});
  }
}

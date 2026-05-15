import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DatePipe } from '@angular/common';
import { DashboardService } from '../../core/services/dashboard.service';
import { DashboardStats } from '../../core/models/dashboard.model';
import { Ticket, TicketPriority, TicketStatus } from '../../core/models/ticket.model';
import { StatusChipComponent } from '../tickets/status-chip.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    RouterLink, MatCardModule, MatTableModule, MatChipsModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule, DatePipe, StatusChipComponent
  ],
  template: `
    <h1>Dashboard</h1>
    @if (loading()) {
      <div class="center"><mat-spinner></mat-spinner></div>
    } @else if (stats()) {
      <div class="stats-grid">
        <mat-card>
          <mat-card-header><mat-card-title>Tickets by Status</mat-card-title></mat-card-header>
          <mat-card-content>
            @for (entry of statusEntries(); track entry[0]) {
              <div class="stat-row">
                <app-status-chip [status]="entry[0]" />
                <strong>{{ entry[1] }}</strong>
              </div>
            }
          </mat-card-content>
        </mat-card>

        <mat-card>
          <mat-card-header><mat-card-title>Tickets by Priority</mat-card-title></mat-card-header>
          <mat-card-content>
            @for (entry of priorityEntries(); track entry[0]) {
              <div class="stat-row">
                <span class="priority-label priority-{{ entry[0].toLowerCase() }}">{{ entry[0] }}</span>
                <strong>{{ entry[1] }}</strong>
              </div>
            }
          </mat-card-content>
        </mat-card>

        <mat-card>
          <mat-card-header><mat-card-title>AI Interactions Today</mat-card-title></mat-card-header>
          <mat-card-content>
            <div class="big-number">{{ stats()!.aiInteractionsToday }}</div>
          </mat-card-content>
        </mat-card>
      </div>

      <mat-card class="table-card">
        <mat-card-header>
          <mat-card-title>My Open Tickets</mat-card-title>
          <mat-card-actions align="end">
            <a mat-button color="primary" routerLink="/tickets">View All</a>
          </mat-card-actions>
        </mat-card-header>
        <mat-card-content>
          <table mat-table [dataSource]="stats()!.myOpenTickets">
            <ng-container matColumnDef="title">
              <th mat-header-cell *matHeaderCellDef>Title</th>
              <td mat-cell *matCellDef="let t">
                <a [routerLink]="['/tickets', t.id]">{{ t.title }}</a>
              </td>
            </ng-container>
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Status</th>
              <td mat-cell *matCellDef="let t"><app-status-chip [status]="t.status" /></td>
            </ng-container>
            <ng-container matColumnDef="priority">
              <th mat-header-cell *matHeaderCellDef>Priority</th>
              <td mat-cell *matCellDef="let t">
                <span class="priority-label priority-{{ t.priority.toLowerCase() }}">{{ t.priority }}</span>
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="['title','status','priority']"></tr>
            <tr mat-row *matRowDef="let row; columns: ['title','status','priority']"></tr>
          </table>
          @if (!stats()!.myOpenTickets.length) {
            <p class="empty-message">No open tickets assigned to you.</p>
          }
        </mat-card-content>
      </mat-card>

      <mat-card class="table-card">
        <mat-card-header><mat-card-title>Recently Updated</mat-card-title></mat-card-header>
        <mat-card-content>
          <table mat-table [dataSource]="stats()!.recentlyUpdated">
            <ng-container matColumnDef="title">
              <th mat-header-cell *matHeaderCellDef>Title</th>
              <td mat-cell *matCellDef="let t">
                <a [routerLink]="['/tickets', t.id]">{{ t.title }}</a>
              </td>
            </ng-container>
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Status</th>
              <td mat-cell *matCellDef="let t"><app-status-chip [status]="t.status" /></td>
            </ng-container>
            <ng-container matColumnDef="updatedAt">
              <th mat-header-cell *matHeaderCellDef>Updated</th>
              <td mat-cell *matCellDef="let t">{{ t.updatedAt | date:'short' }}</td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="['title','status','updatedAt']"></tr>
            <tr mat-row *matRowDef="let row; columns: ['title','status','updatedAt']"></tr>
          </table>
        </mat-card-content>
      </mat-card>
    }
  `,
  styles: [`
    h1 { margin-bottom: 24px; }
    .stats-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 16px; margin-bottom: 24px; }
    .stat-row { display: flex; justify-content: space-between; align-items: center; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
    .big-number { font-size: 48px; font-weight: bold; text-align: center; padding: 16px; color: #3f51b5; }
    .table-card { margin-bottom: 24px; }
    .empty-message { text-align: center; color: #999; padding: 16px; }
    .center { display: flex; justify-content: center; padding: 64px; }
    .priority-label { padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: 500; }
    .priority-low { background: #e8f5e9; color: #388e3c; }
    .priority-medium { background: #fff3e0; color: #f57c00; }
    .priority-high { background: #fce4ec; color: #c62828; }
    .priority-critical { background: #f3e5f5; color: #6a1b9a; }
    a { color: #3f51b5; text-decoration: none; }
    a:hover { text-decoration: underline; }
  `]
})
export class DashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);

  stats = signal<DashboardStats | null>(null);
  loading = signal(true);

  statusEntries = () => Object.entries(this.stats()?.ticketsByStatus ?? {}) as [TicketStatus, number][];
  priorityEntries = () => Object.entries(this.stats()?.ticketsByPriority ?? {}) as [TicketPriority, number][];

  ngOnInit(): void {
    this.dashboardService.getStats().subscribe({
      next: data => { this.stats.set(data); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }
}

import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatBadgeModule } from '@angular/material/badge';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DashboardService } from '../../core/services/dashboard.service';
import { DashboardData } from '../../core/models/dashboard.model';
import { Ticket } from '../../core/models/ticket.model';
import { TicketStatusBadgeComponent } from '../../shared/components/ticket-status-badge/ticket-status-badge.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatTableModule,
    MatBadgeModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    TicketStatusBadgeComponent
  ],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit {
  private dashboardService = inject(DashboardService);

  data: DashboardData | null = null;
  loading = true;
  error = '';

  readonly statusKeys = ['NEW', 'IN_PROGRESS', 'WAITING_FOR_CUSTOMER', 'RESOLVED', 'CLOSED'];
  readonly priorityKeys = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  ticketColumns = ['id', 'title', 'status', 'priority', 'updatedAt'];

  ngOnInit(): void {
    this.dashboardService.getData().subscribe({
      next: (data) => { this.data = data; this.loading = false; },
      error: () => { this.error = 'Failed to load dashboard data.'; this.loading = false; }
    });
  }

  statusCount(status: string): number {
    return this.data?.ticketsByStatus?.[status] ?? 0;
  }

  priorityCount(priority: string): number {
    return this.data?.ticketsByPriority?.[priority] ?? 0;
  }

  myTickets(): Ticket[] {
    return this.data?.myOpenTickets ?? [];
  }

  recentTickets(): Ticket[] {
    return this.data?.recentTickets ?? [];
  }
}

import { Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DatePipe } from '@angular/common';
import { TicketService } from '../../../core/services/ticket.service';
import { Ticket, TicketPage } from '../../../core/models/ticket.model';
import { StatusChipComponent } from '../status-chip.component';

@Component({
  selector: 'app-ticket-list',
  standalone: true,
  imports: [
    RouterLink, FormsModule, MatTableModule, MatPaginatorModule,
    MatButtonModule, MatIconModule, MatFormFieldModule, MatSelectModule,
    MatCardModule, MatProgressSpinnerModule, DatePipe, StatusChipComponent
  ],
  template: `
    <div class="header">
      <h1>Tickets</h1>
      <a mat-raised-button color="primary" routerLink="/tickets/new">
        <mat-icon>add</mat-icon> New Ticket
      </a>
    </div>

    <mat-card class="filter-card">
      <mat-card-content class="filters">
        <mat-form-field appearance="outline">
          <mat-label>Status</mat-label>
          <mat-select [(ngModel)]="filterStatus" (ngModelChange)="onFilterChange()">
            <mat-option value="">All</mat-option>
            @for (s of statuses; track s) {
              <mat-option [value]="s">{{ s }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Priority</mat-label>
          <mat-select [(ngModel)]="filterPriority" (ngModelChange)="onFilterChange()">
            <mat-option value="">All</mat-option>
            @for (p of priorities; track p) {
              <mat-option [value]="p">{{ p }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
      </mat-card-content>
    </mat-card>

    @if (loading()) {
      <div class="center"><mat-spinner></mat-spinner></div>
    } @else {
      <mat-card>
        <table mat-table [dataSource]="page()?.content ?? []">
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
              <span class="priority-{{ t.priority.toLowerCase() }}">{{ t.priority }}</span>
            </td>
          </ng-container>
          <ng-container matColumnDef="category">
            <th mat-header-cell *matHeaderCellDef>Category</th>
            <td mat-cell *matCellDef="let t">{{ t.category }}</td>
          </ng-container>
          <ng-container matColumnDef="assignedTo">
            <th mat-header-cell *matHeaderCellDef>Assigned</th>
            <td mat-cell *matCellDef="let t">{{ t.assignedTo?.fullName ?? '-' }}</td>
          </ng-container>
          <ng-container matColumnDef="updatedAt">
            <th mat-header-cell *matHeaderCellDef>Updated</th>
            <td mat-cell *matCellDef="let t">{{ t.updatedAt | date:'short' }}</td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="columns"></tr>
          <tr mat-row *matRowDef="let row; columns: columns"
              class="clickable-row" [routerLink]="['/tickets', row.id]"></tr>
        </table>
        <mat-paginator [length]="page()?.totalElements ?? 0"
                       [pageSize]="20"
                       [pageSizeOptions]="[10,20,50]"
                       (page)="onPage($event)">
        </mat-paginator>
      </mat-card>
    }
  `,
  styles: [`
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
    h1 { margin: 0; }
    .filter-card { margin-bottom: 16px; }
    .filters { display: flex; gap: 16px; flex-wrap: wrap; padding-top: 8px; }
    .filters mat-form-field { min-width: 180px; }
    .clickable-row { cursor: pointer; }
    .clickable-row:hover { background: #f5f5f5; }
    .center { display: flex; justify-content: center; padding: 64px; }
    a { color: #3f51b5; text-decoration: none; }
    a:hover { text-decoration: underline; }
    .priority-low { color: #388e3c; }
    .priority-medium { color: #f57c00; }
    .priority-high { color: #c62828; font-weight: 500; }
    .priority-critical { color: #6a1b9a; font-weight: bold; }
  `]
})
export class TicketListComponent implements OnInit {
  private ticketService = inject(TicketService);

  columns = ['title', 'status', 'priority', 'category', 'assignedTo', 'updatedAt'];
  statuses = ['NEW', 'IN_PROGRESS', 'WAITING_FOR_CUSTOMER', 'RESOLVED', 'CLOSED'];
  priorities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

  page = signal<TicketPage | null>(null);
  loading = signal(true);
  filterStatus = '';
  filterPriority = '';
  currentPage = 0;

  ngOnInit(): void {
    this.load();
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.load();
  }

  onPage(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.ticketService.list(this.currentPage, 20, this.filterStatus, this.filterPriority).subscribe({
      next: data => { this.page.set(data); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }
}

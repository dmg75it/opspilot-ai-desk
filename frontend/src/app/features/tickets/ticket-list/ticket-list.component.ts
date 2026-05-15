import { Component, OnInit } from '@angular/core';
import { NgIf, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TicketService } from '../../../core/services/ticket.service';
import { Ticket, Page } from '../../../core/models/ticket.model';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { ErrorBannerComponent } from '../../../shared/components/error-banner/error-banner.component';

@Component({
  selector: 'app-ticket-list',
  standalone: true,
  imports: [NgIf, DatePipe, RouterLink, MatTableModule, MatPaginatorModule,
    MatButtonModule, MatIconModule, LoadingSpinnerComponent, ErrorBannerComponent],
  template: `
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:1rem">
      <h1>Tickets</h1>
      <a mat-raised-button color="primary" routerLink="/tickets/new">New Ticket</a>
    </div>
    <app-loading-spinner *ngIf="loading"></app-loading-spinner>
    <app-error-banner [message]="error"></app-error-banner>

    <table mat-table [dataSource]="tickets" *ngIf="!loading" style="width:100%">
      <ng-container matColumnDef="title">
        <th mat-header-cell *matHeaderCellDef>Title</th>
        <td mat-cell *matCellDef="let t">
          <a [routerLink]="['/tickets', t.id]" style="font-weight:500">{{ t.title }}</a>
        </td>
      </ng-container>
      <ng-container matColumnDef="status">
        <th mat-header-cell *matHeaderCellDef>Status</th>
        <td mat-cell *matCellDef="let t">
          <span [class]="'status-' + t.status.toLowerCase()">{{ t.status }}</span>
        </td>
      </ng-container>
      <ng-container matColumnDef="priority">
        <th mat-header-cell *matHeaderCellDef>Priority</th>
        <td mat-cell *matCellDef="let t">{{ t.priority }}</td>
      </ng-container>
      <ng-container matColumnDef="category">
        <th mat-header-cell *matHeaderCellDef>Category</th>
        <td mat-cell *matCellDef="let t">{{ t.category }}</td>
      </ng-container>
      <ng-container matColumnDef="createdByEmail">
        <th mat-header-cell *matHeaderCellDef>Created by</th>
        <td mat-cell *matCellDef="let t">{{ t.createdByEmail }}</td>
      </ng-container>
      <ng-container matColumnDef="updatedAt">
        <th mat-header-cell *matHeaderCellDef>Updated</th>
        <td mat-cell *matCellDef="let t">{{ t.updatedAt | date:'short' }}</td>
      </ng-container>
      <tr mat-header-row *matHeaderRowDef="columns"></tr>
      <tr mat-row *matRowDef="let row; columns: columns"></tr>
    </table>

    <mat-paginator
      [length]="totalElements"
      [pageSize]="pageSize"
      [pageSizeOptions]="[10, 20, 50]"
      (page)="onPage($event)">
    </mat-paginator>
  `,
  styles: [`
    .status-new { color: #1565C0; }
    .status-in_progress { color: #E65100; }
    .status-resolved { color: #2E7D32; }
    .status-closed { color: #616161; }
    .status-waiting_for_customer { color: #6A1B9A; }
  `]
})
export class TicketListComponent implements OnInit {
  tickets: Ticket[] = [];
  loading = true;
  error: string | null = null;
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;
  columns = ['title', 'status', 'priority', 'category', 'createdByEmail', 'updatedAt'];

  constructor(private ticketService: TicketService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.ticketService.list(this.currentPage, this.pageSize).subscribe({
      next: (page: Page<Ticket>) => {
        this.tickets = page.content;
        this.totalElements = page.totalElements;
        this.loading = false;
      },
      error: () => { this.error = 'Failed to load tickets'; this.loading = false; }
    });
  }

  onPage(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.load();
  }
}

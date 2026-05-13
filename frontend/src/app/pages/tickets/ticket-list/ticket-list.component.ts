import { Component, inject, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, MatPaginator, PageEvent } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatInputModule } from '@angular/material/input';
import { TicketService } from '../../../core/services/ticket.service';
import { Ticket, TicketStatus, TicketPriority, TicketCategory } from '../../../core/models/ticket.model';
import { Page } from '../../../core/models/page.model';
import { TicketStatusBadgeComponent } from '../../../shared/components/ticket-status-badge/ticket-status-badge.component';

@Component({
  selector: 'app-ticket-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    MatTableModule,
    MatPaginatorModule,
    MatSelectModule,
    MatFormFieldModule,
    MatButtonModule,
    MatCardModule,
    MatProgressSpinnerModule,
    MatInputModule,
    TicketStatusBadgeComponent
  ],
  templateUrl: './ticket-list.component.html',
  styleUrls: ['./ticket-list.component.scss']
})
export class TicketListComponent implements OnInit {
  private ticketService = inject(TicketService);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  @ViewChild(MatPaginator) paginator!: MatPaginator;

  page: Page<Ticket> | null = null;
  loading = false;
  currentPage = 0;
  pageSize = 20;

  displayedColumns = ['id', 'title', 'status', 'priority', 'category', 'assignedTo', 'updatedAt'];

  readonly statuses: TicketStatus[] = ['NEW', 'IN_PROGRESS', 'WAITING_FOR_CUSTOMER', 'RESOLVED', 'CLOSED'];
  readonly priorities: TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  readonly categories: TicketCategory[] = ['DELIVERY', 'PICKUP', 'DOCUMENTATION', 'CUSTOMER', 'SYSTEM', 'OTHER'];

  filters = this.fb.group({
    status: [''],
    priority: [''],
    category: ['']
  });

  ngOnInit(): void {
    this.load();
    this.filters.valueChanges.subscribe(() => {
      this.currentPage = 0;
      this.load();
    });
  }

  load(): void {
    this.loading = true;
    const { status, priority, category } = this.filters.value;
    this.ticketService.list({
      status: (status || undefined) as TicketStatus | undefined,
      priority: (priority || undefined) as TicketPriority | undefined,
      category: (category || undefined) as TicketCategory | undefined,
      page: this.currentPage,
      size: this.pageSize
    }).subscribe({
      next: (p) => { this.page = p; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  onPage(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.load();
  }

  openTicket(ticket: Ticket): void {
    this.router.navigate(['/tickets', ticket.id]);
  }

  newTicket(): void {
    this.router.navigate(['/tickets/new']);
  }
}

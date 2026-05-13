import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { TicketService } from '../../../core/services/ticket.service';
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/services/auth.service';
import { Ticket, TicketStatus } from '../../../core/models/ticket.model';
import { User } from '../../../core/models/user.model';
import { TicketStatusBadgeComponent } from '../../../shared/components/ticket-status-badge/ticket-status-badge.component';
import { TicketNotesComponent } from './ticket-notes/ticket-notes.component';
import { AiChatComponent } from './ai-chat/ai-chat.component';

@Component({
  selector: 'app-ticket-detail',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatTabsModule,
    MatButtonModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressSpinnerModule,
    MatIconModule,
    TicketStatusBadgeComponent,
    TicketNotesComponent,
    AiChatComponent
  ],
  templateUrl: './ticket-detail.component.html',
  styleUrls: ['./ticket-detail.component.scss']
})
export class TicketDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private ticketService = inject(TicketService);
  private userService = inject(UserService);
  authService = inject(AuthService);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  ticket: Ticket | null = null;
  loading = true;
  error = '';

  operators: User[] = [];

  showStatusPanel = false;
  showAssignPanel = false;

  readonly statuses: TicketStatus[] = ['NEW', 'IN_PROGRESS', 'WAITING_FOR_CUSTOMER', 'RESOLVED', 'CLOSED'];

  statusForm = this.fb.group({
    newStatus: ['', Validators.required],
    reason: ['']
  });

  assignForm = this.fb.group({
    operatorId: [null as number | null, Validators.required]
  });

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.ticketService.get(id).subscribe({
      next: (t) => { this.ticket = t; this.loading = false; },
      error: () => { this.error = 'Ticket not found.'; this.loading = false; }
    });
    this.userService.list().subscribe({
      next: (users) => { this.operators = users; }
    });
  }

  changeStatus(): void {
    if (!this.ticket || this.statusForm.invalid) return;
    const { newStatus, reason } = this.statusForm.value;
    this.ticketService.changeStatus(this.ticket.id, {
      newStatus: newStatus as TicketStatus,
      reason: reason || undefined
    }).subscribe({
      next: (t) => { this.ticket = t; this.showStatusPanel = false; this.statusForm.reset(); },
      error: () => { this.error = 'Failed to change status.'; }
    });
  }

  assignTicket(): void {
    if (!this.ticket || this.assignForm.invalid) return;
    const { operatorId } = this.assignForm.value;
    this.ticketService.assign(this.ticket.id, { operatorId: operatorId! }).subscribe({
      next: (t) => { this.ticket = t; this.showAssignPanel = false; this.assignForm.reset(); },
      error: () => { this.error = 'Failed to assign ticket.'; }
    });
  }

  closeTicket(): void {
    if (!this.ticket) return;
    this.ticketService.close(this.ticket.id).subscribe({
      next: (t) => { this.ticket = t; },
      error: () => { this.error = 'Failed to close ticket.'; }
    });
  }

  backToList(): void {
    this.router.navigate(['/tickets']);
  }

  isClosed(): boolean {
    return this.ticket?.status === 'CLOSED';
  }
}

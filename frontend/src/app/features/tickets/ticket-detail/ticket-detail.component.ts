import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTabsModule } from '@angular/material/tabs';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { TicketService } from '../../../core/services/ticket.service';
import { AuthService } from '../../../core/services/auth.service';
import { AdminService } from '../../../core/services/admin.service';
import { Ticket, TicketNote, TicketStatus } from '../../../core/models/ticket.model';
import { User } from '../../../core/models/user.model';
import { StatusChipComponent } from '../status-chip.component';
import { ChatPanelComponent } from '../../chat/chat-panel/chat-panel.component';

@Component({
  selector: 'app-ticket-detail',
  standalone: true,
  imports: [
    RouterLink, FormsModule, DatePipe, MatCardModule, MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule, MatInputModule, MatTabsModule,
    MatProgressSpinnerModule, MatDividerModule, MatSnackBarModule,
    StatusChipComponent, ChatPanelComponent
  ],
  template: `
    <div class="header">
      <button mat-icon-button routerLink="/tickets"><mat-icon>arrow_back</mat-icon></button>
      <h1>{{ ticket()?.title ?? 'Loading...' }}</h1>
      @if (ticket()) {
        <app-status-chip [status]="ticket()!.status" />
      }
    </div>

    @if (loading()) {
      <div class="center"><mat-spinner></mat-spinner></div>
    } @else if (ticket()) {
      <mat-tab-group>
        <mat-tab label="Details">
          <div class="tab-content">
            <div class="detail-grid">
              <mat-card>
                <mat-card-header><mat-card-title>Ticket Info</mat-card-title></mat-card-header>
                <mat-card-content>
                  <div class="info-row"><span>ID</span><span>{{ ticket()!.id }}</span></div>
                  <div class="info-row"><span>External Ref</span><span>{{ ticket()!.externalRef ?? '-' }}</span></div>
                  <div class="info-row"><span>Priority</span>
                    <span class="priority-{{ ticket()!.priority.toLowerCase() }}">{{ ticket()!.priority }}</span>
                  </div>
                  <div class="info-row"><span>Category</span><span>{{ ticket()!.category }}</span></div>
                  <div class="info-row"><span>Created by</span><span>{{ ticket()!.createdBy.fullName }}</span></div>
                  <div class="info-row"><span>Assigned to</span><span>{{ ticket()!.assignedTo?.fullName ?? 'Unassigned' }}</span></div>
                  <div class="info-row"><span>Created</span><span>{{ ticket()!.createdAt | date:'medium' }}</span></div>
                  <div class="info-row"><span>Updated</span><span>{{ ticket()!.updatedAt | date:'medium' }}</span></div>
                </mat-card-content>
              </mat-card>

              <div class="actions-panel">
                @if (ticket()!.status !== 'CLOSED') {
                  <mat-card>
                    <mat-card-header><mat-card-title>Change Status</mat-card-title></mat-card-header>
                    <mat-card-content>
                      <mat-form-field appearance="outline" class="full-width">
                        <mat-label>New Status</mat-label>
                        <mat-select [(ngModel)]="newStatus">
                          @for (s of availableStatuses(); track s) {
                            <mat-option [value]="s">{{ s }}</mat-option>
                          }
                        </mat-select>
                      </mat-form-field>
                      <button mat-raised-button color="accent" (click)="changeStatus()" [disabled]="!newStatus">
                        Update Status
                      </button>
                    </mat-card-content>
                  </mat-card>

                  <mat-card>
                    <mat-card-header><mat-card-title>Assign Ticket</mat-card-title></mat-card-header>
                    <mat-card-content>
                      <div class="assign-actions">
                        @if (ticket()!.assignedTo?.id !== auth.currentUser()?.id) {
                          <button mat-raised-button color="primary" (click)="assignToMe()">
                            <mat-icon>person</mat-icon> Assign to me
                          </button>
                        }
                        @if (ticket()!.assignedTo) {
                          <button mat-stroked-button (click)="unassign()">
                            <mat-icon>person_off</mat-icon> Unassign
                          </button>
                        }
                      </div>
                      @if (auth.isAdmin() && users().length > 0) {
                        <mat-form-field appearance="outline" class="full-width mt-8">
                          <mat-label>Assign to operator</mat-label>
                          <mat-select [(ngModel)]="selectedOperatorId">
                            @for (u of users(); track u.id) {
                              <mat-option [value]="u.id">{{ u.fullName }} ({{ u.email }})</mat-option>
                            }
                          </mat-select>
                        </mat-form-field>
                        <button mat-raised-button color="accent" (click)="assignTo()" [disabled]="!selectedOperatorId">
                          Assign
                        </button>
                      }
                    </mat-card-content>
                  </mat-card>
                }
              </div>
            </div>

            <mat-card class="description-card">
              <mat-card-header><mat-card-title>Description</mat-card-title></mat-card-header>
              <mat-card-content>
                <p class="description-text">{{ ticket()!.description }}</p>
              </mat-card-content>
            </mat-card>
          </div>
        </mat-tab>

        <mat-tab label="Notes ({{ notes().length }})">
          <div class="tab-content">
            @for (note of notes(); track note.id) {
              <mat-card class="note-card" [class]="'note-' + note.visibility.toLowerCase()">
                <mat-card-content>
                  <div class="note-header">
                    <span class="note-author">{{ note.author.fullName }}</span>
                    <span class="note-vis">{{ note.visibility }}</span>
                    <span class="note-time">{{ note.createdAt | date:'short' }}</span>
                  </div>
                  <p class="note-body">{{ note.body }}</p>
                </mat-card-content>
              </mat-card>
            }
            @if (ticket()!.status !== 'CLOSED') {
              <mat-card class="add-note-card">
                <mat-card-header><mat-card-title>Add Note</mat-card-title></mat-card-header>
                <mat-card-content>
                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Note</mat-label>
                    <textarea matInput [(ngModel)]="newNoteBody" rows="3"></textarea>
                  </mat-form-field>
                  <button mat-raised-button color="primary" (click)="addNote()" [disabled]="!newNoteBody.trim()">
                    Add Note
                  </button>
                </mat-card-content>
              </mat-card>
            }
          </div>
        </mat-tab>

        <mat-tab label="AI Chat">
          <div class="tab-content">
            <app-chat-panel [ticketId]="ticketId" (noteAdded)="loadNotes()" />
          </div>
        </mat-tab>
      </mat-tab-group>
    }
  `,
  styles: [`
    .header { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
    h1 { margin: 0; flex: 1; }
    .tab-content { padding: 16px 0; }
    .detail-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
    .info-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid #f0f0f0; font-size: 14px; }
    .info-row span:first-child { color: #666; }
    .description-card { margin-bottom: 16px; }
    .description-text { white-space: pre-wrap; line-height: 1.6; }
    .note-card { margin-bottom: 12px; }
    .note-internal { border-left: 4px solid #3f51b5; }
    .note-ai_summary { border-left: 4px solid #9c27b0; }
    .note-system { border-left: 4px solid #757575; opacity: 0.8; }
    .note-header { display: flex; gap: 8px; align-items: center; margin-bottom: 8px; }
    .note-author { font-weight: 500; }
    .note-vis { font-size: 11px; background: #f0f0f0; padding: 2px 6px; border-radius: 4px; }
    .note-time { margin-left: auto; font-size: 12px; color: #999; }
    .note-body { margin: 0; white-space: pre-wrap; }
    .add-note-card { margin-top: 16px; }
    .full-width { width: 100%; margin-bottom: 12px; }
    .actions-panel { display: flex; flex-direction: column; gap: 16px; }
    .assign-actions { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 8px; }
    .mt-8 { margin-top: 8px; }
    .center { display: flex; justify-content: center; padding: 64px; }
    .priority-low { color: #388e3c; }
    .priority-medium { color: #f57c00; }
    .priority-high { color: #c62828; font-weight: 500; }
    .priority-critical { color: #6a1b9a; font-weight: bold; }
  `]
})
export class TicketDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private ticketService = inject(TicketService);
  private adminService = inject(AdminService);
  private snackBar = inject(MatSnackBar);
  auth = inject(AuthService);

  ticket = signal<Ticket | null>(null);
  notes = signal<TicketNote[]>([]);
  users = signal<User[]>([]);
  loading = signal(true);
  newStatus: string = '';
  newNoteBody = '';
  selectedOperatorId = '';

  get ticketId(): string {
    return this.route.snapshot.paramMap.get('id')!;
  }

  availableStatuses = () => {
    const t = this.ticket();
    if (!t) return [];
    const all: TicketStatus[] = ['NEW', 'IN_PROGRESS', 'WAITING_FOR_CUSTOMER', 'RESOLVED', 'CLOSED'];
    return all.filter(s => s !== t.status && (
      t.status === 'CLOSED' ? this.auth.isAdmin() : this.isValidTransition(t.status, s)
    ));
  };

  ngOnInit(): void {
    this.load();
    if (this.auth.isAdmin()) {
      this.adminService.listUsers().subscribe({ next: u => this.users.set(u) });
    }
  }

  changeStatus(): void {
    if (!this.newStatus) return;
    this.ticketService.changeStatus(this.ticketId, this.newStatus).subscribe({
      next: t => {
        this.ticket.set(t);
        this.newStatus = '';
        this.snackBar.open('Status updated', '', { duration: 2000 });
        this.loadNotes();
      },
      error: err => this.snackBar.open(err.error?.detail || 'Error', '', { duration: 3000 })
    });
  }

  assignToMe(): void {
    const me = this.auth.currentUser();
    if (!me) return;
    this.ticketService.assign(this.ticketId, me.id).subscribe({
      next: t => { this.ticket.set(t); this.snackBar.open('Ticket assigned to you', '', { duration: 2000 }); this.loadNotes(); },
      error: err => this.snackBar.open(err.error?.detail || 'Error', '', { duration: 3000 })
    });
  }

  assignTo(): void {
    if (!this.selectedOperatorId) return;
    this.ticketService.assign(this.ticketId, this.selectedOperatorId).subscribe({
      next: t => { this.ticket.set(t); this.selectedOperatorId = ''; this.snackBar.open('Ticket assigned', '', { duration: 2000 }); this.loadNotes(); },
      error: err => this.snackBar.open(err.error?.detail || 'Error', '', { duration: 3000 })
    });
  }

  unassign(): void {
    this.ticketService.assign(this.ticketId, null).subscribe({
      next: t => { this.ticket.set(t); this.snackBar.open('Ticket unassigned', '', { duration: 2000 }); this.loadNotes(); },
      error: err => this.snackBar.open(err.error?.detail || 'Error', '', { duration: 3000 })
    });
  }

  addNote(): void {
    if (!this.newNoteBody.trim()) return;
    this.ticketService.addNote(this.ticketId, this.newNoteBody).subscribe({
      next: note => {
        this.notes.update(notes => [...notes, note]);
        this.newNoteBody = '';
        this.snackBar.open('Note added', '', { duration: 2000 });
      }
    });
  }

  private load(): void {
    this.ticketService.getById(this.ticketId).subscribe({
      next: t => { this.ticket.set(t); this.loading.set(false); this.loadNotes(); },
      error: () => this.loading.set(false)
    });
  }

  loadNotes(): void {
    this.ticketService.listNotes(this.ticketId).subscribe({
      next: notes => this.notes.set(notes)
    });
  }

  private isValidTransition(from: TicketStatus, to: TicketStatus): boolean {
    const transitions: Record<TicketStatus, TicketStatus[]> = {
      NEW: ['IN_PROGRESS', 'CLOSED'],
      IN_PROGRESS: ['WAITING_FOR_CUSTOMER', 'RESOLVED', 'CLOSED'],
      WAITING_FOR_CUSTOMER: ['IN_PROGRESS', 'RESOLVED', 'CLOSED'],
      RESOLVED: ['IN_PROGRESS', 'CLOSED'],
      CLOSED: []
    };
    return transitions[from]?.includes(to) ?? false;
  }
}

import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { TicketService } from '../../services/ticket.service';
import { ChatService } from '../../services/chat.service';
import { UserService } from '../../services/user.service';
import { Ticket, TicketNote } from '../../models/ticket.model';
import { ChatMessage, ChatSession } from '../../models/chat.model';
import { User } from '../../models/user.model';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-ticket-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="page-container">
      <a routerLink="/tickets" class="back-link">← Back to Tickets</a>

      @if (loading) { <div class="loading">Loading...</div> }
      @if (error) { <div class="alert-error">{{ error }}</div> }

      @if (ticket) {
        <div class="detail-layout">
          <div class="detail-main">
            <div class="ticket-header">
              <div class="ticket-meta">
                <span class="status-badge status-{{ ticket.status.toLowerCase() }}">{{ ticket.status }}</span>
                <span class="priority-badge priority-{{ ticket.priority.toLowerCase() }}">{{ ticket.priority }}</span>
                <span class="category-badge">{{ ticket.category }}</span>
              </div>
              <h1>{{ ticket.title }}</h1>
              @if (ticket.externalRef) { <div class="ext-ref">Ref: {{ ticket.externalRef }}</div> }
              <div class="ticket-info">
                <span>Created by {{ ticket.createdBy.fullName }}</span>
                <span>on {{ ticket.createdAt | date }}</span>
                @if (ticket.assignedTo) { <span>Assigned to {{ ticket.assignedTo.fullName }}</span> }
              </div>
            </div>

            <div class="section-card">
              <h3>Description</h3>
              <p class="description">{{ ticket.description }}</p>
            </div>

            <div class="section-card">
              <h3>Change Status</h3>
              <div class="status-actions">
                @for (s of getAvailableStatuses(); track s) {
                  <button (click)="changeStatus(s)" class="btn-status">{{ s }}</button>
                }
              </div>
            </div>

            <div class="section-card">
              <h3>Assignment</h3>
              <div class="assign-current">
                @if (ticket.assignedTo) {
                  <span class="assigned-label">Assigned to <strong>{{ ticket.assignedTo.fullName }}</strong></span>
                  <button (click)="unassign()" class="btn-unassign">Unassign</button>
                } @else {
                  <span class="unassigned-label">Not assigned</span>
                }
              </div>
              <div class="assign-actions">
                <button (click)="assignToMe()" class="btn-assign-me">Assign to me</button>
                @if (auth.isAdmin() && operators.length > 0) {
                  <select (change)="assignToOperator($event)" class="assign-select">
                    <option value="">— assign to operator —</option>
                    @for (op of operators; track op.id) {
                      <option [value]="op.id">{{ op.fullName }}</option>
                    }
                  </select>
                }
              </div>
            </div>

            <div class="section-card">
              <h3>Notes</h3>
              @for (note of notes; track note.id) {
                <div class="note-item" [class]="'note-' + note.visibility.toLowerCase()">
                  <div class="note-header">
                    <span class="note-author">{{ note.authorName }}</span>
                    <span class="note-visibility">{{ note.visibility }}</span>
                    <span class="note-date">{{ note.createdAt | date:'short' }}</span>
                  </div>
                  <div class="note-body">{{ note.body }}</div>
                </div>
              }
              <form [formGroup]="noteForm" (ngSubmit)="addNote()" class="note-form">
                <textarea formControlName="body" rows="3" placeholder="Add internal note..."></textarea>
                <button type="submit" [disabled]="noteForm.invalid || noteLoading" class="btn-primary">
                  {{ noteLoading ? 'Adding...' : 'Add Note' }}
                </button>
              </form>
            </div>
          </div>

          <div class="detail-sidebar">
            <div class="section-card ai-panel">
              <h3>AI Assistant</h3>
              <div class="ai-actions">
                <button (click)="generateSummary()" [disabled]="aiLoading" class="btn-ai">Summarize Ticket</button>
                <button (click)="suggestReply()" [disabled]="aiLoading" class="btn-ai">Suggest Reply</button>
              </div>
              <div class="chat-messages" #chatContainer>
                @for (msg of chatMessages; track msg.id) {
                  <div class="chat-message" [class]="'msg-' + msg.role.toLowerCase()">
                    <div class="msg-role">{{ msg.role }}</div>
                    <div class="msg-content" [class.msg-error]="msg.error">{{ msg.content }}</div>
                    @if (msg.role === 'ASSISTANT' && !msg.error) {
                      <button (click)="applyAsNote(msg)" class="btn-apply-note">Apply as Note</button>
                    }
                  </div>
                }
                @if (aiLoading) { <div class="ai-loading">AI is thinking...</div> }
              </div>
              <form [formGroup]="chatForm" (ngSubmit)="sendChatMessage()" class="chat-form">
                <input type="text" formControlName="content" placeholder="Ask AI about this ticket..." />
                <button type="submit" [disabled]="chatForm.invalid || aiLoading" class="btn-send">Send</button>
              </form>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .page-container { padding: 32px; }
    .back-link { color: #6366f1; text-decoration: none; font-size: 14px; display: block; margin-bottom: 16px; }
    .loading { padding: 20px; color: #6366f1; }
    .alert-error { background: #fee2e2; color: #dc2626; padding: 12px; border-radius: 8px; margin-bottom: 20px; }
    .detail-layout { display: grid; grid-template-columns: 1fr 380px; gap: 24px; }
    .detail-main { display: flex; flex-direction: column; gap: 20px; }
    .ticket-header { background: white; border-radius: 12px; padding: 24px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
    .ticket-meta { display: flex; gap: 8px; margin-bottom: 12px; flex-wrap: wrap; }
    .ticket-header h1 { font-size: 22px; color: #1a1f36; margin: 0 0 8px; }
    .ext-ref { font-size: 13px; color: #94a3b8; margin-bottom: 8px; }
    .ticket-info { display: flex; gap: 16px; font-size: 13px; color: #64748b; flex-wrap: wrap; }
    .section-card { background: white; border-radius: 12px; padding: 24px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
    .section-card h3 { font-size: 15px; color: #374151; margin: 0 0 16px; font-weight: 600; }
    .description { color: #374151; line-height: 1.7; white-space: pre-wrap; }
    .status-actions { display: flex; gap: 8px; flex-wrap: wrap; }
    .btn-status { padding: 6px 14px; border: 1px solid #d1d5db; border-radius: 6px; cursor: pointer; font-size: 13px; background: white; }
    .btn-status:hover { background: #6366f1; color: white; border-color: #6366f1; }
    .note-item { padding: 12px; border-radius: 8px; margin-bottom: 12px; }
    .note-internal { background: #f8fafc; border-left: 3px solid #6366f1; }
    .note-ai_summary { background: #f0fdf4; border-left: 3px solid #22c55e; }
    .note-system { background: #fefce8; border-left: 3px solid #eab308; }
    .note-header { display: flex; gap: 12px; margin-bottom: 6px; font-size: 12px; color: #64748b; }
    .note-author { font-weight: 600; color: #374151; }
    .note-body { font-size: 14px; color: #374151; white-space: pre-wrap; }
    .note-form { margin-top: 16px; }
    .note-form textarea { width: 100%; padding: 10px; border: 1px solid #d1d5db; border-radius: 8px; font-size: 14px; resize: vertical; box-sizing: border-box; font-family: inherit; }
    .btn-primary { margin-top: 8px; padding: 8px 20px; background: #6366f1; color: white; border: none; border-radius: 8px; font-weight: 600; cursor: pointer; }
    .btn-primary:disabled { opacity: 0.6; }
    .ai-panel { display: flex; flex-direction: column; height: fit-content; }
    .ai-actions { display: flex; gap: 8px; margin-bottom: 16px; }
    .btn-ai { flex: 1; padding: 8px 12px; background: #ede9fe; color: #5b21b6; border: none; border-radius: 8px; cursor: pointer; font-size: 13px; font-weight: 600; }
    .btn-ai:hover { background: #ddd6fe; }
    .btn-ai:disabled { opacity: 0.5; }
    .chat-messages { max-height: 400px; overflow-y: auto; display: flex; flex-direction: column; gap: 10px; margin-bottom: 16px; min-height: 100px; }
    .chat-message { padding: 10px 14px; border-radius: 10px; font-size: 13px; }
    .msg-user { background: #ede9fe; margin-left: 20%; }
    .msg-assistant { background: #f0fdf4; margin-right: 20%; }
    .msg-system { background: #fefce8; }
    .msg-role { font-size: 11px; font-weight: 600; color: #64748b; margin-bottom: 4px; text-transform: uppercase; }
    .msg-content { color: #374151; line-height: 1.5; white-space: pre-wrap; }
    .msg-error { color: #dc2626; }
    .btn-apply-note { margin-top: 8px; padding: 4px 10px; background: transparent; border: 1px solid #6366f1; color: #6366f1; border-radius: 6px; cursor: pointer; font-size: 12px; }
    .ai-loading { padding: 12px; color: #6366f1; font-size: 13px; text-align: center; }
    .chat-form { display: flex; gap: 8px; }
    .chat-form input { flex: 1; padding: 8px 12px; border: 1px solid #d1d5db; border-radius: 8px; font-size: 14px; }
    .btn-send { padding: 8px 16px; background: #6366f1; color: white; border: none; border-radius: 8px; cursor: pointer; }
    .btn-send:disabled { opacity: 0.5; }
    .status-badge, .priority-badge, .category-badge { display: inline-block; padding: 3px 10px; border-radius: 20px; font-size: 12px; font-weight: 600; text-transform: uppercase; }
    .status-new { background: #dbeafe; color: #1d4ed8; }
    .status-in_progress { background: #fef9c3; color: #854d0e; }
    .status-waiting_for_customer { background: #fce7f3; color: #9d174d; }
    .status-resolved { background: #dcfce7; color: #15803d; }
    .status-closed { background: #f1f5f9; color: #64748b; }
    .priority-low { background: #dcfce7; color: #15803d; }
    .priority-medium { background: #fef9c3; color: #854d0e; }
    .priority-high { background: #fed7aa; color: #9a3412; }
    .priority-critical { background: #fee2e2; color: #dc2626; }
    .category-badge { background: #e0e7ff; color: #3730a3; }
    .assign-current { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
    .assigned-label { font-size: 14px; color: #374151; }
    .unassigned-label { font-size: 14px; color: #94a3b8; }
    .btn-unassign { padding: 4px 12px; border: 1px solid #dc2626; color: #dc2626; background: white; border-radius: 6px; cursor: pointer; font-size: 13px; }
    .btn-unassign:hover { background: #fee2e2; }
    .assign-actions { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; }
    .btn-assign-me { padding: 6px 16px; background: #6366f1; color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 13px; font-weight: 600; }
    .btn-assign-me:hover { background: #4f46e5; }
    .assign-select { padding: 6px 12px; border: 1px solid #d1d5db; border-radius: 8px; font-size: 13px; background: white; cursor: pointer; }
    @media (max-width: 900px) { .detail-layout { grid-template-columns: 1fr; } }
  `]
})
export class TicketDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private ticketService = inject(TicketService);
  private chatService = inject(ChatService);
  private userService = inject(UserService);
  auth = inject(AuthService);
  private fb = inject(FormBuilder);

  ticket: Ticket | null = null;
  notes: TicketNote[] = [];
  chatMessages: ChatMessage[] = [];
  chatSession: ChatSession | null = null;
  operators: User[] = [];
  loading = false;
  error = '';
  aiLoading = false;
  noteLoading = false;

  noteForm = this.fb.group({ body: ['', [Validators.required, Validators.minLength(1)]] });
  chatForm = this.fb.group({ content: ['', Validators.required] });

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.loading = true;
    this.ticketService.getById(id).subscribe({
      next: t => {
        this.ticket = t;
        this.loading = false;
        this.loadNotes();
        this.initChat();
      },
      error: () => { this.error = 'Ticket not found'; this.loading = false; }
    });
    if (this.auth.isAdmin()) {
      this.userService.listAll().subscribe(users => this.operators = users);
    }
  }

  loadNotes() {
    this.ticketService.getNotes(this.ticket!.id).subscribe(n => this.notes = n);
  }

  initChat() {
    this.chatService.getOrCreateSession(this.ticket!.id).subscribe(session => {
      this.chatSession = session;
      this.chatService.getMessages(session.id).subscribe(msgs => this.chatMessages = msgs);
    });
  }

  changeStatus(status: string) {
    this.ticketService.changeStatus(this.ticket!.id, status).subscribe({
      next: t => this.ticket = t,
      error: err => alert(err.error?.message || 'Invalid transition')
    });
  }

  addNote() {
    if (!this.noteForm.value.body) return;
    this.noteLoading = true;
    this.ticketService.addNote(this.ticket!.id, this.noteForm.value.body).subscribe({
      next: n => { this.notes.push(n); this.noteForm.reset(); this.noteLoading = false; },
      error: () => this.noteLoading = false
    });
  }

  sendChatMessage() {
    if (!this.chatSession || !this.chatForm.value.content) return;
    const content = this.chatForm.value.content!;
    this.chatForm.reset();
    const userMsg: ChatMessage = { id: 'temp', sessionId: this.chatSession.id, role: 'USER', content, error: false, createdAt: new Date().toISOString() };
    this.chatMessages.push(userMsg);
    this.aiLoading = true;
    this.chatService.sendMessage(this.chatSession.id, content).subscribe({
      next: msg => { this.chatMessages.push(msg); this.aiLoading = false; },
      error: () => this.aiLoading = false
    });
  }

  generateSummary() {
    this.aiLoading = true;
    this.chatService.generateSummary(this.ticket!.id).subscribe({
      next: msg => { this.chatMessages.push(msg); this.aiLoading = false; },
      error: () => this.aiLoading = false
    });
  }

  suggestReply() {
    this.aiLoading = true;
    this.chatService.suggestReply(this.ticket!.id).subscribe({
      next: msg => { this.chatMessages.push(msg); this.aiLoading = false; },
      error: () => this.aiLoading = false
    });
  }

  applyAsNote(msg: ChatMessage) {
    this.chatService.applyAsSummary(this.ticket!.id, msg.id).subscribe({
      next: note => { this.notes.push(note); alert('AI summary applied as note!'); },
      error: () => alert('Failed to apply note')
    });
  }

  assignToMe() {
    const me = this.auth.currentUser();
    if (!me) return;
    this.ticketService.assign(this.ticket!.id, me.id).subscribe({
      next: t => this.ticket = t,
      error: () => this.error = 'Failed to assign ticket'
    });
  }

  unassign() {
    this.ticketService.assign(this.ticket!.id, null).subscribe({
      next: t => this.ticket = t,
      error: () => this.error = 'Failed to unassign ticket'
    });
  }

  assignToOperator(event: Event) {
    const operatorId = (event.target as HTMLSelectElement).value;
    if (!operatorId) return;
    this.ticketService.assign(this.ticket!.id, operatorId).subscribe({
      next: t => {
        this.ticket = t;
        (event.target as HTMLSelectElement).value = '';
      },
      error: () => this.error = 'Failed to assign ticket'
    });
  }

  getAvailableStatuses(): string[] {
    const transitions: Record<string, string[]> = {
      'NEW': ['IN_PROGRESS', 'CLOSED'],
      'IN_PROGRESS': ['WAITING_FOR_CUSTOMER', 'RESOLVED', 'CLOSED'],
      'WAITING_FOR_CUSTOMER': ['IN_PROGRESS', 'RESOLVED', 'CLOSED'],
      'RESOLVED': ['CLOSED', 'IN_PROGRESS'],
      'CLOSED': []
    };
    return transitions[this.ticket?.status || ''] || [];
  }
}

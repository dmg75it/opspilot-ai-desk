import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { NgIf, NgFor, DatePipe } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDividerModule } from '@angular/material/divider';
import { MatTabsModule } from '@angular/material/tabs';
import { TicketService } from '../../../core/services/ticket.service';
import { NoteService } from '../../../core/services/note.service';
import { Ticket, Note } from '../../../core/models/ticket.model';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { ErrorBannerComponent } from '../../../shared/components/error-banner/error-banner.component';
import { AiChatPanelComponent } from '../../ai-chat/ai-chat-panel.component';

@Component({
  selector: 'app-ticket-detail',
  standalone: true,
  imports: [NgIf, NgFor, DatePipe, ReactiveFormsModule, MatCardModule, MatButtonModule,
    MatSelectModule, MatFormFieldModule, MatInputModule, MatDividerModule,
    MatTabsModule, LoadingSpinnerComponent, ErrorBannerComponent, AiChatPanelComponent],
  template: `
    <app-loading-spinner *ngIf="loading()"></app-loading-spinner>
    <app-error-banner [message]="error()"></app-error-banner>

    <div *ngIf="ticket()">
      <div style="display:flex;justify-content:space-between;align-items:flex-start">
        <div>
          <h1>{{ ticket()!.title }}</h1>
          <p style="color:grey">{{ ticket()!.externalRef ? '#' + ticket()!.externalRef + ' · ' : '' }}{{ ticket()!.status }} · {{ ticket()!.priority }} · {{ ticket()!.category }}</p>
        </div>
        <div style="display:flex;gap:0.5rem;align-items:center">
          <mat-form-field style="width:200px">
            <mat-label>Change Status</mat-label>
            <mat-select [formControl]="statusControl">
              <mat-option value="NEW">NEW</mat-option>
              <mat-option value="IN_PROGRESS">IN_PROGRESS</mat-option>
              <mat-option value="WAITING_FOR_CUSTOMER">WAITING_FOR_CUSTOMER</mat-option>
              <mat-option value="RESOLVED">RESOLVED</mat-option>
              <mat-option value="CLOSED">CLOSED</mat-option>
            </mat-select>
          </mat-form-field>
          <button mat-raised-button color="accent" (click)="changeStatus()" [disabled]="statusControl.value === ticket()!.status">Apply</button>
        </div>
      </div>

      <mat-card style="margin-bottom:1rem">
        <mat-card-content>
          <p><strong>Description:</strong></p>
          <p style="white-space:pre-wrap">{{ ticket()!.description }}</p>
          <mat-divider style="margin:1rem 0"></mat-divider>
          <p><strong>Created by:</strong> {{ ticket()!.createdByEmail }} · <strong>Assigned:</strong> {{ ticket()!.assignedToEmail || 'Unassigned' }}</p>
          <p><strong>Created:</strong> {{ ticket()!.createdAt | date:'medium' }} · <strong>Updated:</strong> {{ ticket()!.updatedAt | date:'medium' }}</p>
        </mat-card-content>
      </mat-card>

      <mat-tab-group>
        <mat-tab label="Notes">
          <div style="padding:1rem">
            <div *ngFor="let note of notes()" style="margin-bottom:0.75rem;padding:0.75rem;background:#f5f5f5;border-radius:4px">
              <small><strong>{{ note.visibility }}</strong> · {{ note.authorEmail || 'system' }} · {{ note.createdAt | date:'short' }}</small>
              <p style="margin:0.5rem 0 0;white-space:pre-wrap">{{ note.body }}</p>
            </div>
            <div style="display:flex;gap:0.5rem;margin-top:1rem">
              <mat-form-field style="flex:1">
                <mat-label>Add note</mat-label>
                <textarea matInput [formControl]="noteControl" rows="3"></textarea>
              </mat-form-field>
              <button mat-raised-button (click)="addNote()" [disabled]="!noteControl.value">Add</button>
            </div>
          </div>
        </mat-tab>
        <mat-tab label="AI Assistant">
          <div style="padding:1rem">
            <app-ai-chat-panel [ticketId]="ticket()!.id"></app-ai-chat-panel>
          </div>
        </mat-tab>
      </mat-tab-group>
    </div>
  `
})
export class TicketDetailComponent implements OnInit {
  ticket = signal<Ticket | null>(null);
  notes = signal<Note[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  statusControl = new FormControl('');
  noteControl = new FormControl('');

  constructor(
    private route: ActivatedRoute,
    private ticketService: TicketService,
    private noteService: NoteService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.ticketService.getById(id).subscribe({
      next: t => {
        this.ticket.set(t);
        this.statusControl.setValue(t.status);
        this.loading.set(false);
        this.loadNotes(id);
      },
      error: () => { this.error.set('Ticket not found'); this.loading.set(false); }
    });
  }

  loadNotes(id: string): void {
    this.noteService.listNotes(id).subscribe({
      next: notes => this.notes.set(notes),
      error: () => {}
    });
  }

  changeStatus(): void {
    const newStatus = this.statusControl.value;
    const t = this.ticket();
    if (!newStatus || !t) return;
    this.ticketService.changeStatus(t.id, { status: newStatus }).subscribe({
      next: updated => { this.ticket.set(updated); this.statusControl.setValue(updated.status); },
      error: () => { this.error.set('Invalid status transition'); }
    });
  }

  addNote(): void {
    const body = this.noteControl.value?.trim();
    const t = this.ticket();
    if (!body || !t) return;
    this.noteService.addNote(t.id, body).subscribe({
      next: note => { this.notes.update(arr => [...arr, note]); this.noteControl.setValue(''); },
      error: () => { this.error.set('Failed to add note'); }
    });
  }
}

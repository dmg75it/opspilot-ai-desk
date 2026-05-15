import { Component, Input, OnInit } from '@angular/core';
import { NgIf, NgFor } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { AiService } from '../../core/services/ai.service';
import { ChatMessage } from '../../core/models/chat.model';

@Component({
  selector: 'app-ai-chat-panel',
  standalone: true,
  imports: [NgIf, NgFor, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatProgressSpinnerModule, MatDividerModule],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>AI Assistant</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <div style="display:flex;gap:0.5rem;margin-bottom:1rem;flex-wrap:wrap">
          <button mat-stroked-button (click)="getSummary()" [disabled]="aiLoading">Summary</button>
          <button mat-stroked-button (click)="getSuggestedReply()" [disabled]="aiLoading">Suggested Reply</button>
        </div>

        <div *ngIf="actionResult" style="background:#e8f5e9;padding:1rem;border-radius:4px;margin-bottom:1rem;white-space:pre-wrap">
          {{ actionResult }}
          <div style="margin-top:0.5rem">
            <button mat-button color="primary" (click)="applyAsNote()">Apply as Note</button>
          </div>
        </div>

        <div *ngIf="noteApplied" style="color:green;margin-bottom:1rem">Note added to ticket.</div>
        <div *ngIf="aiError" style="color:red;margin-bottom:1rem">{{ aiError }}</div>

        <mat-divider style="margin-bottom:1rem"></mat-divider>

        <div style="max-height:300px;overflow-y:auto;margin-bottom:1rem">
          <div *ngFor="let msg of messages"
               [style.text-align]="msg.role === 'USER' ? 'right' : 'left'"
               style="margin-bottom:0.75rem">
            <span [style.background]="msg.role === 'USER' ? '#E3F2FD' : '#F3E5F5'"
                  style="padding:0.5rem 1rem;border-radius:12px;display:inline-block;max-width:80%;white-space:pre-wrap">
              <strong>{{ msg.role }}</strong><br>
              {{ msg.error ? 'Error: ' + msg.errorMessage : msg.content }}
            </span>
          </div>
        </div>

        <div style="display:flex;gap:0.5rem">
          <mat-form-field style="flex:1">
            <mat-label>Message</mat-label>
            <input matInput [formControl]="messageControl" (keyup.enter)="sendMessage()">
          </mat-form-field>
          <button mat-raised-button color="primary" (click)="sendMessage()" [disabled]="aiLoading || !messageControl.value">
            <mat-spinner *ngIf="aiLoading" diameter="20"></mat-spinner>
            <span *ngIf="!aiLoading">Send</span>
          </button>
        </div>
      </mat-card-content>
    </mat-card>
  `
})
export class AiChatPanelComponent implements OnInit {
  @Input() ticketId!: string;
  messages: ChatMessage[] = [];
  messageControl = new FormControl('');
  aiLoading = false;
  aiError: string | null = null;
  actionResult: string | null = null;
  noteApplied = false;

  constructor(private aiService: AiService) {}

  ngOnInit(): void {
    this.aiService.getSession(this.ticketId).subscribe({
      next: session => this.messages = session.messages,
      error: () => {}
    });
  }

  sendMessage(): void {
    const content = this.messageControl.value?.trim();
    if (!content) return;
    this.aiLoading = true;
    this.aiError = null;
    this.messageControl.setValue('');
    this.messages.push({ id: '', role: 'USER', content, createdAt: new Date().toISOString(), error: false });
    this.aiService.sendMessage(this.ticketId, content).subscribe({
      next: msg => { this.messages.push(msg); this.aiLoading = false; },
      error: () => { this.aiError = 'Failed to send message'; this.aiLoading = false; }
    });
  }

  getSummary(): void {
    this.aiLoading = true;
    this.actionResult = null;
    this.noteApplied = false;
    this.aiService.generateSummary(this.ticketId).subscribe({
      next: r => { this.actionResult = r.content; this.aiLoading = false; },
      error: () => { this.aiError = 'Failed to generate summary'; this.aiLoading = false; }
    });
  }

  getSuggestedReply(): void {
    this.aiLoading = true;
    this.actionResult = null;
    this.noteApplied = false;
    this.aiService.generateSuggestedReply(this.ticketId).subscribe({
      next: r => { this.actionResult = r.content; this.aiLoading = false; },
      error: () => { this.aiError = 'Failed to generate reply'; this.aiLoading = false; }
    });
  }

  applyAsNote(): void {
    if (!this.actionResult) return;
    this.aiService.applySummaryAsNote(this.ticketId, this.actionResult).subscribe({
      next: () => { this.noteApplied = true; this.actionResult = null; },
      error: () => { this.aiError = 'Failed to apply note'; }
    });
  }
}

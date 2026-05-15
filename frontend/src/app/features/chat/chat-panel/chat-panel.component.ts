import { Component, inject, input, OnInit, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ChatService } from '../../../core/services/chat.service';
import { ChatMessage } from '../../../core/models/chat.model';

@Component({
  selector: 'app-chat-panel',
  standalone: true,
  imports: [
    FormsModule, DatePipe, MatCardModule, MatButtonModule, MatIconModule,
    MatInputModule, MatFormFieldModule, MatProgressSpinnerModule,
    MatDividerModule, MatTooltipModule
  ],
  template: `
    <mat-card class="chat-card">
      <mat-card-header>
        <mat-card-title>
          <mat-icon>smart_toy</mat-icon>
          AI Assistant
        </mat-card-title>
        <mat-card-actions align="end">
          <button mat-button (click)="generateSummary()" [disabled]="aiLoading()">
            <mat-icon>summarize</mat-icon> Summary
          </button>
          <button mat-button (click)="generateReply()" [disabled]="aiLoading()">
            <mat-icon>reply</mat-icon> Suggest Reply
          </button>
        </mat-card-actions>
      </mat-card-header>

      <mat-card-content class="messages-container">
        @if (messages().length === 0) {
          <p class="empty-chat">Ask a question or generate a summary to get started.</p>
        }
        @for (msg of messages(); track msg.id) {
          <div class="message" [class]="'message-' + msg.role.toLowerCase()">
            <div class="message-header">
              <span class="role-label">{{ msg.role }}</span>
              <span class="message-time">{{ msg.createdAt | date:'HH:mm' }}</span>
              @if (msg.role === 'ASSISTANT' && !msg.errorFlag) {
                <button mat-icon-button class="apply-btn" [matTooltip]="'Apply as note'"
                        (click)="applyAsNote(msg)">
                  <mat-icon>note_add</mat-icon>
                </button>
              }
            </div>
            @if (msg.errorFlag) {
              <div class="error-content">
                <mat-icon>error</mat-icon>
                <span>{{ msg.content }}</span>
              </div>
            } @else {
              <div class="message-content">{{ msg.content }}</div>
            }
            @if (msg.model) {
              <div class="message-meta">{{ msg.model }} {{ msg.tokenEstimate ? '· ' + msg.tokenEstimate + ' tokens' : '' }}</div>
            }
          </div>
        }
        @if (aiLoading()) {
          <div class="ai-loading">
            <mat-spinner diameter="24"></mat-spinner>
            <span>AI is thinking...</span>
          </div>
        }
      </mat-card-content>

      <mat-divider></mat-divider>
      <mat-card-actions class="input-area">
        <mat-form-field appearance="outline" class="message-input">
          <mat-label>Message</mat-label>
          <input matInput [(ngModel)]="userInput" (keyup.enter)="sendMessage()"
                 placeholder="Ask about this ticket..." [disabled]="aiLoading()">
        </mat-form-field>
        <button mat-icon-button color="primary" (click)="sendMessage()"
                [disabled]="!userInput.trim() || aiLoading()">
          <mat-icon>send</mat-icon>
        </button>
      </mat-card-actions>
    </mat-card>

    @if (noteApplied()) {
      <div class="note-success">
        <mat-icon>check_circle</mat-icon> AI summary applied as note.
      </div>
    }
  `,
  styles: [`
    .chat-card { height: 600px; display: flex; flex-direction: column; }
    mat-card-header { flex-shrink: 0; align-items: center; }
    mat-card-title { display: flex; align-items: center; gap: 8px; font-size: 16px; }
    .messages-container { flex: 1; overflow-y: auto; padding: 8px; min-height: 0; }
    .empty-chat { color: #999; text-align: center; padding: 32px; }
    .message { margin-bottom: 12px; padding: 12px; border-radius: 8px; position: relative; }
    .message-user { background: #e3f2fd; margin-left: 32px; }
    .message-assistant { background: #f3e5f5; margin-right: 32px; }
    .message-system { background: #f5f5f5; font-size: 12px; }
    .message-header { display: flex; align-items: center; gap: 8px; margin-bottom: 4px; }
    .role-label { font-size: 11px; font-weight: bold; text-transform: uppercase; color: #666; }
    .message-time { font-size: 11px; color: #999; margin-left: auto; }
    .apply-btn { width: 24px; height: 24px; line-height: 24px; }
    .message-content { white-space: pre-wrap; word-break: break-word; }
    .message-meta { font-size: 11px; color: #999; margin-top: 4px; }
    .error-content { display: flex; align-items: center; gap: 8px; color: #f44336; }
    .ai-loading { display: flex; align-items: center; gap: 12px; padding: 12px; color: #999; }
    .input-area { display: flex; align-items: center; gap: 8px; padding: 8px 16px; flex-shrink: 0; }
    .message-input { flex: 1; }
    .note-success { display: flex; align-items: center; gap: 8px; color: #388e3c; margin-top: 8px; }
  `]
})
export class ChatPanelComponent implements OnInit {
  ticketId = input.required<string>();
  noteAdded = output<void>();
  private chatService = inject(ChatService);

  messages = signal<ChatMessage[]>([]);
  aiLoading = signal(false);
  noteApplied = signal(false);
  userInput = '';

  ngOnInit(): void {
    this.chatService.getOrCreateSession(this.ticketId()).subscribe({
      next: () => this.loadMessages(),
      error: () => {}
    });
  }

  sendMessage(): void {
    const content = this.userInput.trim();
    if (!content) return;
    this.userInput = '';
    const userMsg: ChatMessage = {
      id: 'temp-' + Date.now(), sessionId: '', role: 'USER',
      content, createdAt: new Date().toISOString(), errorFlag: false
    };
    this.messages.update(msgs => [...msgs, userMsg]);
    this.aiLoading.set(true);
    this.chatService.sendMessage(this.ticketId(), content).subscribe({
      next: msg => { this.messages.update(msgs => [...msgs, msg]); this.aiLoading.set(false); },
      error: () => this.aiLoading.set(false)
    });
  }

  generateSummary(): void {
    this.aiLoading.set(true);
    this.chatService.generateSummary(this.ticketId()).subscribe({
      next: msg => { this.messages.update(msgs => [...msgs, msg]); this.aiLoading.set(false); },
      error: () => this.aiLoading.set(false)
    });
  }

  generateReply(): void {
    this.aiLoading.set(true);
    this.chatService.generateSuggestedReply(this.ticketId()).subscribe({
      next: msg => { this.messages.update(msgs => [...msgs, msg]); this.aiLoading.set(false); },
      error: () => this.aiLoading.set(false)
    });
  }

  applyAsNote(msg: ChatMessage): void {
    this.chatService.applyAsNote(this.ticketId(), msg.id).subscribe({
      next: () => {
        this.noteApplied.set(true);
        this.noteAdded.emit();
        setTimeout(() => this.noteApplied.set(false), 3000);
      }
    });
  }

  private loadMessages(): void {
    this.chatService.listMessages(this.ticketId()).subscribe({
      next: msgs => this.messages.set(msgs)
    });
  }
}

import { Component, Input, inject, OnInit, AfterViewChecked, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { ChatService } from '../../../../core/services/chat.service';
import { ChatMessage } from '../../../../core/models/chat.model';

@Component({
  selector: 'app-ai-chat',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatIconModule
  ],
  templateUrl: './ai-chat.component.html',
  styleUrls: ['./ai-chat.component.scss']
})
export class AiChatComponent implements OnInit, AfterViewChecked {
  @Input() ticketId!: number;
  @ViewChild('messagesEnd') messagesEnd!: ElementRef;

  private chatService = inject(ChatService);
  private fb = inject(FormBuilder);

  messages: ChatMessage[] = [];
  loading = false;
  aiLoading = false;
  errorMessage = '';

  form = this.fb.group({
    content: ['', Validators.required]
  });

  ngOnInit(): void {
    this.loading = true;
    this.chatService.getMessages(this.ticketId).subscribe({
      next: (msgs) => {
        this.messages = msgs.filter(m => m.role !== 'SYSTEM');
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  ngAfterViewChecked(): void {
    this.scrollToBottom();
  }

  send(): void {
    if (this.form.invalid) return;
    const content = this.form.value.content!;
    this.form.reset();
    this.aiLoading = true;
    this.errorMessage = '';
    this.chatService.sendMessage(this.ticketId, content).subscribe({
      next: (msg) => {
        this.messages = [...this.messages, msg];
        this.aiLoading = false;
        this.loadLatestMessages();
      },
      error: (err) => {
        this.aiLoading = false;
        this.errorMessage = 'AI request failed. Please try again.';
      }
    });
  }

  summarize(): void {
    this.aiLoading = true;
    this.errorMessage = '';
    this.chatService.summarize(this.ticketId).subscribe({
      next: (msg) => {
        this.messages = [...this.messages, msg];
        this.aiLoading = false;
      },
      error: () => {
        this.aiLoading = false;
        this.errorMessage = 'Summarize request failed. Please try again.';
      }
    });
  }

  suggestReply(): void {
    this.aiLoading = true;
    this.errorMessage = '';
    this.chatService.suggestReply(this.ticketId).subscribe({
      next: (msg) => {
        this.messages = [...this.messages, msg];
        this.aiLoading = false;
      },
      error: () => {
        this.aiLoading = false;
        this.errorMessage = 'Suggest reply request failed. Please try again.';
      }
    });
  }

  applyAsNote(msg: ChatMessage): void {
    this.chatService.applySummary(this.ticketId, msg.id).subscribe({
      next: () => { /* note applied */ },
      error: () => { this.errorMessage = 'Failed to apply summary as note.'; }
    });
  }

  private loadLatestMessages(): void {
    this.chatService.getMessages(this.ticketId).subscribe({
      next: (msgs) => { this.messages = msgs.filter(m => m.role !== 'SYSTEM'); }
    });
  }

  onEnter(event: Event): void {
    const ke = event as KeyboardEvent;
    if (!ke.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  private scrollToBottom(): void {
    try {
      if (this.messagesEnd) {
        this.messagesEnd.nativeElement.scrollIntoView({ behavior: 'smooth' });
      }
    } catch {}
  }
}

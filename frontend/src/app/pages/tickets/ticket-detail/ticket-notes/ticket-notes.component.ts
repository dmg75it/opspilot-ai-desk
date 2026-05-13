import { Component, Input, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TicketService } from '../../../../core/services/ticket.service';
import { TicketNote } from '../../../../core/models/ticket.model';

@Component({
  selector: 'app-ticket-notes',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatChipsModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './ticket-notes.component.html'
})
export class TicketNotesComponent implements OnInit {
  @Input() ticketId!: number;

  private ticketService = inject(TicketService);
  private fb = inject(FormBuilder);

  notes: TicketNote[] = [];
  loading = false;
  submitting = false;

  form = this.fb.group({
    body: ['', [Validators.required, Validators.minLength(1)]]
  });

  ngOnInit(): void {
    this.loadNotes();
  }

  loadNotes(): void {
    this.loading = true;
    this.ticketService.getNotes(this.ticketId).subscribe({
      next: (notes) => { this.notes = notes; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  submit(): void {
    if (this.form.invalid) return;
    this.submitting = true;
    this.ticketService.addNote(this.ticketId, this.form.value.body!).subscribe({
      next: (note) => {
        this.notes = [...this.notes, note];
        this.form.reset();
        this.submitting = false;
      },
      error: () => { this.submitting = false; }
    });
  }

  visibilityClass(visibility: string): string {
    const map: Record<string, string> = {
      'INTERNAL': 'internal',
      'AI_SUMMARY': 'ai-summary',
      'SYSTEM': 'system'
    };
    return map[visibility] ?? '';
  }
}

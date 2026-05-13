import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TicketService } from '../../../core/services/ticket.service';
import { TicketPriority, TicketCategory } from '../../../core/models/ticket.model';

@Component({
  selector: 'app-ticket-create',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './ticket-create.component.html'
})
export class TicketCreateComponent {
  private fb = inject(FormBuilder);
  private ticketService = inject(TicketService);
  private router = inject(Router);

  readonly priorities: TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  readonly categories: TicketCategory[] = ['DELIVERY', 'PICKUP', 'DOCUMENTATION', 'CUSTOMER', 'SYSTEM', 'OTHER'];

  form = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['', [Validators.required, Validators.maxLength(5000)]],
    priority: ['MEDIUM' as TicketPriority, Validators.required],
    category: ['OTHER' as TicketCategory, Validators.required],
    externalRef: ['']
  });

  loading = false;
  errorMessage = '';

  submit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.errorMessage = '';
    const { title, description, priority, category, externalRef } = this.form.value;
    this.ticketService.create({
      title: title!,
      description: description!,
      priority: priority!,
      category: category!,
      externalRef: externalRef || undefined
    }).subscribe({
      next: (ticket) => this.router.navigate(['/tickets', ticket.id]),
      error: () => {
        this.loading = false;
        this.errorMessage = 'Failed to create ticket. Please try again.';
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/tickets']);
  }
}

import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { TicketService } from '../../services/ticket.service';

@Component({
  selector: 'app-ticket-create',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, RouterLink],
  template: `
    <div class="page-container">
      <div class="page-header">
        <a routerLink="/tickets" class="back-link">← Back to Tickets</a>
        <h1>New Ticket</h1>
      </div>
      <div class="form-card">
        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label>Title *</label>
            <input type="text" formControlName="title" placeholder="Brief issue description" maxlength="150" />
            @if (form.get('title')?.invalid && form.get('title')?.touched) {
              <span class="error">Title is required (max 150 chars)</span>
            }
          </div>
          <div class="form-group">
            <label>External Reference</label>
            <input type="text" formControlName="externalRef" placeholder="Order #, shipment ID, etc." />
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>Priority *</label>
              <select formControlName="priority">
                <option *ngFor="let p of priorities" [value]="p">{{ p }}</option>
              </select>
            </div>
            <div class="form-group">
              <label>Category *</label>
              <select formControlName="category">
                <option *ngFor="let c of categories" [value]="c">{{ c }}</option>
              </select>
            </div>
          </div>
          <div class="form-group">
            <label>Description *</label>
            <textarea formControlName="description" rows="8" placeholder="Detailed description of the issue..." maxlength="5000"></textarea>
            @if (form.get('description')?.invalid && form.get('description')?.touched) {
              <span class="error">Description is required (max 5000 chars)</span>
            }
          </div>
          @if (error) { <div class="alert-error">{{ error }}</div> }
          <div class="form-actions">
            <button type="button" routerLink="/tickets" class="btn-secondary">Cancel</button>
            <button type="submit" [disabled]="loading || form.invalid" class="btn-primary">
              {{ loading ? 'Creating...' : 'Create Ticket' }}
            </button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .page-container { padding: 32px; max-width: 800px; }
    .back-link { color: #6366f1; text-decoration: none; font-size: 14px; }
    .page-header h1 { font-size: 28px; color: #1a1f36; margin: 8px 0 24px; }
    .form-card { background: white; border-radius: 12px; padding: 32px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); }
    .form-group { margin-bottom: 20px; }
    .form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
    .form-group label { display: block; margin-bottom: 6px; font-weight: 500; color: #374151; font-size: 14px; }
    .form-group input, .form-group select, .form-group textarea { width: 100%; padding: 10px 14px; border: 1px solid #d1d5db; border-radius: 8px; font-size: 15px; box-sizing: border-box; font-family: inherit; }
    .form-group input:focus, .form-group select:focus, .form-group textarea:focus { outline: none; border-color: #6366f1; }
    .error { color: #dc2626; font-size: 12px; }
    .alert-error { background: #fee2e2; color: #dc2626; padding: 12px; border-radius: 8px; margin-bottom: 16px; }
    .form-actions { display: flex; gap: 12px; justify-content: flex-end; }
    .btn-primary { padding: 10px 24px; background: #6366f1; color: white; border: none; border-radius: 8px; font-weight: 600; cursor: pointer; }
    .btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
    .btn-secondary { padding: 10px 24px; background: #f1f5f9; color: #374151; border: none; border-radius: 8px; font-weight: 600; cursor: pointer; text-decoration: none; }
  `]
})
export class TicketCreateComponent {
  private ticketService = inject(TicketService);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  priorities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  categories = ['DELIVERY', 'PICKUP', 'DOCUMENTATION', 'CUSTOMER', 'SYSTEM', 'OTHER'];
  loading = false;
  error = '';

  form = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(150)]],
    externalRef: [''],
    priority: ['MEDIUM', Validators.required],
    category: ['DELIVERY', Validators.required],
    description: ['', [Validators.required, Validators.maxLength(5000)]]
  });

  onSubmit() {
    if (this.form.invalid) return;
    this.loading = true;
    const v = this.form.value;
    this.ticketService.create({
      title: v.title!, description: v.description!,
      priority: v.priority as any, category: v.category as any,
      externalRef: v.externalRef || undefined
    }).subscribe({
      next: t => this.router.navigate(['/tickets', t.id]),
      error: () => { this.error = 'Failed to create ticket'; this.loading = false; }
    });
  }
}

import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { TicketService } from '../../../core/services/ticket.service';

@Component({
  selector: 'app-create-ticket',
  standalone: true,
  imports: [
    ReactiveFormsModule, RouterLink, MatCardModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule
  ],
  template: `
    <div class="header">
      <button mat-icon-button routerLink="/tickets"><mat-icon>arrow_back</mat-icon></button>
      <h1>New Ticket</h1>
    </div>

    <mat-card class="form-card">
      <mat-card-content>
        <form [formGroup]="form" (ngSubmit)="onSubmit()">
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Title</mat-label>
            <input matInput formControlName="title" placeholder="Brief description of the issue">
            <mat-error>Title is required (max 150 chars)</mat-error>
          </mat-form-field>

          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Description</mat-label>
            <textarea matInput formControlName="description" rows="6"
                      placeholder="Detailed description of the issue"></textarea>
            <mat-error>Description is required (max 5000 chars)</mat-error>
          </mat-form-field>

          <div class="row">
            <mat-form-field appearance="outline">
              <mat-label>Priority</mat-label>
              <mat-select formControlName="priority">
                @for (p of priorities; track p) {
                  <mat-option [value]="p">{{ p }}</mat-option>
                }
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>Category</mat-label>
              <mat-select formControlName="category">
                @for (c of categories; track c) {
                  <mat-option [value]="c">{{ c }}</mat-option>
                }
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>External Ref (optional)</mat-label>
              <input matInput formControlName="externalRef">
            </mat-form-field>
          </div>

          @if (error) {
            <div class="error-message">{{ error }}</div>
          }

          <div class="actions">
            <button mat-button type="button" routerLink="/tickets">Cancel</button>
            <button mat-raised-button color="primary" type="submit" [disabled]="loading || form.invalid">
              @if (loading) { <mat-spinner diameter="20"></mat-spinner> }
              @else { Create Ticket }
            </button>
          </div>
        </form>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .header { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; }
    h1 { margin: 0; }
    .form-card { max-width: 800px; }
    .full-width { width: 100%; margin-bottom: 16px; }
    .row { display: flex; gap: 16px; flex-wrap: wrap; margin-bottom: 16px; }
    .row mat-form-field { flex: 1; min-width: 180px; }
    .actions { display: flex; justify-content: flex-end; gap: 8px; }
    .error-message { color: #f44336; margin-bottom: 16px; }
  `]
})
export class CreateTicketComponent {
  private fb = inject(FormBuilder);
  private ticketService = inject(TicketService);
  private router = inject(Router);

  priorities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
  categories = ['DELIVERY', 'PICKUP', 'DOCUMENTATION', 'CUSTOMER', 'SYSTEM', 'OTHER'];

  form = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['', [Validators.required, Validators.maxLength(5000)]],
    priority: ['MEDIUM', Validators.required],
    category: ['OTHER', Validators.required],
    externalRef: ['']
  });

  loading = false;
  error = '';

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.error = '';
    const value = this.form.value;
    this.ticketService.create({
      title: value.title!,
      description: value.description!,
      priority: value.priority as any,
      category: value.category as any,
      externalRef: value.externalRef || undefined
    }).subscribe({
      next: ticket => this.router.navigate(['/tickets', ticket.id]),
      error: err => {
        this.error = err.error?.detail || 'Failed to create ticket';
        this.loading = false;
      }
    });
  }
}

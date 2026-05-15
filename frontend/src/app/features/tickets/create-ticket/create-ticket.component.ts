import { Component } from '@angular/core';
import { NgIf } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { TicketService } from '../../../core/services/ticket.service';
import { ErrorBannerComponent } from '../../../shared/components/error-banner/error-banner.component';

@Component({
  selector: 'app-create-ticket',
  standalone: true,
  imports: [NgIf, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatButtonModule, ErrorBannerComponent],
  template: `
    <h1>Create Ticket</h1>
    <app-error-banner [message]="error"></app-error-banner>
    <mat-card style="max-width:700px">
      <mat-card-content>
        <form [formGroup]="form" (ngSubmit)="onSubmit()" style="display:flex;flex-direction:column;gap:1rem;padding:1rem">
          <mat-form-field>
            <mat-label>Title *</mat-label>
            <input matInput formControlName="title" maxlength="150">
            <mat-error *ngIf="form.get('title')?.hasError('required')">Title is required</mat-error>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Description *</mat-label>
            <textarea matInput formControlName="description" rows="5" maxlength="5000"></textarea>
            <mat-error *ngIf="form.get('description')?.hasError('required')">Description is required</mat-error>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Priority *</mat-label>
            <mat-select formControlName="priority">
              <mat-option value="LOW">LOW</mat-option>
              <mat-option value="MEDIUM">MEDIUM</mat-option>
              <mat-option value="HIGH">HIGH</mat-option>
              <mat-option value="CRITICAL">CRITICAL</mat-option>
            </mat-select>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Category *</mat-label>
            <mat-select formControlName="category">
              <mat-option value="DELIVERY">DELIVERY</mat-option>
              <mat-option value="PICKUP">PICKUP</mat-option>
              <mat-option value="DOCUMENTATION">DOCUMENTATION</mat-option>
              <mat-option value="CUSTOMER">CUSTOMER</mat-option>
              <mat-option value="SYSTEM">SYSTEM</mat-option>
              <mat-option value="OTHER">OTHER</mat-option>
            </mat-select>
          </mat-form-field>
          <mat-form-field>
            <mat-label>External Reference</mat-label>
            <input matInput formControlName="externalRef">
          </mat-form-field>
          <div style="display:flex;gap:1rem">
            <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || loading">Create</button>
            <button mat-button type="button" (click)="router.navigate(['/tickets'])">Cancel</button>
          </div>
        </form>
      </mat-card-content>
    </mat-card>
  `
})
export class CreateTicketComponent {
  form: FormGroup;
  loading = false;
  error: string | null = null;

  constructor(private fb: FormBuilder, private ticketService: TicketService, public router: Router) {
    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(150)]],
      description: ['', [Validators.required, Validators.maxLength(5000)]],
      priority: ['MEDIUM', Validators.required],
      category: ['OTHER', Validators.required],
      externalRef: ['']
    });
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.ticketService.create(this.form.value).subscribe({
      next: t => this.router.navigate(['/tickets', t.id]),
      error: () => { this.error = 'Failed to create ticket'; this.loading = false; }
    });
  }
}

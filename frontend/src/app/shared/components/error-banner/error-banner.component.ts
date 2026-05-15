import { Component, Input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { NgIf } from '@angular/common';

@Component({
  selector: 'app-error-banner',
  standalone: true,
  imports: [NgIf, MatCardModule],
  template: `
    <mat-card *ngIf="message" style="background:#fce4ec;margin:1rem 0">
      <mat-card-content>
        <strong>Error:</strong> {{ message }}
      </mat-card-content>
    </mat-card>
  `
})
export class ErrorBannerComponent {
  @Input() message: string | null = null;
}

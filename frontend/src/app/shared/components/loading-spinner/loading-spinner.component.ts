import { Component } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [MatProgressSpinnerModule],
  template: `
    <div style="display:flex;justify-content:center;padding:2rem">
      <mat-spinner diameter="48"></mat-spinner>
    </div>
  `
})
export class LoadingSpinnerComponent {}

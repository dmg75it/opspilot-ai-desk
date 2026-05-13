import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-ticket-status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `<span [class]="'badge badge-' + status.toLowerCase().replace('_', '-')">{{ status | titlecase }}</span>`,
  styles: [`
    .badge { padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: 500; white-space: nowrap; }
    .badge-new { background: #e3f2fd; color: #1565c0; }
    .badge-in-progress { background: #fff3e0; color: #e65100; }
    .badge-in_progress { background: #fff3e0; color: #e65100; }
    .badge-waiting-for-customer { background: #f3e5f5; color: #6a1b9a; }
    .badge-waiting_for_customer { background: #f3e5f5; color: #6a1b9a; }
    .badge-resolved { background: #e8f5e9; color: #2e7d32; }
    .badge-closed { background: #f5f5f5; color: #616161; }
  `]
})
export class TicketStatusBadgeComponent {
  @Input() status!: string;
}

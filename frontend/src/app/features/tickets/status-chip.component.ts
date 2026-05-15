import { Component, input } from '@angular/core';
import { MatChipsModule } from '@angular/material/chips';
import { TicketStatus } from '../../core/models/ticket.model';

@Component({
  selector: 'app-status-chip',
  standalone: true,
  imports: [MatChipsModule],
  template: `<mat-chip [class]="'status-' + status().toLowerCase()">{{ status() }}</mat-chip>`,
  styles: [`
    mat-chip { font-size: 11px; font-weight: 500; }
    .status-new { --mdc-chip-label-text-color: #1565c0; --mdc-chip-container-color: #e3f2fd; }
    .status-in_progress { --mdc-chip-label-text-color: #e65100; --mdc-chip-container-color: #fff3e0; }
    .status-waiting_for_customer { --mdc-chip-label-text-color: #6a1b9a; --mdc-chip-container-color: #f3e5f5; }
    .status-resolved { --mdc-chip-label-text-color: #1b5e20; --mdc-chip-container-color: #e8f5e9; }
    .status-closed { --mdc-chip-label-text-color: #424242; --mdc-chip-container-color: #eeeeee; }
  `]
})
export class StatusChipComponent {
  status = input.required<TicketStatus>();
}

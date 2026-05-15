import { Component, OnInit, signal } from '@angular/core';
import { NgIf } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatCardModule } from '@angular/material/card';
import { UserService } from '../../core/services/user.service';
import { User } from '../../core/models/user.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';
import { ErrorBannerComponent } from '../../shared/components/error-banner/error-banner.component';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [NgIf, MatTableModule, MatCardModule, LoadingSpinnerComponent, ErrorBannerComponent],
  template: `
    <h1>Users</h1>
    <app-error-banner [message]="error()"></app-error-banner>
    <app-loading-spinner *ngIf="loading()"></app-loading-spinner>
    <mat-card *ngIf="!loading()">
      <mat-card-content>
        <table mat-table [dataSource]="users()" style="width:100%">
          <ng-container matColumnDef="email">
            <th mat-header-cell *matHeaderCellDef>Email</th>
            <td mat-cell *matCellDef="let u">{{ u.email }}</td>
          </ng-container>
          <ng-container matColumnDef="role">
            <th mat-header-cell *matHeaderCellDef>Role</th>
            <td mat-cell *matCellDef="let u">{{ u.role }}</td>
          </ng-container>
          <ng-container matColumnDef="active">
            <th mat-header-cell *matHeaderCellDef>Active</th>
            <td mat-cell *matCellDef="let u">{{ u.active ? 'Yes' : 'No' }}</td>
          </ng-container>
          <tr mat-header-row *matHeaderRowDef="['email','role','active']"></tr>
          <tr mat-row *matRowDef="let row; columns: ['email','role','active']"></tr>
        </table>
      </mat-card-content>
    </mat-card>
  `
})
export class UserListComponent implements OnInit {
  users = signal<User[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.userService.listUsers().subscribe({
      next: u => { this.users.set(u); this.loading.set(false); },
      error: () => { this.loading.set(false); this.error.set('Failed to load users'); }
    });
  }
}

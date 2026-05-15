import { Component, inject, OnInit, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AdminService } from '../../core/services/admin.service';
import { User } from '../../core/models/user.model';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [MatCardModule, MatTableModule, MatChipsModule, MatProgressSpinnerModule],
  template: `
    <h1>Admin - Users</h1>
    @if (loading()) {
      <div class="center"><mat-spinner></mat-spinner></div>
    } @else {
      <mat-card>
        <mat-card-content>
          <table mat-table [dataSource]="users()">
            <ng-container matColumnDef="email">
              <th mat-header-cell *matHeaderCellDef>Email</th>
              <td mat-cell *matCellDef="let u">{{ u.email }}</td>
            </ng-container>
            <ng-container matColumnDef="fullName">
              <th mat-header-cell *matHeaderCellDef>Full Name</th>
              <td mat-cell *matCellDef="let u">{{ u.fullName }}</td>
            </ng-container>
            <ng-container matColumnDef="role">
              <th mat-header-cell *matHeaderCellDef>Role</th>
              <td mat-cell *matCellDef="let u">
                <mat-chip [color]="u.role === 'ADMIN' ? 'warn' : 'primary'" highlighted>{{ u.role }}</mat-chip>
              </td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="['email','fullName','role']"></tr>
            <tr mat-row *matRowDef="let row; columns: ['email','fullName','role']"></tr>
          </table>
        </mat-card-content>
      </mat-card>
    }
  `,
  styles: [`
    h1 { margin-bottom: 24px; }
    .center { display: flex; justify-content: center; padding: 64px; }
  `]
})
export class AdminComponent implements OnInit {
  private adminService = inject(AdminService);

  users = signal<User[]>([]);
  loading = signal(true);

  ngOnInit(): void {
    this.adminService.listUsers().subscribe({
      next: users => { this.users.set(users); this.loading.set(false); },
      error: () => this.loading.set(false)
    });
  }
}

import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { UserService } from '../../services/user.service';
import { User } from '../../models/user.model';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>Admin - Users</h1>
      </div>
      @if (loading) { <div class="loading">Loading...</div> }
      @if (error) { <div class="alert-error">{{ error }}</div> }
      <div class="table-card">
        <table class="table">
          <thead>
            <tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th>Created</th></tr>
          </thead>
          <tbody>
            @for (u of users; track u.id) {
              <tr>
                <td>{{ u.fullName }}</td>
                <td>{{ u.email }}</td>
                <td><span class="role-badge role-{{ u.role.toLowerCase() }}">{{ u.role }}</span></td>
                <td><span [class]="u.enabled ? 'badge-active' : 'badge-inactive'">{{ u.enabled ? 'Active' : 'Disabled' }}</span></td>
                <td>{{ u.createdAt | date:'mediumDate' }}</td>
              </tr>
            }
          </tbody>
        </table>
      </div>
    </div>
  `,
  styles: [`
    .page-container { padding: 32px; }
    .page-header h1 { font-size: 28px; color: #1a1f36; margin: 0 0 24px; }
    .loading { padding: 20px; color: #6366f1; }
    .alert-error { background: #fee2e2; color: #dc2626; padding: 12px; border-radius: 8px; margin-bottom: 20px; }
    .table-card { background: white; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,0.1); overflow: hidden; }
    .table { width: 100%; border-collapse: collapse; font-size: 14px; }
    .table th { text-align: left; padding: 12px 16px; border-bottom: 2px solid #e2e8f0; color: #64748b; font-weight: 600; font-size: 12px; text-transform: uppercase; background: #f8fafc; }
    .table td { padding: 14px 16px; border-bottom: 1px solid #f1f5f9; }
    .role-badge { display: inline-block; padding: 2px 10px; border-radius: 20px; font-size: 11px; font-weight: 600; text-transform: uppercase; }
    .role-admin { background: #ede9fe; color: #5b21b6; }
    .role-operator { background: #dbeafe; color: #1d4ed8; }
    .badge-active { display: inline-block; padding: 2px 10px; border-radius: 20px; font-size: 11px; background: #dcfce7; color: #15803d; }
    .badge-inactive { display: inline-block; padding: 2px 10px; border-radius: 20px; font-size: 11px; background: #fee2e2; color: #dc2626; }
  `]
})
export class AdminComponent implements OnInit {
  private userService = inject(UserService);
  users: User[] = [];
  loading = false;
  error = '';

  ngOnInit() {
    this.loading = true;
    this.userService.listAll().subscribe({
      next: u => { this.users = u; this.loading = false; },
      error: () => { this.error = 'Failed to load users'; this.loading = false; }
    });
  }
}

import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { UserService } from '../../core/services/user.service';
import { AuthService } from '../../core/services/auth.service';
import { User } from '../../core/models/user.model';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatProgressSpinnerModule,
    MatChipsModule
  ],
  templateUrl: './admin.component.html'
})
export class AdminComponent implements OnInit {
  private userService = inject(UserService);
  authService = inject(AuthService);

  users: User[] = [];
  loading = true;
  error = '';

  displayedColumns = ['id', 'email', 'fullName', 'role'];

  ngOnInit(): void {
    if (!this.authService.isAdmin()) {
      this.error = 'Access denied. Admin role required.';
      this.loading = false;
      return;
    }
    this.userService.list().subscribe({
      next: (users) => { this.users = users; this.loading = false; },
      error: () => { this.error = 'Failed to load users.'; this.loading = false; }
    });
  }
}

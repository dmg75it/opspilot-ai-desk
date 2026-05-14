import { Component, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule],
  template: `
    <div class="app-layout">
      <nav class="sidebar">
        <div class="sidebar-brand">
          <span class="brand-icon">◈</span>
          <span class="brand-name">OpsPilot</span>
        </div>
        <ul class="nav-links">
          <li><a routerLink="/dashboard" routerLinkActive="active">
            <span class="nav-icon">⊞</span> Dashboard
          </a></li>
          <li><a routerLink="/tickets" routerLinkActive="active">
            <span class="nav-icon">☰</span> Tickets
          </a></li>
          <li><a routerLink="/tickets/new" routerLinkActive="active">
            <span class="nav-icon">＋</span> New Ticket
          </a></li>
          @if (auth.isAdmin()) {
            <li><a routerLink="/admin" routerLinkActive="active">
              <span class="nav-icon">⚙</span> Admin
            </a></li>
          }
        </ul>
        <div class="sidebar-footer">
          <div class="user-info">
            <div class="user-name">{{ auth.currentUser()?.fullName }}</div>
            <div class="user-role">{{ auth.currentUser()?.role }}</div>
          </div>
          <button class="btn-logout" (click)="auth.logout()">Logout</button>
        </div>
      </nav>
      <main class="content">
        <router-outlet />
      </main>
    </div>
  `,
  styles: [`
    .app-layout { display: flex; height: 100vh; overflow: hidden; }
    .sidebar { width: 240px; background: #1a1f36; color: #fff; display: flex; flex-direction: column; padding: 0; }
    .sidebar-brand { padding: 24px 20px; font-size: 20px; font-weight: 700; display: flex; align-items: center; gap: 10px; border-bottom: 1px solid rgba(255,255,255,0.1); }
    .brand-icon { color: #6366f1; font-size: 24px; }
    .nav-links { list-style: none; padding: 16px 0; margin: 0; flex: 1; }
    .nav-links li a { display: flex; align-items: center; gap: 10px; padding: 12px 20px; color: rgba(255,255,255,0.7); text-decoration: none; transition: all 0.2s; border-left: 3px solid transparent; }
    .nav-links li a:hover, .nav-links li a.active { color: #fff; background: rgba(99,102,241,0.2); border-left-color: #6366f1; }
    .nav-icon { font-size: 16px; width: 20px; text-align: center; }
    .sidebar-footer { padding: 16px 20px; border-top: 1px solid rgba(255,255,255,0.1); }
    .user-name { font-weight: 600; font-size: 14px; }
    .user-role { font-size: 12px; color: rgba(255,255,255,0.5); margin-bottom: 12px; }
    .btn-logout { width: 100%; padding: 8px; background: rgba(255,255,255,0.1); color: #fff; border: none; border-radius: 6px; cursor: pointer; transition: background 0.2s; }
    .btn-logout:hover { background: rgba(220,38,38,0.6); }
    .content { flex: 1; overflow-y: auto; background: #f1f5f9; }
  `]
})
export class LayoutComponent {
  auth = inject(AuthService);
}

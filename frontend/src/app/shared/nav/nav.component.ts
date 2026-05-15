import { Component, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-nav',
  standalone: true,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatButtonModule, MatIconModule,
    MatSidenavModule, MatListModule
  ],
  template: `
    <mat-sidenav-container class="sidenav-container">
      <mat-sidenav mode="side" opened class="sidenav">
        <mat-toolbar color="primary">
          <span>OpsPilot</span>
        </mat-toolbar>
        <mat-nav-list>
          <a mat-list-item routerLink="/dashboard" routerLinkActive="active-link">
            <mat-icon matListItemIcon>dashboard</mat-icon>
            <span matListItemTitle>Dashboard</span>
          </a>
          <a mat-list-item routerLink="/tickets" routerLinkActive="active-link">
            <mat-icon matListItemIcon>confirmation_number</mat-icon>
            <span matListItemTitle>Tickets</span>
          </a>
          @if (auth.isAdmin()) {
            <a mat-list-item routerLink="/admin" routerLinkActive="active-link">
              <mat-icon matListItemIcon>admin_panel_settings</mat-icon>
              <span matListItemTitle>Admin</span>
            </a>
          }
        </mat-nav-list>
        <div class="sidenav-footer">
          <div class="user-info">{{ auth.currentUser()?.email }}</div>
          <button mat-icon-button (click)="auth.logout()" title="Logout">
            <mat-icon>logout</mat-icon>
          </button>
        </div>
      </mat-sidenav>
      <mat-sidenav-content class="main-content">
        <router-outlet />
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    .sidenav-container { height: 100vh; }
    .sidenav { width: 220px; display: flex; flex-direction: column; }
    .main-content { padding: 24px; }
    .sidenav-footer {
      margin-top: auto;
      padding: 16px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      border-top: 1px solid rgba(0,0,0,0.12);
    }
    .user-info { font-size: 12px; color: rgba(0,0,0,0.6); overflow: hidden; text-overflow: ellipsis; }
    .active-link { background-color: rgba(63, 81, 181, 0.1); }
  `]
})
export class NavComponent {
  auth = inject(AuthService);
}

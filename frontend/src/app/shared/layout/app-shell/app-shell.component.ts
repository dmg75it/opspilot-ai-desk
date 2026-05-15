import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { NgIf } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [NgIf, RouterOutlet, RouterLink, RouterLinkActive,
    MatToolbarModule, MatSidenavModule, MatListModule, MatIconModule, MatButtonModule],
  template: `
    <mat-sidenav-container style="height:100vh">
      <mat-sidenav mode="side" opened style="width:220px">
        <mat-toolbar color="primary">OpsPilot</mat-toolbar>
        <mat-nav-list>
          <a mat-list-item routerLink="/dashboard" routerLinkActive="active-link">
            <mat-icon matListItemIcon>dashboard</mat-icon>
            <span matListItemTitle>Dashboard</span>
          </a>
          <a mat-list-item routerLink="/tickets" routerLinkActive="active-link">
            <mat-icon matListItemIcon>confirmation_number</mat-icon>
            <span matListItemTitle>Tickets</span>
          </a>
          <a mat-list-item routerLink="/tickets/new" routerLinkActive="active-link">
            <mat-icon matListItemIcon>add_circle</mat-icon>
            <span matListItemTitle>New Ticket</span>
          </a>
          <a mat-list-item routerLink="/admin" routerLinkActive="active-link" *ngIf="auth.isAdmin()">
            <mat-icon matListItemIcon>admin_panel_settings</mat-icon>
            <span matListItemTitle>Admin</span>
          </a>
        </mat-nav-list>
        <div style="position:absolute;bottom:1rem;left:1rem">
          <small>{{ auth.currentUser()?.email }}</small><br>
          <button mat-button (click)="auth.logout()">Logout</button>
        </div>
      </mat-sidenav>
      <mat-sidenav-content style="padding:1.5rem">
        <router-outlet></router-outlet>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`.active-link { background: rgba(0,0,0,0.08); }`]
})
export class AppShellComponent {
  constructor(public auth: AuthService) {}
}

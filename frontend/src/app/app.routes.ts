import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { adminGuard } from './guards/admin.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent) },
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () => import('./layout/layout.component').then(m => m.LayoutComponent),
    children: [
      { path: 'dashboard', loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'tickets', loadComponent: () => import('./pages/ticket-list/ticket-list.component').then(m => m.TicketListComponent) },
      { path: 'tickets/new', loadComponent: () => import('./pages/ticket-create/ticket-create.component').then(m => m.TicketCreateComponent) },
      { path: 'tickets/:id', loadComponent: () => import('./pages/ticket-detail/ticket-detail.component').then(m => m.TicketDetailComponent) },
      { path: 'admin', canActivate: [adminGuard], loadComponent: () => import('./pages/admin/admin.component').then(m => m.AdminComponent) }
    ]
  },
  { path: '**', redirectTo: '/dashboard' }
];

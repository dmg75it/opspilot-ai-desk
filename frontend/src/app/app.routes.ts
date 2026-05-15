import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { adminGuard } from './core/auth/admin.guard';
import { AppShellComponent } from './shared/layout/app-shell/app-shell.component';
import { LoginComponent } from './features/login/login.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { TicketListComponent } from './features/tickets/ticket-list/ticket-list.component';
import { TicketDetailComponent } from './features/tickets/ticket-detail/ticket-detail.component';
import { CreateTicketComponent } from './features/tickets/create-ticket/create-ticket.component';
import { UserListComponent } from './features/admin/user-list.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', component: DashboardComponent },
      { path: 'tickets', component: TicketListComponent },
      { path: 'tickets/new', component: CreateTicketComponent },
      { path: 'tickets/:id', component: TicketDetailComponent },
      { path: 'admin', component: UserListComponent, canActivate: [adminGuard] },
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];

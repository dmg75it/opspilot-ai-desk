# OpsPilot AI Desk — Frontend + Docker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the Angular 19 frontend SPA with all pages (login, dashboard, tickets, AI chat, admin) and Docker multi-stage builds for full-stack deployment.

**Architecture:** Angular 19 standalone components. Angular Material for UI. JWT stored in localStorage, injected via HttpInterceptor. Proxy in dev mode to avoid CORS. Backend must be running on localhost:8080.

**Tech Stack:** Angular 19, Angular Material 19, TypeScript 5.x, Node 22, Karma for tests. `ng serve` proxies to backend via `proxy.conf.json`.

**Prerequisites:** Backend plan complete and backend running (`make backend-run`). PostgreSQL running (`make db-up`).

---

## File Map

```
frontend/
  package.json
  angular.json
  tsconfig.json
  proxy.conf.json
  src/
    environments/environment.ts
    environments/environment.prod.ts
    app/
      app.config.ts
      app.routes.ts
      core/
        models/user.model.ts
        models/ticket.model.ts
        models/chat.model.ts
        models/dashboard.model.ts
        auth/auth.service.ts
        auth/auth.guard.ts
        auth/admin.guard.ts
        auth/jwt.interceptor.ts
        services/ticket.service.ts
        services/note.service.ts
        services/ai.service.ts
        services/dashboard.service.ts
        services/user.service.ts
      shared/
        components/loading-spinner/loading-spinner.component.ts
        components/error-banner/error-banner.component.ts
        layout/app-shell/app-shell.component.ts
      features/
        login/login.component.ts
        dashboard/dashboard.component.ts
        tickets/ticket-list/ticket-list.component.ts
        tickets/ticket-detail/ticket-detail.component.ts
        tickets/create-ticket/create-ticket.component.ts
        ai-chat/ai-chat-panel.component.ts
        admin/user-list.component.ts
      core/auth/auth.service.spec.ts
      features/tickets/ticket-list/ticket-list.component.spec.ts
backend/Dockerfile
frontend/Dockerfile
frontend/nginx.conf
```

---

### Task 15: Angular project setup

- [ ] **Step 1: Create Angular project**

```bash
cd /home/gianluca/applicazioni/VibeCoding/git/opspilot-ai-desk
ng new frontend --routing=false --style=scss --ssr=false --skip-git=true
```

When prompted: choose defaults. This creates Angular 19 with standalone components.

- [ ] **Step 2: Add Angular Material**

```bash
cd frontend && ng add @angular/material --skip-confirmation
```

When prompted: choose "Azure/Blue" theme (or any), enable typography, enable animations.

- [ ] **Step 3: Create `frontend/proxy.conf.json`**

```json
{
  "/api": {
    "target": "http://localhost:8080",
    "secure": false,
    "changeOrigin": true
  }
}
```

- [ ] **Step 4: Update `frontend/package.json` start script to use proxy**

In `package.json`, change the `"start"` script:
```json
"start": "ng serve --proxy-config proxy.conf.json"
```

- [ ] **Step 5: Create `frontend/src/environments/environment.ts`**

```typescript
export const environment = {
  production: false,
  apiBaseUrl: '/api'
};
```

- [ ] **Step 6: Create `frontend/src/environments/environment.prod.ts`**

```typescript
export const environment = {
  production: true,
  apiBaseUrl: '/api'
};
```

- [ ] **Step 7: Verify project starts**

```bash
cd frontend && npm start &
sleep 15 && curl -s http://localhost:4200 | head -5
kill %1
```

Expected: HTML response with Angular app

- [ ] **Step 8: Commit**

```bash
cd .. && git add frontend/ && git commit -m "feat: scaffold Angular 19 project with Material and proxy"
```

---

### Task 16: Core models + auth service + interceptor + guards

**Files:** `src/app/core/`

- [ ] **Step 1: Create `core/models/user.model.ts`**

```typescript
export interface User {
  id: string;
  email: string;
  role: 'ADMIN' | 'OPERATOR';
  active: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  email: string;
  role: string;
}
```

- [ ] **Step 2: Create `core/models/ticket.model.ts`**

```typescript
export type TicketStatus = 'NEW' | 'IN_PROGRESS' | 'WAITING_FOR_CUSTOMER' | 'RESOLVED' | 'CLOSED';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type TicketCategory = 'DELIVERY' | 'PICKUP' | 'DOCUMENTATION' | 'CUSTOMER' | 'SYSTEM' | 'OTHER';

export interface Ticket {
  id: string;
  externalRef?: string;
  title: string;
  description: string;
  status: TicketStatus;
  priority: TicketPriority;
  category: TicketCategory;
  assignedToEmail?: string;
  createdByEmail: string;
  createdAt: string;
  updatedAt: string;
  resolvedAt?: string;
  version: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface CreateTicketRequest {
  title: string;
  description: string;
  priority: string;
  category: string;
  externalRef?: string;
}

export interface ChangeStatusRequest {
  status: string;
}

export interface Note {
  id: string;
  ticketId: string;
  authorEmail?: string;
  body: string;
  visibility: 'INTERNAL' | 'AI_SUMMARY' | 'SYSTEM';
  createdAt: string;
}
```

- [ ] **Step 3: Create `core/models/chat.model.ts`**

```typescript
export interface ChatMessage {
  id: string;
  role: 'SYSTEM' | 'USER' | 'ASSISTANT';
  content: string;
  model?: string;
  promptTokens?: number;
  completionTokens?: number;
  createdAt: string;
  error: boolean;
  errorMessage?: string;
}

export interface ChatSession {
  id: string;
  ticketId: string;
  createdAt: string;
  messages: ChatMessage[];
}

export interface AiActionResponse {
  content: string;
  success: boolean;
  error?: string;
}
```

- [ ] **Step 4: Create `core/models/dashboard.model.ts`**

```typescript
import { Ticket } from './ticket.model';

export interface DashboardData {
  ticketsByStatus: Record<string, number>;
  ticketsByPriority: Record<string, number>;
  myOpenTickets: Ticket[];
  recentlyUpdated: Ticket[];
  aiInteractionsToday: number;
}
```

- [ ] **Step 5: Create `core/auth/auth.service.ts`**

```typescript
import { Injectable, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { tap } from 'rxjs/operators';
import { Observable } from 'rxjs';
import { LoginRequest, LoginResponse, User } from '../models/user.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly TOKEN_KEY = 'auth_token';
  currentUser = signal<User | null>(null);

  constructor(private http: HttpClient, private router: Router) {
    const token = this.getToken();
    if (token) this.loadCurrentUser();
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${environment.apiBaseUrl}/auth/login`, request).pipe(
      tap(response => {
        localStorage.setItem(this.TOKEN_KEY, response.token);
        this.loadCurrentUser();
      })
    );
  }

  logout(): void {
    localStorage.removeItem(this.TOKEN_KEY);
    this.currentUser.set(null);
    this.router.navigate(['/login']);
  }

  getToken(): string | null {
    return localStorage.getItem(this.TOKEN_KEY);
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  isAdmin(): boolean {
    return this.currentUser()?.role === 'ADMIN';
  }

  private loadCurrentUser(): void {
    this.http.get<User>(`${environment.apiBaseUrl}/auth/me`).subscribe({
      next: user => this.currentUser.set(user),
      error: () => this.logout()
    });
  }
}
```

- [ ] **Step 6: Create `core/auth/jwt.interceptor.ts`**

```typescript
import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthService).getToken();
  if (token) {
    req = req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
  }
  return next(req);
};
```

- [ ] **Step 7: Create `core/auth/auth.guard.ts`**

```typescript
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (auth.isLoggedIn()) return true;
  inject(Router).navigate(['/login']);
  return false;
};
```

- [ ] **Step 8: Create `core/auth/admin.guard.ts`**

```typescript
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const adminGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  if (auth.isAdmin()) return true;
  inject(Router).navigate(['/dashboard']);
  return false;
};
```

- [ ] **Step 9: Update `app/app.config.ts`**

```typescript
import { ApplicationConfig } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { routes } from './app.routes';
import { jwtInterceptor } from './core/auth/jwt.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([jwtInterceptor])),
    provideAnimationsAsync(),
  ]
};
```

- [ ] **Step 10: Compile check**

```bash
cd frontend && npx ng build --configuration=development 2>&1 | tail -10
```

Expected: Compilation successful (may have warnings about unused routes — ignore)

- [ ] **Step 11: Commit**

```bash
cd .. && git add frontend/src/ && git commit -m "feat: add Angular core models, AuthService, guards, interceptor"
```

---

### Task 17: Core API services

**Files:** `src/app/core/services/`

- [ ] **Step 1: Create `core/services/ticket.service.ts`**

```typescript
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Ticket, Page, CreateTicketRequest, ChangeStatusRequest } from '../models/ticket.model';

@Injectable({ providedIn: 'root' })
export class TicketService {
  private base = `${environment.apiBaseUrl}/tickets`;

  constructor(private http: HttpClient) {}

  list(page = 0, size = 20, sort = 'createdAt', dir = 'desc'): Observable<Page<Ticket>> {
    const params = new HttpParams()
      .set('page', page).set('size', size).set('sort', sort).set('dir', dir);
    return this.http.get<Page<Ticket>>(this.base, { params });
  }

  getById(id: string): Observable<Ticket> {
    return this.http.get<Ticket>(`${this.base}/${id}`);
  }

  create(req: CreateTicketRequest): Observable<Ticket> {
    return this.http.post<Ticket>(this.base, req);
  }

  update(id: string, req: Partial<CreateTicketRequest>): Observable<Ticket> {
    return this.http.patch<Ticket>(`${this.base}/${id}`, req);
  }

  changeStatus(id: string, req: ChangeStatusRequest): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.base}/${id}/status`, req);
  }

  assign(id: string, assigneeId: string | null): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.base}/${id}/assign`, { assigneeId });
  }
}
```

- [ ] **Step 2: Create `core/services/note.service.ts`**

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Note } from '../models/ticket.model';

@Injectable({ providedIn: 'root' })
export class NoteService {
  constructor(private http: HttpClient) {}

  listNotes(ticketId: string): Observable<Note[]> {
    return this.http.get<Note[]>(`${environment.apiBaseUrl}/tickets/${ticketId}/notes`);
  }

  addNote(ticketId: string, body: string): Observable<Note> {
    return this.http.post<Note>(`${environment.apiBaseUrl}/tickets/${ticketId}/notes`, { body });
  }
}
```

- [ ] **Step 3: Create `core/services/ai.service.ts`**

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ChatSession, ChatMessage, AiActionResponse } from '../models/chat.model';
import { Note } from '../models/ticket.model';

@Injectable({ providedIn: 'root' })
export class AiService {
  constructor(private http: HttpClient) {}

  private base(ticketId: string) { return `${environment.apiBaseUrl}/tickets/${ticketId}/ai`; }

  getSession(ticketId: string): Observable<ChatSession> {
    return this.http.get<ChatSession>(`${this.base(ticketId)}/session`);
  }

  sendMessage(ticketId: string, content: string): Observable<ChatMessage> {
    return this.http.post<ChatMessage>(`${this.base(ticketId)}/messages`, { content });
  }

  generateSummary(ticketId: string): Observable<AiActionResponse> {
    return this.http.post<AiActionResponse>(`${this.base(ticketId)}/summary`, {});
  }

  generateSuggestedReply(ticketId: string): Observable<AiActionResponse> {
    return this.http.post<AiActionResponse>(`${this.base(ticketId)}/suggested-reply`, {});
  }

  applySummaryAsNote(ticketId: string, content: string): Observable<Note> {
    return this.http.post<Note>(`${this.base(ticketId)}/apply-summary`, { content });
  }
}
```

- [ ] **Step 4: Create `core/services/dashboard.service.ts`**

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { DashboardData } from '../models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  constructor(private http: HttpClient) {}

  getDashboard(): Observable<DashboardData> {
    return this.http.get<DashboardData>(`${environment.apiBaseUrl}/dashboard`);
  }
}
```

- [ ] **Step 5: Create `core/services/user.service.ts`**

```typescript
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  constructor(private http: HttpClient) {}

  listUsers(): Observable<User[]> {
    return this.http.get<User[]>(`${environment.apiBaseUrl}/users`);
  }
}
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/ && git commit -m "feat: add Angular core services (ticket, note, ai, dashboard, user)"
```

---

### Task 18: Shared layout components

**Files:** `src/app/shared/`

- [ ] **Step 1: Create `shared/components/loading-spinner/loading-spinner.component.ts`**

```typescript
import { Component } from '@angular/core';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

@Component({
  selector: 'app-loading-spinner',
  standalone: true,
  imports: [MatProgressSpinnerModule],
  template: `
    <div style="display:flex;justify-content:center;padding:2rem">
      <mat-spinner diameter="48"></mat-spinner>
    </div>
  `
})
export class LoadingSpinnerComponent {}
```

- [ ] **Step 2: Create `shared/components/error-banner/error-banner.component.ts`**

```typescript
import { Component, Input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-error-banner',
  standalone: true,
  imports: [CommonModule, MatCardModule],
  template: `
    <mat-card *ngIf="message" style="background:#fce4ec;margin:1rem 0">
      <mat-card-content>
        <strong>Error:</strong> {{ message }}
      </mat-card-content>
    </mat-card>
  `
})
export class ErrorBannerComponent {
  @Input() message: string | null = null;
}
```

- [ ] **Step 3: Create `shared/layout/app-shell/app-shell.component.ts`**

```typescript
import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive,
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
```

- [ ] **Step 4: Update `app/app.routes.ts`**

```typescript
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
```

- [ ] **Step 5: Update `app/app.component.ts`** to just render the router outlet:

```typescript
import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `<router-outlet></router-outlet>`
})
export class AppComponent {}
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/ && git commit -m "feat: add shared layout components, app shell, routing"
```

---

### Task 19: Login page

**File:** `features/login/login.component.ts`

- [ ] **Step 1: Create `features/login/login.component.ts`**

```typescript
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatProgressSpinnerModule],
  template: `
    <div style="display:flex;justify-content:center;align-items:center;height:100vh;background:#f5f5f5">
      <mat-card style="width:380px;padding:1rem">
        <mat-card-header>
          <mat-card-title>OpsPilot AI Desk</mat-card-title>
          <mat-card-subtitle>Sign in to continue</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()" style="display:flex;flex-direction:column;gap:1rem;margin-top:1rem">
            <mat-form-field>
              <mat-label>Email</mat-label>
              <input matInput type="email" formControlName="email" autocomplete="email">
              <mat-error *ngIf="form.get('email')?.hasError('required')">Email is required</mat-error>
            </mat-form-field>
            <mat-form-field>
              <mat-label>Password</mat-label>
              <input matInput type="password" formControlName="password" autocomplete="current-password">
              <mat-error *ngIf="form.get('password')?.hasError('required')">Password is required</mat-error>
            </mat-form-field>
            <div *ngIf="error" style="color:red;font-size:0.875rem">{{ error }}</div>
            <button mat-raised-button color="primary" type="submit" [disabled]="loading || form.invalid">
              <mat-spinner *ngIf="loading" diameter="20" style="display:inline-block"></mat-spinner>
              <span *ngIf="!loading">Sign In</span>
            </button>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `
})
export class LoginComponent {
  form: FormGroup;
  loading = false;
  error: string | null = null;

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {
    this.form = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', Validators.required]
    });
    if (auth.isLoggedIn()) router.navigate(['/dashboard']);
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.error = null;
    this.auth.login(this.form.value).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: () => {
        this.error = 'Invalid email or password';
        this.loading = false;
      }
    });
  }
}
```

- [ ] **Step 2: Compile check**

```bash
cd frontend && npx ng build --configuration=development 2>&1 | tail -5
```

Expected: Compilation successful

- [ ] **Step 3: Commit**

```bash
cd .. && git add frontend/src/ && git commit -m "feat: add login page"
```

---

### Task 20: Dashboard page

**File:** `features/dashboard/dashboard.component.ts`

- [ ] **Step 1: Create `features/dashboard/dashboard.component.ts`**

```typescript
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { DashboardService } from '../../core/services/dashboard.service';
import { DashboardData } from '../../core/models/dashboard.model';
import { Ticket } from '../../core/models/ticket.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';
import { ErrorBannerComponent } from '../../shared/components/error-banner/error-banner.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, MatCardModule, MatTableModule,
    MatChipsModule, MatButtonModule, LoadingSpinnerComponent, ErrorBannerComponent],
  template: `
    <h1>Dashboard</h1>
    <app-loading-spinner *ngIf="loading"></app-loading-spinner>
    <app-error-banner [message]="error"></app-error-banner>

    <div *ngIf="data" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:1rem;margin-bottom:1.5rem">
      <mat-card *ngFor="let entry of statusEntries()">
        <mat-card-content>
          <div style="font-size:2rem;font-weight:bold">{{ entry[1] }}</div>
          <div>{{ entry[0] }}</div>
        </mat-card-content>
      </mat-card>
      <mat-card>
        <mat-card-content>
          <div style="font-size:2rem;font-weight:bold">{{ data.aiInteractionsToday }}</div>
          <div>AI interactions today</div>
        </mat-card-content>
      </mat-card>
    </div>

    <div *ngIf="data" style="display:grid;grid-template-columns:1fr 1fr;gap:1.5rem">
      <mat-card>
        <mat-card-header><mat-card-title>My Open Tickets</mat-card-title></mat-card-header>
        <mat-card-content>
          <div *ngIf="data.myOpenTickets.length === 0" style="color:grey">No assigned tickets</div>
          <table mat-table [dataSource]="data.myOpenTickets" *ngIf="data.myOpenTickets.length > 0" style="width:100%">
            <ng-container matColumnDef="title">
              <th mat-header-cell *matHeaderCellDef>Title</th>
              <td mat-cell *matCellDef="let t">
                <a [routerLink]="['/tickets', t.id]">{{ t.title }}</a>
              </td>
            </ng-container>
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Status</th>
              <td mat-cell *matCellDef="let t">{{ t.status }}</td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="['title','status']"></tr>
            <tr mat-row *matRowDef="let row; columns: ['title','status']"></tr>
          </table>
        </mat-card-content>
      </mat-card>

      <mat-card>
        <mat-card-header><mat-card-title>Recently Updated</mat-card-title></mat-card-header>
        <mat-card-content>
          <table mat-table [dataSource]="data.recentlyUpdated" style="width:100%">
            <ng-container matColumnDef="title">
              <th mat-header-cell *matHeaderCellDef>Title</th>
              <td mat-cell *matCellDef="let t">
                <a [routerLink]="['/tickets', t.id]">{{ t.title }}</a>
              </td>
            </ng-container>
            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Status</th>
              <td mat-cell *matCellDef="let t">{{ t.status }}</td>
            </ng-container>
            <tr mat-header-row *matHeaderRowDef="['title','status']"></tr>
            <tr mat-row *matRowDef="let row; columns: ['title','status']"></tr>
          </table>
        </mat-card-content>
      </mat-card>
    </div>
  `
})
export class DashboardComponent implements OnInit {
  loading = true;
  error: string | null = null;
  data: DashboardData | null = null;

  constructor(private dashboardService: DashboardService) {}

  ngOnInit(): void {
    this.dashboardService.getDashboard().subscribe({
      next: d => { this.data = d; this.loading = false; },
      error: () => { this.error = 'Failed to load dashboard'; this.loading = false; }
    });
  }

  statusEntries(): [string, number][] {
    return Object.entries(this.data?.ticketsByStatus ?? {});
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/ && git commit -m "feat: add dashboard page"
```

---

### Task 21: Ticket list page

**File:** `features/tickets/ticket-list/ticket-list.component.ts`

- [ ] **Step 1: Create the component**

```typescript
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { TicketService } from '../../../core/services/ticket.service';
import { Ticket, Page } from '../../../core/models/ticket.model';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { ErrorBannerComponent } from '../../../shared/components/error-banner/error-banner.component';

@Component({
  selector: 'app-ticket-list',
  standalone: true,
  imports: [CommonModule, RouterLink, MatTableModule, MatPaginatorModule,
    MatButtonModule, MatChipsModule, MatIconModule, LoadingSpinnerComponent, ErrorBannerComponent],
  template: `
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:1rem">
      <h1>Tickets</h1>
      <a mat-raised-button color="primary" routerLink="/tickets/new">New Ticket</a>
    </div>
    <app-loading-spinner *ngIf="loading"></app-loading-spinner>
    <app-error-banner [message]="error"></app-error-banner>

    <table mat-table [dataSource]="tickets" *ngIf="!loading" style="width:100%">
      <ng-container matColumnDef="title">
        <th mat-header-cell *matHeaderCellDef>Title</th>
        <td mat-cell *matCellDef="let t">
          <a [routerLink]="['/tickets', t.id]" style="font-weight:500">{{ t.title }}</a>
        </td>
      </ng-container>
      <ng-container matColumnDef="status">
        <th mat-header-cell *matHeaderCellDef>Status</th>
        <td mat-cell *matCellDef="let t">
          <span [class]="'status-' + t.status.toLowerCase()">{{ t.status }}</span>
        </td>
      </ng-container>
      <ng-container matColumnDef="priority">
        <th mat-header-cell *matHeaderCellDef>Priority</th>
        <td mat-cell *matCellDef="let t">{{ t.priority }}</td>
      </ng-container>
      <ng-container matColumnDef="category">
        <th mat-header-cell *matHeaderCellDef>Category</th>
        <td mat-cell *matCellDef="let t">{{ t.category }}</td>
      </ng-container>
      <ng-container matColumnDef="createdByEmail">
        <th mat-header-cell *matHeaderCellDef>Created by</th>
        <td mat-cell *matCellDef="let t">{{ t.createdByEmail }}</td>
      </ng-container>
      <ng-container matColumnDef="updatedAt">
        <th mat-header-cell *matHeaderCellDef>Updated</th>
        <td mat-cell *matCellDef="let t">{{ t.updatedAt | date:'short' }}</td>
      </ng-container>
      <tr mat-header-row *matHeaderRowDef="columns"></tr>
      <tr mat-row *matRowDef="let row; columns: columns"></tr>
    </table>

    <mat-paginator
      [length]="totalElements"
      [pageSize]="pageSize"
      [pageSizeOptions]="[10, 20, 50]"
      (page)="onPage($event)">
    </mat-paginator>
  `,
  styles: [`
    .status-new { color: #1565C0; }
    .status-in_progress { color: #E65100; }
    .status-resolved { color: #2E7D32; }
    .status-closed { color: #616161; }
    .status-waiting_for_customer { color: #6A1B9A; }
  `]
})
export class TicketListComponent implements OnInit {
  tickets: Ticket[] = [];
  loading = true;
  error: string | null = null;
  totalElements = 0;
  pageSize = 20;
  currentPage = 0;
  columns = ['title', 'status', 'priority', 'category', 'createdByEmail', 'updatedAt'];

  constructor(private ticketService: TicketService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.ticketService.list(this.currentPage, this.pageSize).subscribe({
      next: (page: Page<Ticket>) => {
        this.tickets = page.content;
        this.totalElements = page.totalElements;
        this.loading = false;
      },
      error: () => { this.error = 'Failed to load tickets'; this.loading = false; }
    });
  }

  onPage(event: PageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.load();
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add frontend/src/ && git commit -m "feat: add ticket list page"
```

---

### Task 22: Ticket detail page + AI chat panel

**Files:**
- Create: `features/tickets/ticket-detail/ticket-detail.component.ts`
- Create: `features/ai-chat/ai-chat-panel.component.ts`

- [ ] **Step 1: Create `features/ai-chat/ai-chat-panel.component.ts`**

```typescript
import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { AiService } from '../../core/services/ai.service';
import { ChatSession, ChatMessage, AiActionResponse } from '../../core/models/chat.model';
import { Note } from '../../core/models/ticket.model';

@Component({
  selector: 'app-ai-chat-panel',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatProgressSpinnerModule, MatDividerModule],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>AI Assistant</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <div style="display:flex;gap:0.5rem;margin-bottom:1rem;flex-wrap:wrap">
          <button mat-stroked-button (click)="getSummary()" [disabled]="aiLoading">Summary</button>
          <button mat-stroked-button (click)="getSuggestedReply()" [disabled]="aiLoading">Suggested Reply</button>
        </div>

        <div *ngIf="actionResult" style="background:#e8f5e9;padding:1rem;border-radius:4px;margin-bottom:1rem;white-space:pre-wrap">
          {{ actionResult }}
          <div style="margin-top:0.5rem">
            <button mat-button color="primary" (click)="applyAsNote()">Apply as Note</button>
          </div>
        </div>

        <div *ngIf="noteApplied" style="color:green;margin-bottom:1rem">Note added to ticket.</div>
        <div *ngIf="aiError" style="color:red;margin-bottom:1rem">{{ aiError }}</div>

        <mat-divider style="margin-bottom:1rem"></mat-divider>

        <div style="max-height:300px;overflow-y:auto;margin-bottom:1rem">
          <div *ngFor="let msg of messages"
               [style.text-align]="msg.role === 'USER' ? 'right' : 'left'"
               style="margin-bottom:0.75rem">
            <span [style.background]="msg.role === 'USER' ? '#E3F2FD' : '#F3E5F5'"
                  style="padding:0.5rem 1rem;border-radius:12px;display:inline-block;max-width:80%;white-space:pre-wrap">
              <strong>{{ msg.role }}</strong><br>
              {{ msg.error ? 'Error: ' + msg.errorMessage : msg.content }}
            </span>
          </div>
        </div>

        <div style="display:flex;gap:0.5rem">
          <mat-form-field style="flex:1">
            <mat-label>Message</mat-label>
            <input matInput [formControl]="messageControl" (keyup.enter)="sendMessage()">
          </mat-form-field>
          <button mat-raised-button color="primary" (click)="sendMessage()" [disabled]="aiLoading || !messageControl.value">
            <mat-spinner *ngIf="aiLoading" diameter="20"></mat-spinner>
            <span *ngIf="!aiLoading">Send</span>
          </button>
        </div>
      </mat-card-content>
    </mat-card>
  `
})
export class AiChatPanelComponent implements OnInit {
  @Input() ticketId!: string;
  messages: ChatMessage[] = [];
  messageControl = new FormControl('');
  aiLoading = false;
  aiError: string | null = null;
  actionResult: string | null = null;
  noteApplied = false;

  constructor(private aiService: AiService) {}

  ngOnInit(): void {
    this.aiService.getSession(this.ticketId).subscribe({
      next: session => this.messages = session.messages,
      error: () => {}
    });
  }

  sendMessage(): void {
    const content = this.messageControl.value?.trim();
    if (!content) return;
    this.aiLoading = true;
    this.aiError = null;
    this.messageControl.setValue('');
    this.messages.push({ id: '', role: 'USER', content, createdAt: new Date().toISOString(), error: false });
    this.aiService.sendMessage(this.ticketId, content).subscribe({
      next: msg => { this.messages.push(msg); this.aiLoading = false; },
      error: () => { this.aiError = 'Failed to send message'; this.aiLoading = false; }
    });
  }

  getSummary(): void {
    this.aiLoading = true;
    this.actionResult = null;
    this.noteApplied = false;
    this.aiService.generateSummary(this.ticketId).subscribe({
      next: r => { this.actionResult = r.content; this.aiLoading = false; },
      error: () => { this.aiError = 'Failed to generate summary'; this.aiLoading = false; }
    });
  }

  getSuggestedReply(): void {
    this.aiLoading = true;
    this.actionResult = null;
    this.noteApplied = false;
    this.aiService.generateSuggestedReply(this.ticketId).subscribe({
      next: r => { this.actionResult = r.content; this.aiLoading = false; },
      error: () => { this.aiError = 'Failed to generate reply'; this.aiLoading = false; }
    });
  }

  applyAsNote(): void {
    if (!this.actionResult) return;
    this.aiService.applySummaryAsNote(this.ticketId, this.actionResult).subscribe({
      next: () => { this.noteApplied = true; this.actionResult = null; },
      error: () => { this.aiError = 'Failed to apply note'; }
    });
  }
}
```

- [ ] **Step 2: Create `features/tickets/ticket-detail/ticket-detail.component.ts`**

```typescript
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormControl } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDividerModule } from '@angular/material/divider';
import { MatTabsModule } from '@angular/material/tabs';
import { TicketService } from '../../../core/services/ticket.service';
import { NoteService } from '../../../core/services/note.service';
import { Ticket, Note } from '../../../core/models/ticket.model';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { ErrorBannerComponent } from '../../../shared/components/error-banner/error-banner.component';
import { AiChatPanelComponent } from '../../ai-chat/ai-chat-panel.component';

@Component({
  selector: 'app-ticket-detail',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatCardModule, MatButtonModule,
    MatSelectModule, MatFormFieldModule, MatInputModule, MatDividerModule,
    MatTabsModule, LoadingSpinnerComponent, ErrorBannerComponent, AiChatPanelComponent],
  template: `
    <app-loading-spinner *ngIf="loading"></app-loading-spinner>
    <app-error-banner [message]="error"></app-error-banner>

    <div *ngIf="ticket">
      <div style="display:flex;justify-content:space-between;align-items:flex-start">
        <div>
          <h1>{{ ticket.title }}</h1>
          <p style="color:grey">{{ ticket.externalRef ? '#' + ticket.externalRef + ' · ' : '' }}{{ ticket.status }} · {{ ticket.priority }} · {{ ticket.category }}</p>
        </div>
        <div style="display:flex;gap:0.5rem;align-items:center">
          <mat-form-field style="width:200px">
            <mat-label>Change Status</mat-label>
            <mat-select [formControl]="statusControl">
              <mat-option value="NEW">NEW</mat-option>
              <mat-option value="IN_PROGRESS">IN_PROGRESS</mat-option>
              <mat-option value="WAITING_FOR_CUSTOMER">WAITING_FOR_CUSTOMER</mat-option>
              <mat-option value="RESOLVED">RESOLVED</mat-option>
              <mat-option value="CLOSED">CLOSED</mat-option>
            </mat-select>
          </mat-form-field>
          <button mat-raised-button color="accent" (click)="changeStatus()" [disabled]="statusControl.value === ticket.status">Apply</button>
        </div>
      </div>

      <mat-card style="margin-bottom:1rem">
        <mat-card-content>
          <p><strong>Description:</strong></p>
          <p style="white-space:pre-wrap">{{ ticket.description }}</p>
          <mat-divider style="margin:1rem 0"></mat-divider>
          <p><strong>Created by:</strong> {{ ticket.createdByEmail }} · <strong>Assigned:</strong> {{ ticket.assignedToEmail || 'Unassigned' }}</p>
          <p><strong>Created:</strong> {{ ticket.createdAt | date:'medium' }} · <strong>Updated:</strong> {{ ticket.updatedAt | date:'medium' }}</p>
        </mat-card-content>
      </mat-card>

      <mat-tab-group>
        <mat-tab label="Notes">
          <div style="padding:1rem">
            <div *ngFor="let note of notes" style="margin-bottom:0.75rem;padding:0.75rem;background:#f5f5f5;border-radius:4px">
              <small><strong>{{ note.visibility }}</strong> · {{ note.authorEmail || 'system' }} · {{ note.createdAt | date:'short' }}</small>
              <p style="margin:0.5rem 0 0;white-space:pre-wrap">{{ note.body }}</p>
            </div>
            <div style="display:flex;gap:0.5rem;margin-top:1rem">
              <mat-form-field style="flex:1">
                <mat-label>Add note</mat-label>
                <textarea matInput [formControl]="noteControl" rows="3"></textarea>
              </mat-form-field>
              <button mat-raised-button (click)="addNote()" [disabled]="!noteControl.value">Add</button>
            </div>
          </div>
        </mat-tab>
        <mat-tab label="AI Assistant">
          <div style="padding:1rem">
            <app-ai-chat-panel [ticketId]="ticket.id"></app-ai-chat-panel>
          </div>
        </mat-tab>
      </mat-tab-group>
    </div>
  `
})
export class TicketDetailComponent implements OnInit {
  ticket: Ticket | null = null;
  notes: Note[] = [];
  loading = true;
  error: string | null = null;
  statusControl = new FormControl('');
  noteControl = new FormControl('');

  constructor(
    private route: ActivatedRoute,
    private ticketService: TicketService,
    private noteService: NoteService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.ticketService.getById(id).subscribe({
      next: t => {
        this.ticket = t;
        this.statusControl.setValue(t.status);
        this.loading = false;
        this.loadNotes(id);
      },
      error: () => { this.error = 'Ticket not found'; this.loading = false; }
    });
  }

  loadNotes(id: string): void {
    this.noteService.listNotes(id).subscribe({
      next: notes => this.notes = notes,
      error: () => {}
    });
  }

  changeStatus(): void {
    const newStatus = this.statusControl.value;
    if (!newStatus || !this.ticket) return;
    this.ticketService.changeStatus(this.ticket.id, { status: newStatus }).subscribe({
      next: t => { this.ticket = t; this.statusControl.setValue(t.status); },
      error: err => this.error = 'Invalid status transition'
    });
  }

  addNote(): void {
    const body = this.noteControl.value?.trim();
    if (!body || !this.ticket) return;
    this.noteService.addNote(this.ticket.id, body).subscribe({
      next: note => { this.notes.push(note); this.noteControl.setValue(''); },
      error: () => this.error = 'Failed to add note'
    });
  }
}
```

- [ ] **Step 3: Commit**

```bash
git add frontend/src/ && git commit -m "feat: add ticket detail page with notes and AI chat panel"
```

---

### Task 23: Create ticket + Admin pages

- [ ] **Step 1: Create `features/tickets/create-ticket/create-ticket.component.ts`**

```typescript
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { TicketService } from '../../../core/services/ticket.service';
import { ErrorBannerComponent } from '../../../shared/components/error-banner/error-banner.component';

@Component({
  selector: 'app-create-ticket',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatButtonModule, ErrorBannerComponent],
  template: `
    <h1>Create Ticket</h1>
    <app-error-banner [message]="error"></app-error-banner>
    <mat-card style="max-width:700px">
      <mat-card-content>
        <form [formGroup]="form" (ngSubmit)="onSubmit()" style="display:flex;flex-direction:column;gap:1rem;padding:1rem">
          <mat-form-field>
            <mat-label>Title *</mat-label>
            <input matInput formControlName="title" maxlength="150">
            <mat-error *ngIf="form.get('title')?.hasError('required')">Title is required</mat-error>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Description *</mat-label>
            <textarea matInput formControlName="description" rows="5" maxlength="5000"></textarea>
            <mat-error *ngIf="form.get('description')?.hasError('required')">Description is required</mat-error>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Priority *</mat-label>
            <mat-select formControlName="priority">
              <mat-option value="LOW">LOW</mat-option>
              <mat-option value="MEDIUM">MEDIUM</mat-option>
              <mat-option value="HIGH">HIGH</mat-option>
              <mat-option value="CRITICAL">CRITICAL</mat-option>
            </mat-select>
          </mat-form-field>
          <mat-form-field>
            <mat-label>Category *</mat-label>
            <mat-select formControlName="category">
              <mat-option value="DELIVERY">DELIVERY</mat-option>
              <mat-option value="PICKUP">PICKUP</mat-option>
              <mat-option value="DOCUMENTATION">DOCUMENTATION</mat-option>
              <mat-option value="CUSTOMER">CUSTOMER</mat-option>
              <mat-option value="SYSTEM">SYSTEM</mat-option>
              <mat-option value="OTHER">OTHER</mat-option>
            </mat-select>
          </mat-form-field>
          <mat-form-field>
            <mat-label>External Reference</mat-label>
            <input matInput formControlName="externalRef">
          </mat-form-field>
          <div style="display:flex;gap:1rem">
            <button mat-raised-button color="primary" type="submit" [disabled]="form.invalid || loading">Create</button>
            <button mat-button type="button" (click)="router.navigate(['/tickets'])">Cancel</button>
          </div>
        </form>
      </mat-card-content>
    </mat-card>
  `
})
export class CreateTicketComponent {
  form: FormGroup;
  loading = false;
  error: string | null = null;

  constructor(private fb: FormBuilder, private ticketService: TicketService, public router: Router) {
    this.form = this.fb.group({
      title: ['', [Validators.required, Validators.maxLength(150)]],
      description: ['', [Validators.required, Validators.maxLength(5000)]],
      priority: ['MEDIUM', Validators.required],
      category: ['OTHER', Validators.required],
      externalRef: ['']
    });
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.ticketService.create(this.form.value).subscribe({
      next: t => this.router.navigate(['/tickets', t.id]),
      error: () => { this.error = 'Failed to create ticket'; this.loading = false; }
    });
  }
}
```

- [ ] **Step 2: Create `features/admin/user-list.component.ts`**

```typescript
import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { UserService } from '../../core/services/user.service';
import { User } from '../../core/models/user.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, MatTableModule, MatCardModule, MatChipsModule, LoadingSpinnerComponent],
  template: `
    <h1>Users</h1>
    <app-loading-spinner *ngIf="loading"></app-loading-spinner>
    <mat-card *ngIf="!loading">
      <mat-card-content>
        <table mat-table [dataSource]="users" style="width:100%">
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
  users: User[] = [];
  loading = true;

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.userService.listUsers().subscribe({
      next: u => { this.users = u; this.loading = false; },
      error: () => this.loading = false
    });
  }
}
```

- [ ] **Step 3: Full build check**

```bash
cd frontend && npx ng build --configuration=development 2>&1 | tail -10
```

Expected: Compilation successful

- [ ] **Step 4: Commit**

```bash
cd .. && git add frontend/src/ && git commit -m "feat: add create ticket page and admin user list"
```

---

### Task 24: Frontend unit tests

**Files:**
- Create: `core/auth/auth.service.spec.ts`
- Create: `features/tickets/ticket-list/ticket-list.component.spec.ts`

- [ ] **Step 1: Create `core/auth/auth.service.spec.ts`**

```typescript
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, RouterTestingModule],
      providers: [AuthService]
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/auth/me').flush(null, { status: 401, statusText: 'Unauthorized' });
  });

  afterEach(() => { http.verify(); localStorage.clear(); });

  it('should not be logged in initially', () => {
    expect(service.isLoggedIn()).toBeFalse();
  });

  it('should store token on login', () => {
    service.login({ email: 'admin@example.com', password: 'admin123' }).subscribe();
    http.expectOne('/api/auth/login').flush({ token: 'test-token', email: 'admin@example.com', role: 'ADMIN' });
    http.expectOne('/api/auth/me').flush({ id: '1', email: 'admin@example.com', role: 'ADMIN', active: true });
    expect(service.isLoggedIn()).toBeTrue();
    expect(service.getToken()).toBe('test-token');
  });

  it('should clear token on logout', () => {
    localStorage.setItem('auth_token', 'some-token');
    service.logout();
    expect(service.isLoggedIn()).toBeFalse();
    expect(service.getToken()).toBeNull();
  });

  it('should report isAdmin correctly', () => {
    service.currentUser.set({ id: '1', email: 'a@b.com', role: 'ADMIN', active: true });
    expect(service.isAdmin()).toBeTrue();
    service.currentUser.set({ id: '2', email: 'b@c.com', role: 'OPERATOR', active: true });
    expect(service.isAdmin()).toBeFalse();
  });
});
```

- [ ] **Step 2: Create `features/tickets/ticket-list/ticket-list.component.spec.ts`**

```typescript
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TicketListComponent } from './ticket-list.component';

describe('TicketListComponent', () => {
  let component: TicketListComponent;
  let fixture: ComponentFixture<TicketListComponent>;
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TicketListComponent, HttpClientTestingModule, RouterTestingModule, NoopAnimationsModule]
    }).compileComponents();
    fixture = TestBed.createComponent(TicketListComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load tickets on init', () => {
    fixture.detectChanges();
    const req = http.expectOne(r => r.url.includes('/api/tickets'));
    req.flush({ content: [
      { id: '1', title: 'Test', status: 'NEW', priority: 'HIGH', category: 'DELIVERY',
        createdByEmail: 'op@test.com', createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(), version: 0 }
    ], totalElements: 1, totalPages: 1, number: 0, size: 20 });
    fixture.detectChanges();
    expect(component.tickets.length).toBe(1);
    expect(component.tickets[0].title).toBe('Test');
  });

  it('should show error when loading fails', () => {
    fixture.detectChanges();
    http.expectOne(r => r.url.includes('/api/tickets')).flush('error', { status: 500, statusText: 'Error' });
    fixture.detectChanges();
    expect(component.error).toBeTruthy();
  });
});
```

- [ ] **Step 3: Run frontend tests**

```bash
cd frontend && CHROME_BIN=/usr/bin/chromium npm test -- --watch=false --browsers=ChromeHeadless 2>&1 | tail -20
```

Expected: Tests pass (at least 7 specs: 4 AuthService + 3 TicketListComponent)

- [ ] **Step 4: Commit**

```bash
cd .. && git add frontend/src/ && git commit -m "test: add frontend unit tests for AuthService and TicketListComponent"
```

---

### Task 25: Docker builds + full-stack compose

**Files:**
- Create: `backend/Dockerfile`
- Create: `frontend/Dockerfile`
- Create: `frontend/nginx.conf`

- [ ] **Step 1: Create `backend/Dockerfile`**

```dockerfile
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: Create `frontend/nginx.conf`**

```nginx
server {
    listen 80;
    root /usr/share/nginx/html;
    index index.html;

    location /api/ {
        proxy_pass http://backend:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

- [ ] **Step 3: Create `frontend/Dockerfile`**

```dockerfile
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build -- --configuration=production

FROM nginx:alpine
COPY --from=build /app/dist/frontend/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

- [ ] **Step 4: Verify Docker backend build**

```bash
docker build -t opspilot-backend backend/ 2>&1 | tail -5
```

Expected: Successfully built (may take a few minutes for Maven download)

- [ ] **Step 5: Commit**

```bash
git add backend/Dockerfile frontend/Dockerfile frontend/nginx.conf
git commit -m "feat: add Docker multi-stage builds for backend and frontend"
```

---

### Task 26: Final verification and README

- [ ] **Step 1: Run all backend tests**

```bash
JAVA_HOME=/opt/platform/jdk-21.0.7 mvn -f backend/pom.xml test 2>&1 | tail -15
```

Expected: All tests pass

- [ ] **Step 2: Run frontend tests**

```bash
cd frontend && CHROME_BIN=/usr/bin/chromium npm test -- --watch=false --browsers=ChromeHeadless 2>&1 | tail -10
cd ..
```

Expected: All tests pass

- [ ] **Step 3: Verify full local stack**

```bash
make db-up
# In terminal 2: make backend-run
# Wait for "Started OpsPilotApplication"
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@example.com","password":"admin123"}' | python3 -m json.tool
```

Expected: JSON with token, email, role

- [ ] **Step 4: Update `README.md`**

```markdown
# OpsPilot AI Desk

AI Support Desk for field operations teams — Spring Boot 3.3 + Angular 19 + PostgreSQL.

## Quick Start

### Prerequisites
- JDK 21 at `/opt/platform/jdk-21.0.7`
- Node 22, npm 10
- Docker + Docker Compose

### Local development

```bash
make db-up          # Start PostgreSQL
make backend-run    # Start Spring Boot on :8080
make frontend-run   # Start Angular on :4200
```

Visit http://localhost:4200

Login: admin@example.com / admin123  or  operator@example.com / operator123

### Environment Variables

Copy `.env.example` to `.env` and adjust values. Key variables:
- `AI_PROVIDER=fake|openrouter` — default: `fake`
- `OPENROUTER_API_KEY` — required only when `AI_PROVIDER=openrouter`

### Full Docker stack

```bash
cp .env.example .env
make stack-up   # docker compose --profile fullstack up --build
```

Visit http://localhost:4200

### Tests

```bash
make backend-test    # Unit + integration tests
make frontend-test   # Angular unit tests (requires chromium)
```

## Architecture

```
Angular 19 → Spring Boot 3.3 → PostgreSQL 16
                 ↓
             AiClient (FakeAiClient | OpenRouterClient)
```

- JWT authentication, BCrypt passwords
- Flyway schema migrations
- AI behind interface — no OpenRouter calls without explicit config
- See `docs/superpowers/specs/2026-05-15-opspilot-design.md` for full design

## Swagger UI

http://localhost:8080/swagger-ui.html
```

- [ ] **Step 5: Final commit**

```bash
git add README.md && git commit -m "docs: update README with setup and architecture"
```

---

Implementation complete. Definition of done checklist:
1. Backend starts — `make backend-run`
2. Frontend starts — `make frontend-run`
3. Flyway creates schema automatically at backend startup
4. Seed users can log in — admin@example.com / admin123
5. Tickets can be created and status transitions enforced
6. AI chat works with FakeAiClient by default
7. Tests pass — `make backend-test` and `make frontend-test`
8. README explains setup, architecture, and limitations

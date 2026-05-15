# Ticket Assignment UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Aggiungere controlli di assegnazione/autoassegnazione nella pagina ticket detail.

**Architecture:** Modifica di un solo file (`ticket-detail.component.ts`). Si iniettano `AuthService` e `UserService` già esistenti. Gli admin caricano la lista utenti all'init; tutti gli utenti vedono i bottoni di auto-assign/unassign. La chiamata API usa `ticketService.assign()` già presente.

**Tech Stack:** Angular 21, Signals, Angular Material (`mat-select`, `mat-button`), servizi esistenti.

---

### Task 1: Aggiungere assign controls al ticket detail

**Files:**
- Modify: `frontend/src/app/features/tickets/ticket-detail/ticket-detail.component.ts`

- [ ] **Step 1: Sostituire il file con la versione aggiornata**

Sostituire completamente `frontend/src/app/features/tickets/ticket-detail/ticket-detail.component.ts` con:

```typescript
import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { NgIf, NgFor, DatePipe } from '@angular/common';
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
import { UserService } from '../../../core/services/user.service';
import { AuthService } from '../../../core/auth/auth.service';
import { Ticket, Note } from '../../../core/models/ticket.model';
import { User } from '../../../core/models/user.model';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { ErrorBannerComponent } from '../../../shared/components/error-banner/error-banner.component';
import { AiChatPanelComponent } from '../../ai-chat/ai-chat-panel.component';

@Component({
  selector: 'app-ticket-detail',
  standalone: true,
  imports: [NgIf, NgFor, DatePipe, ReactiveFormsModule, MatCardModule, MatButtonModule,
    MatSelectModule, MatFormFieldModule, MatInputModule, MatDividerModule,
    MatTabsModule, LoadingSpinnerComponent, ErrorBannerComponent, AiChatPanelComponent],
  template: `
    <app-loading-spinner *ngIf="loading()"></app-loading-spinner>
    <app-error-banner [message]="error()"></app-error-banner>

    <div *ngIf="ticket()">
      <div style="display:flex;justify-content:space-between;align-items:flex-start">
        <div>
          <h1>{{ ticket()!.title }}</h1>
          <p style="color:grey">{{ ticket()!.externalRef ? '#' + ticket()!.externalRef + ' · ' : '' }}{{ ticket()!.status }} · {{ ticket()!.priority }} · {{ ticket()!.category }}</p>
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
          <button mat-raised-button color="accent" (click)="changeStatus()" [disabled]="statusControl.value === ticket()!.status">Apply</button>
        </div>
      </div>

      <mat-card style="margin-bottom:1rem">
        <mat-card-content>
          <p><strong>Description:</strong></p>
          <p style="white-space:pre-wrap">{{ ticket()!.description }}</p>
          <mat-divider style="margin:1rem 0"></mat-divider>
          <p><strong>Created by:</strong> {{ ticket()!.createdByEmail }} · <strong>Assigned:</strong> {{ ticket()!.assignedToEmail || 'Unassigned' }}</p>
          <p><strong>Created:</strong> {{ ticket()!.createdAt | date:'medium' }} · <strong>Updated:</strong> {{ ticket()!.updatedAt | date:'medium' }}</p>

          <!-- Assignment controls -->
          <mat-divider style="margin:1rem 0"></mat-divider>
          <div style="display:flex;gap:0.5rem;align-items:center;flex-wrap:wrap">

            <!-- Admin: dropdown + assign button -->
            <ng-container *ngIf="auth.isAdmin()">
              <mat-form-field style="width:220px;margin-bottom:-1.25em">
                <mat-label>Assign to</mat-label>
                <mat-select [formControl]="assigneeControl">
                  <mat-option [value]="null">— Unassigned —</mat-option>
                  <mat-option *ngFor="let u of users()" [value]="u.id">{{ u.email }}</mat-option>
                </mat-select>
              </mat-form-field>
              <button mat-raised-button color="primary"
                      (click)="applyAssign()"
                      [disabled]="assigneeControl.value === currentAssigneeId()">
                Assign
              </button>
            </ng-container>

            <!-- Operator: self-assign or unassign self -->
            <ng-container *ngIf="!auth.isAdmin()">
              <button mat-stroked-button color="primary"
                      *ngIf="ticket()!.assignedToEmail !== auth.currentUser()?.email"
                      (click)="assignToMe()">
                Assign to me
              </button>
              <button mat-stroked-button color="warn"
                      *ngIf="ticket()!.assignedToEmail === auth.currentUser()?.email"
                      (click)="unassign()">
                Unassign
              </button>
            </ng-container>

          </div>
        </mat-card-content>
      </mat-card>

      <mat-tab-group>
        <mat-tab label="Notes">
          <div style="padding:1rem">
            <div *ngFor="let note of notes()" style="margin-bottom:0.75rem;padding:0.75rem;background:#f5f5f5;border-radius:4px">
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
            <app-ai-chat-panel [ticketId]="ticket()!.id"></app-ai-chat-panel>
          </div>
        </mat-tab>
      </mat-tab-group>
    </div>
  `
})
export class TicketDetailComponent implements OnInit {
  ticket = signal<Ticket | null>(null);
  notes = signal<Note[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);
  users = signal<User[]>([]);
  statusControl = new FormControl('');
  noteControl = new FormControl('');
  assigneeControl = new FormControl<string | null>(null);

  constructor(
    private route: ActivatedRoute,
    private ticketService: TicketService,
    private noteService: NoteService,
    private userService: UserService,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.ticketService.getById(id).subscribe({
      next: t => {
        this.ticket.set(t);
        this.statusControl.setValue(t.status);
        this.loading.set(false);
        this.loadNotes(id);
        if (this.auth.isAdmin()) this.loadUsers(t);
      },
      error: () => { this.error.set('Ticket not found'); this.loading.set(false); }
    });
  }

  loadUsers(t: Ticket): void {
    this.userService.listUsers().subscribe({
      next: users => {
        this.users.set(users);
        this.assigneeControl.setValue(this.currentAssigneeId());
      },
      error: () => {}
    });
  }

  loadNotes(id: string): void {
    this.noteService.listNotes(id).subscribe({
      next: notes => this.notes.set(notes),
      error: () => {}
    });
  }

  currentAssigneeId(): string | null {
    const email = this.ticket()?.assignedToEmail ?? null;
    if (!email) return null;
    return this.users().find(u => u.email === email)?.id ?? null;
  }

  assignToMe(): void {
    const id = this.auth.currentUser()?.id;
    if (!id) return;
    this.doAssign(id);
  }

  unassign(): void {
    this.doAssign(null);
  }

  applyAssign(): void {
    this.doAssign(this.assigneeControl.value);
  }

  private doAssign(userId: string | null): void {
    const t = this.ticket();
    if (!t) return;
    this.ticketService.assign(t.id, userId).subscribe({
      next: updated => {
        this.ticket.set(updated);
        this.assigneeControl.setValue(this.currentAssigneeId());
      },
      error: () => { this.error.set('Failed to update assignment'); }
    });
  }

  changeStatus(): void {
    const newStatus = this.statusControl.value;
    const t = this.ticket();
    if (!newStatus || !t) return;
    this.ticketService.changeStatus(t.id, { status: newStatus }).subscribe({
      next: updated => { this.ticket.set(updated); this.statusControl.setValue(updated.status); },
      error: () => { this.error.set('Invalid status transition'); }
    });
  }

  addNote(): void {
    const body = this.noteControl.value?.trim();
    const t = this.ticket();
    if (!body || !t) return;
    this.noteService.addNote(t.id, body).subscribe({
      next: note => { this.notes.update(arr => [...arr, note]); this.noteControl.setValue(''); },
      error: () => { this.error.set('Failed to add note'); }
    });
  }
}
```

- [ ] **Step 2: Verificare il build Angular nel container**

```bash
docker compose up --build -d frontend 2>&1 | tail -10
```

Output atteso: `Container opspilot-ai-desk-frontend-1 Started`  
Se ci sono errori TypeScript il build fallisce e Docker lo riporta nell'output.

- [ ] **Step 3: Test headless — operator self-assign**

Eseguire questo script con Node.js (richiede `/tmp/pwtest/node_modules/playwright`):

```javascript
// /tmp/pwtest/test_assign.js
const { chromium } = require('playwright');
(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  page.on('pageerror', err => console.log('[PAGE ERROR]', err.message));

  // Login come operator
  await page.goto('http://localhost:4200/login', { waitUntil: 'networkidle' });
  await page.fill('input[type="email"]', 'operator@example.com');
  await page.fill('input[type="password"]', 'operator123');
  await page.click('button[type="submit"]');
  await page.waitForURL('**/dashboard', { timeout: 8000 });
  await page.waitForTimeout(1500);

  // Naviga al primo ticket via sidebar
  await page.click('a[routerlink="/tickets"]');
  await page.waitForURL('**/tickets', { timeout: 5000 });
  await page.waitForTimeout(1500);
  await page.locator('table a').first().click();
  await page.waitForURL('**/tickets/*', { timeout: 5000 });
  await page.waitForTimeout(1500);

  const assignBtn = page.locator('button:has-text("Assign to me")');
  const hasAssignBtn = await assignBtn.isVisible().catch(() => false);
  console.log('Bottone "Assign to me" visibile:', hasAssignBtn);

  if (hasAssignBtn) {
    await assignBtn.click();
    await page.waitForTimeout(1500);
    const unassignBtn = page.locator('button:has-text("Unassign")');
    console.log('"Assign to me" scomparso, "Unassign" visibile:', await unassignBtn.isVisible().catch(() => false));
  }

  await browser.close();
})();
```

Eseguire:
```bash
cd /tmp/pwtest && node test_assign.js
```

Output atteso:
```
Bottone "Assign to me" visibile: true
"Assign to me" scomparso, "Unassign" visibile: true
```

- [ ] **Step 4: Test headless — admin assign via dropdown**

```javascript
// /tmp/pwtest/test_assign_admin.js
const { chromium } = require('playwright');
(async () => {
  const browser = await chromium.launch({ headless: true });
  const page = await browser.newPage();
  page.on('pageerror', err => console.log('[PAGE ERROR]', err.message));

  // Login come admin
  await page.goto('http://localhost:4200/login', { waitUntil: 'networkidle' });
  await page.fill('input[type="email"]', 'admin@example.com');
  await page.fill('input[type="password"]', 'admin123');
  await page.click('button[type="submit"]');
  await page.waitForURL('**/dashboard', { timeout: 8000 });
  await page.waitForTimeout(1500);

  // Naviga al primo ticket
  await page.click('a[routerlink="/tickets"]');
  await page.waitForURL('**/tickets', { timeout: 5000 });
  await page.waitForTimeout(1500);
  await page.locator('table a').first().click();
  await page.waitForURL('**/tickets/*', { timeout: 5000 });
  await page.waitForTimeout(1500);

  // Verifica dropdown presente
  const dropdown = page.locator('mat-select').filter({ hasText: /Assign to|Unassigned/ }).first();
  console.log('Dropdown assegnazione visibile:', await dropdown.isVisible().catch(() => false));

  // Seleziona operator@example.com
  await dropdown.click();
  await page.waitForTimeout(500);
  await page.locator('mat-option').filter({ hasText: 'operator@example.com' }).click();
  await page.waitForTimeout(500);

  // Clicca Assign
  await page.locator('button:has-text("Assign")').click();
  await page.waitForTimeout(1500);

  // Verifica che il testo Assigned sia aggiornato
  const assignedText = await page.locator('mat-card-content p:has-text("Assigned")').textContent().catch(() => '');
  console.log('Riga assigned dopo assign:', assignedText.trim());
  console.log('Contiene operator@example.com:', assignedText.includes('operator@example.com'));

  await browser.close();
})();
```

Eseguire:
```bash
cd /tmp/pwtest && node test_assign_admin.js
```

Output atteso:
```
Dropdown assegnazione visibile: true
Riga assigned dopo assign: Created by: operator@example.com · Assigned: operator@example.com
Contiene operator@example.com: true
```

- [ ] **Step 5: Commit**

```bash
cd /home/gianluca/applicazioni/VibeCoding/git/opspilot-ai-desk
git add frontend/src/app/features/tickets/ticket-detail/ticket-detail.component.ts
git commit -m "feat: add ticket assignment UI (self-assign for operators, full assign for admins)"
```

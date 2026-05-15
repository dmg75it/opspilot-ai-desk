import { Component, OnInit } from '@angular/core';
import { NgIf, NgFor } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { DashboardService } from '../../core/services/dashboard.service';
import { DashboardData } from '../../core/models/dashboard.model';
import { LoadingSpinnerComponent } from '../../shared/components/loading-spinner/loading-spinner.component';
import { ErrorBannerComponent } from '../../shared/components/error-banner/error-banner.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [NgIf, NgFor, RouterLink, MatCardModule, MatTableModule,
    MatButtonModule, LoadingSpinnerComponent, ErrorBannerComponent],
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

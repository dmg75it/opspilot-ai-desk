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

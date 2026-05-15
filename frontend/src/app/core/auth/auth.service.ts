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

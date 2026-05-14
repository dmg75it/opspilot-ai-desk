import { Component, inject } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule],
  template: `
    <div class="login-page">
      <div class="login-card">
        <div class="login-header">
          <div class="login-logo">◈</div>
          <h1>OpsPilot AI Desk</h1>
          <p>Sign in to continue</p>
        </div>
        <form [formGroup]="form" (ngSubmit)="onSubmit()" class="login-form">
          <div class="form-group">
            <label>Email</label>
            <input type="email" formControlName="email" placeholder="you@example.com" autocomplete="email" />
            @if (form.get('email')?.invalid && form.get('email')?.touched) {
              <span class="error">Valid email required</span>
            }
          </div>
          <div class="form-group">
            <label>Password</label>
            <input type="password" formControlName="password" placeholder="Password" autocomplete="current-password" />
          </div>
          @if (errorMsg) {
            <div class="alert-error">{{ errorMsg }}</div>
          }
          <button type="submit" [disabled]="loading || form.invalid" class="btn-primary">
            {{ loading ? 'Signing in...' : 'Sign In' }}
          </button>
        </form>
        <div class="login-hint">
          <p>Demo accounts:</p>
          <p>admin@example.com / admin123</p>
          <p>operator@example.com / operator123</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .login-page { min-height: 100vh; display: flex; align-items: center; justify-content: center; background: linear-gradient(135deg, #1a1f36 0%, #2d3561 100%); }
    .login-card { background: white; border-radius: 16px; padding: 48px; width: 100%; max-width: 420px; box-shadow: 0 25px 50px rgba(0,0,0,0.3); }
    .login-header { text-align: center; margin-bottom: 32px; }
    .login-logo { font-size: 48px; color: #6366f1; margin-bottom: 12px; }
    .login-header h1 { font-size: 24px; color: #1a1f36; margin: 0 0 8px; }
    .login-header p { color: #64748b; margin: 0; }
    .form-group { margin-bottom: 20px; }
    .form-group label { display: block; margin-bottom: 6px; font-weight: 500; color: #374151; font-size: 14px; }
    .form-group input { width: 100%; padding: 10px 14px; border: 1px solid #d1d5db; border-radius: 8px; font-size: 15px; box-sizing: border-box; transition: border-color 0.2s; }
    .form-group input:focus { outline: none; border-color: #6366f1; box-shadow: 0 0 0 3px rgba(99,102,241,0.1); }
    .error { color: #dc2626; font-size: 12px; }
    .alert-error { background: #fee2e2; color: #dc2626; padding: 10px 14px; border-radius: 8px; margin-bottom: 16px; font-size: 14px; }
    .btn-primary { width: 100%; padding: 12px; background: #6366f1; color: white; border: none; border-radius: 8px; font-size: 15px; font-weight: 600; cursor: pointer; transition: background 0.2s; }
    .btn-primary:hover:not(:disabled) { background: #4f46e5; }
    .btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
    .login-hint { margin-top: 24px; padding-top: 20px; border-top: 1px solid #e5e7eb; color: #6b7280; font-size: 12px; }
    .login-hint p { margin: 2px 0; }
  `]
})
export class LoginComponent {
  form = inject(FormBuilder).group({
    email: ['operator@example.com', [Validators.required, Validators.email]],
    password: ['operator123', Validators.required]
  });
  loading = false;
  errorMsg = '';
  private auth = inject(AuthService);
  private router = inject(Router);

  onSubmit() {
    if (this.form.invalid) return;
    this.loading = true;
    this.errorMsg = '';
    const { email, password } = this.form.value;
    this.auth.login({ email: email!, password: password! }).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: () => {
        this.errorMsg = 'Invalid credentials. Please try again.';
        this.loading = false;
      }
    });
  }
}

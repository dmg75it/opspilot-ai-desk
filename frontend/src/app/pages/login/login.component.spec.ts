import { TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideAnimations } from '@angular/platform-browser/animations';
import { LoginComponent } from './login.component';
import { AuthService } from '../../core/services/auth.service';
import { of, throwError } from 'rxjs';
import { vi } from 'vitest';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let authService: Partial<AuthService>;

  beforeEach(async () => {
    authService = {
      login: vi.fn(),
      getToken: vi.fn().mockReturnValue(null),
      isLoggedIn: vi.fn().mockReturnValue(false),
      currentUser$: of(null) as any,
      currentUser: null,
      isAdmin: vi.fn().mockReturnValue(false)
    };

    await TestBed.configureTestingModule({
      imports: [LoginComponent, RouterTestingModule.withRoutes([{ path: '**', redirectTo: '' }])],
      providers: [
        { provide: AuthService, useValue: authService },
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimations()
      ]
    }).compileComponents();

    const fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('form should be invalid when empty', () => {
    expect(component.form.valid).toBe(false);
  });

  it('email field should be invalid with wrong format', () => {
    component.form.get('email')!.setValue('not-an-email');
    expect(component.form.get('email')!.valid).toBe(false);
  });

  it('email field should be valid with correct format', () => {
    component.form.get('email')!.setValue('user@example.com');
    expect(component.form.get('email')!.valid).toBe(true);
  });

  it('password field should be invalid when empty', () => {
    component.form.get('password')!.setValue('');
    expect(component.form.get('password')!.valid).toBe(false);
  });

  it('password field should be valid when provided', () => {
    component.form.get('password')!.setValue('password123');
    expect(component.form.get('password')!.valid).toBe(true);
  });

  it('submit should not call login when form is invalid', () => {
    component.submit();
    expect(authService.login).not.toHaveBeenCalled();
  });

  it('submit should call AuthService.login with form values', () => {
    (authService.login as ReturnType<typeof vi.fn>).mockReturnValue(
      of({ token: 'tok', user: { id: 1, email: 'a@b.com', fullName: 'A', role: 'ADMIN' } })
    );
    component.form.setValue({ email: 'admin@example.com', password: 'admin123' });
    component.submit();
    expect(authService.login).toHaveBeenCalledWith('admin@example.com', 'admin123');
  });

  it('should set errorMessage on login failure with 401', () => {
    (authService.login as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => ({ status: 401 }))
    );
    component.form.setValue({ email: 'bad@example.com', password: 'wrongpass' });
    component.submit();
    expect(component.errorMessage).toBe('Invalid email or password.');
  });

  it('should set generic errorMessage on non-401 error', () => {
    (authService.login as ReturnType<typeof vi.fn>).mockReturnValue(
      throwError(() => ({ status: 500 }))
    );
    component.form.setValue({ email: 'bad@example.com', password: 'wrongpass' });
    component.submit();
    expect(component.errorMessage).toBe('Login failed. Please try again.');
  });
});

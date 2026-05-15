import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Component } from '@angular/core';
import { AuthService } from './auth.service';

@Component({ template: '', standalone: true })
class DummyComponent {}

describe('AuthService', () => {
  let service: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideRouter([{ path: 'login', component: DummyComponent }]),
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => { http.verify(); localStorage.clear(); });

  it('should not be logged in initially', () => {
    expect(service.isLoggedIn()).toBe(false);
  });

  it('should store token on login', () => {
    service.login({ email: 'admin@example.com', password: 'admin123' }).subscribe();
    http.expectOne('/api/auth/login').flush({ token: 'test-token', email: 'admin@example.com', role: 'ADMIN' });
    http.expectOne('/api/auth/me').flush({ id: '1', email: 'admin@example.com', role: 'ADMIN', active: true });
    expect(service.isLoggedIn()).toBe(true);
    expect(service.getToken()).toBe('test-token');
  });

  it('should clear token on logout', () => {
    localStorage.setItem('auth_token', 'some-token');
    service.logout();
    // logout triggers loadCurrentUser() error path which calls logout() again — drain any pending requests
    http.match('/api/auth/me');
    expect(service.isLoggedIn()).toBe(false);
    expect(service.getToken()).toBeNull();
  });

  it('should report isAdmin correctly', () => {
    service.currentUser.set({ id: '1', email: 'a@b.com', role: 'ADMIN', active: true });
    expect(service.isAdmin()).toBe(true);
    service.currentUser.set({ id: '2', email: 'b@c.com', role: 'OPERATOR', active: true });
    expect(service.isAdmin()).toBe(false);
  });
});

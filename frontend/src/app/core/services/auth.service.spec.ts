import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { RouterTestingModule } from '@angular/router/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { AuthResponse } from '../models/user.model';

const mockAuthResponse: AuthResponse = {
  token: 'test-token-123',
  user: { id: 1, email: 'admin@example.com', fullName: 'Admin User', role: 'ADMIN' }
};

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [RouterTestingModule.withRoutes([{ path: '**', redirectTo: '' }])],
      providers: [AuthService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    // Flush any initial /me request if token was in storage
    const pending = httpMock.match(() => true);
    pending.forEach(r => r.flush({}, { status: 401, statusText: 'Unauthorized' }));
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should not be logged in initially', () => {
    expect(service.isLoggedIn()).toBe(false);
  });

  it('login should store token and set current user', () => {
    service.login('admin@example.com', 'admin123').subscribe(response => {
      expect(response.token).toBe('test-token-123');
    });

    const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush(mockAuthResponse);

    expect(service.isLoggedIn()).toBe(true);
    expect(service.getToken()).toBe('test-token-123');
    expect(service.currentUser?.email).toBe('admin@example.com');
  });

  it('logout should remove token and clear user', () => {
    service.login('admin@example.com', 'admin123').subscribe();
    const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
    req.flush(mockAuthResponse);

    service.logout();
    expect(service.isLoggedIn()).toBe(false);
    expect(service.getToken()).toBeNull();
    expect(service.currentUser).toBeNull();
  });

  it('isLoggedIn returns true when token is stored', () => {
    localStorage.setItem('opspilot_token', 'some-token');
    expect(service.isLoggedIn()).toBe(true);
  });

  it('isAdmin returns true when current user is ADMIN', () => {
    service.login('admin@example.com', 'admin123').subscribe();
    const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
    req.flush(mockAuthResponse);
    expect(service.isAdmin()).toBe(true);
  });

  it('isAdmin returns false when current user is OPERATOR', () => {
    const operatorResponse: AuthResponse = {
      token: 'op-token',
      user: { id: 2, email: 'operator@example.com', fullName: 'Operator', role: 'OPERATOR' }
    };
    service.login('operator@example.com', 'operator123').subscribe();
    const req = httpMock.expectOne('http://localhost:8080/api/auth/login');
    req.flush(operatorResponse);
    expect(service.isAdmin()).toBe(false);
  });
});

import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { AuthService } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([])
      ]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should not be authenticated without token', () => {
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('should store token on login', () => {
    const mockResponse = {
      token: 'test.token.value',
      user: { id: '1', email: 'test@example.com', fullName: 'Test', role: 'OPERATOR' as const }
    };

    service.login({ email: 'test@example.com', password: 'password' }).subscribe();

    const req = httpMock.expectOne(req => req.url.includes('/auth/login'));
    req.flush(mockResponse);

    expect(localStorage.getItem('opspilot_token')).toBe('test.token.value');
    expect(service.currentUser()?.email).toBe('test@example.com');
  });

  it('logout should clear token and user', () => {
    service['currentUser'].set({ id: '1', email: 'x@x.com', fullName: 'X', role: 'OPERATOR' });
    localStorage.setItem('opspilot_token', 'some-token');
    service.logout();
    expect(service.isAuthenticated()).toBeFalse();
    expect(localStorage.getItem('opspilot_token')).toBeNull();
  });

  it('isAdmin should return true for ADMIN role', () => {
    service['currentUser'].set({ id: '1', email: 'a@a.com', fullName: 'A', role: 'ADMIN' });
    expect(service.isAdmin()).toBeTrue();
  });
});

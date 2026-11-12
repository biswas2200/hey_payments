import { describe, expect, it, vi } from 'vitest';
import { HttpRequest, HttpHandlerFn, HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { authInterceptor } from './auth.interceptor';
import { AuthStore } from './auth.store';

describe('authInterceptor', () => {
  it('should attach Bearer token to protected API requests when accessToken is present', () => {
    const authStoreMock = {
      accessToken: vi.fn().mockReturnValue('valid-jwt-token'),
      refreshSession: vi.fn(),
    } as unknown as AuthStore;

    const request = new HttpRequest('GET', '/api/v1/users/me');
    let capturedReq: HttpRequest<unknown> | null = null;
    const next: HttpHandlerFn = (req) => {
      capturedReq = req;
      return of(new HttpResponse({ status: 200 }));
    };

    authInterceptor(request, next, authStoreMock).subscribe();

    expect(capturedReq).not.toBeNull();
    expect(capturedReq!.headers.get('Authorization')).toBe('Bearer valid-jwt-token');
  });

  it('should NOT attach Bearer token to public endpoints', () => {
    const authStoreMock = {
      accessToken: vi.fn().mockReturnValue('valid-jwt-token'),
      refreshSession: vi.fn(),
    } as unknown as AuthStore;

    const request = new HttpRequest('POST', '/api/v1/auth/login', {});
    let capturedReq: HttpRequest<unknown> | null = null;
    const next: HttpHandlerFn = (req) => {
      capturedReq = req;
      return of(new HttpResponse({ status: 200 }));
    };

    authInterceptor(request, next, authStoreMock).subscribe();

    expect(capturedReq).not.toBeNull();
    expect(capturedReq!.headers.has('Authorization')).toBe(false);
  });

  it('should trigger refreshSession on 401 response and retry request', () => {
    const authStoreMock = {
      accessToken: vi.fn().mockReturnValue('refreshed-jwt-token'),
      refreshSession: vi.fn().mockReturnValue(of({ accessToken: 'refreshed-jwt-token' })),
    } as unknown as AuthStore;

    const request = new HttpRequest('GET', '/api/v1/users/me');
    let calls = 0;
    const next: HttpHandlerFn = () => {
      calls++;
      if (calls === 1) {
        return throwError(() => new HttpErrorResponse({ status: 401 }));
      }
      return of(new HttpResponse({ status: 200 }));
    };

    authInterceptor(request, next, authStoreMock).subscribe();

    expect(authStoreMock.refreshSession).toHaveBeenCalled();
  });
});

import { describe, expect, it, vi } from 'vitest';
import { HttpRequest, HttpHandlerFn, HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { of, throwError, firstValueFrom } from 'rxjs';
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

  it('should deduplicate concurrent 401 refresh calls to prevent token rotation race condition', async () => {
    let refreshCalls = 0;
    const authStoreMock = {
      accessToken: vi.fn().mockReturnValue('refreshed-token'),
      refreshSession: vi.fn().mockImplementation(() => {
        refreshCalls++;
        return new Promise((resolve) => {
          setTimeout(() => {
            resolve({
              accessToken: 'refreshed-token',
              expiresIn: 900,
              user: { id: 'u1', role: 'PAYER', displayName: 'User' },
            });
          }, 20);
        });
      }),
    } as unknown as AuthStore;

    let req1Calls = 0;
    let req2Calls = 0;

    const next1: HttpHandlerFn = () => {
      req1Calls++;
      if (req1Calls === 1) {
        return throwError(() => new HttpErrorResponse({ status: 401 }));
      }
      return of(new HttpResponse({ status: 200, body: 'res1' }));
    };

    const next2: HttpHandlerFn = () => {
      req2Calls++;
      if (req2Calls === 1) {
        return throwError(() => new HttpErrorResponse({ status: 401 }));
      }
      return of(new HttpResponse({ status: 200, body: 'res2' }));
    };

    const req1 = new HttpRequest('GET', '/api/v1/users/me');
    const req2 = new HttpRequest('GET', '/api/v1/devices/me');

    const [res1, res2] = await Promise.all([
      firstValueFrom(authInterceptor(req1, next1, authStoreMock)),
      firstValueFrom(authInterceptor(req2, next2, authStoreMock)),
    ]);

    expect((res1 as HttpResponse<string>).body).toBe('res1');
    expect((res2 as HttpResponse<string>).body).toBe('res2');
    expect(authStoreMock.refreshSession).toHaveBeenCalledTimes(1);
    expect(refreshCalls).toBe(1);
  });
});

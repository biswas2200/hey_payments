import { describe, expect, it, vi } from 'vitest';
import { HttpRequest, HttpHandlerFn, HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { of, throwError, firstValueFrom } from 'rxjs';
import { authInterceptor } from './auth.interceptor';
import { TokenStore } from './token.store';
import { TokenRefreshService } from './token-refresh.service';

describe('authInterceptor', () => {
  it('should attach Bearer token to protected API requests when accessToken is present', () => {
    const tokenStoreMock = {
      accessToken: vi.fn().mockReturnValue('valid-jwt-token'),
    } as unknown as TokenStore;
    const refreshServiceMock = {
      refreshSession: vi.fn(),
    } as unknown as TokenRefreshService;

    const request = new HttpRequest('GET', '/api/v1/users/me');
    let capturedReq: HttpRequest<unknown> | null = null;
    const next: HttpHandlerFn = (req) => {
      capturedReq = req;
      return of(new HttpResponse({ status: 200 }));
    };

    authInterceptor(request, next, tokenStoreMock, refreshServiceMock).subscribe();

    expect(capturedReq).not.toBeNull();
    expect(capturedReq!.headers.get('Authorization')).toBe('Bearer valid-jwt-token');
  });

  it('should NOT attach Bearer token to public endpoints', () => {
    const tokenStoreMock = {
      accessToken: vi.fn().mockReturnValue('valid-jwt-token'),
    } as unknown as TokenStore;
    const refreshServiceMock = {
      refreshSession: vi.fn(),
    } as unknown as TokenRefreshService;

    const request = new HttpRequest('POST', '/api/v1/auth/login', {});
    let capturedReq: HttpRequest<unknown> | null = null;
    const next: HttpHandlerFn = (req) => {
      capturedReq = req;
      return of(new HttpResponse({ status: 200 }));
    };

    authInterceptor(request, next, tokenStoreMock, refreshServiceMock).subscribe();

    expect(capturedReq).not.toBeNull();
    expect(capturedReq!.headers.has('Authorization')).toBe(false);
  });

  it('should trigger refreshSession on 401 response and retry request', () => {
    const tokenStoreMock = {
      accessToken: vi.fn().mockReturnValue('refreshed-jwt-token'),
    } as unknown as TokenStore;
    const refreshServiceMock = {
      refreshSession: vi.fn().mockReturnValue(of({ accessToken: 'refreshed-jwt-token' })),
    } as unknown as TokenRefreshService;

    const request = new HttpRequest('GET', '/api/v1/users/me');
    let calls = 0;
    const next: HttpHandlerFn = () => {
      calls++;
      if (calls === 1) {
        return throwError(() => new HttpErrorResponse({ status: 401 }));
      }
      return of(new HttpResponse({ status: 200 }));
    };

    authInterceptor(request, next, tokenStoreMock, refreshServiceMock).subscribe();

    expect(refreshServiceMock.refreshSession).toHaveBeenCalled();
  });

  it('should retry requests with refreshed token on 401', async () => {
    const tokenStoreMock = {
      accessToken: vi.fn().mockReturnValue('old-token'),
    } as unknown as TokenStore;
    const refreshServiceMock = {
      refreshSession: vi.fn().mockReturnValue(
        of({
          accessToken: 'refreshed-token',
          expiresIn: 900,
          user: { id: 'u1', role: 'PAYER', displayName: 'User' },
        }),
      ),
    } as unknown as TokenRefreshService;

    let calls = 0;
    const next: HttpHandlerFn = (req) => {
      calls++;
      if (calls === 1) {
        return throwError(() => new HttpErrorResponse({ status: 401 }));
      }
      expect(req.headers.get('Authorization')).toBe('Bearer refreshed-token');
      return of(new HttpResponse({ status: 200, body: 'success' }));
    };

    const req = new HttpRequest('GET', '/api/v1/users/me');
    const res = await firstValueFrom(
      authInterceptor(req, next, tokenStoreMock, refreshServiceMock),
    );

    expect((res as HttpResponse<string>).body).toBe('success');
    expect(refreshServiceMock.refreshSession).toHaveBeenCalledTimes(1);
  });
});

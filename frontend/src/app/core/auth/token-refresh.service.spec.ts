import { describe, expect, it, vi } from 'vitest';
import { HttpBackend, HttpRequest, HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { of, throwError, firstValueFrom } from 'rxjs';
import { TokenRefreshService } from './token-refresh.service';
import { TokenStore } from './token.store';
import { TestBed } from '@angular/core/testing';

describe('TokenRefreshService', () => {
  it('should refresh session via HttpBackend and update TokenStore', async () => {
    const mockBackend = {
      handle: vi.fn().mockImplementation((req: HttpRequest<unknown>) => {
        expect(req.url).toBe('/api/v1/auth/refresh');
        expect(req.withCredentials).toBe(true);
        return of(
          new HttpResponse({
            status: 200,
            body: {
              accessToken: 'refreshed-jwt',
              expiresIn: 900,
              user: { id: 'u-1', role: 'PAYER', displayName: 'Payer' },
            },
          }),
        );
      }),
    } as unknown as HttpBackend;

    TestBed.configureTestingModule({
      providers: [TokenStore, { provide: HttpBackend, useValue: mockBackend }, TokenRefreshService],
    });

    const refreshService = TestBed.inject(TokenRefreshService);
    const tokenStore = TestBed.inject(TokenStore);

    const res = await firstValueFrom(refreshService.refreshSession());
    expect(res.accessToken).toBe('refreshed-jwt');
    expect(tokenStore.accessToken()).toBe('refreshed-jwt');
  });

  it('should clear TokenStore on refresh failure', async () => {
    const mockBackend = {
      handle: vi.fn().mockImplementation(() => {
        return throwError(() => new HttpErrorResponse({ status: 401 }));
      }),
    } as unknown as HttpBackend;

    TestBed.configureTestingModule({
      providers: [TokenStore, { provide: HttpBackend, useValue: mockBackend }, TokenRefreshService],
    });

    const refreshService = TestBed.inject(TokenRefreshService);
    const tokenStore = TestBed.inject(TokenStore);
    tokenStore.setToken('old-token', 900);

    await expect(firstValueFrom(refreshService.refreshSession())).rejects.toThrow();
    expect(tokenStore.accessToken()).toBeNull();
  });
});

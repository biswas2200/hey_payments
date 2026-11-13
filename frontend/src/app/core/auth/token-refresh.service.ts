/**
 * Token Refresh Service via HttpBackend.
 * Bypasses all functional HTTP interceptors to ensure token refresh requests
 * never re-enter the interceptor chain or cause recursive calls.
 */
import { inject, Injectable } from '@angular/core';
import { HttpBackend, HttpClient } from '@angular/common/http';
import { Observable, catchError, finalize, shareReplay, tap, throwError } from 'rxjs';
import { TokenStore } from './token.store';
import type { TokenResponse } from '../api/models';

@Injectable({
  providedIn: 'root',
})
export class TokenRefreshService {
  private readonly http: HttpClient;
  private readonly tokenStore = inject(TokenStore);
  private activeRefresh$: Observable<TokenResponse> | null = null;

  constructor() {
    const backend = inject(HttpBackend);
    this.http = new HttpClient(backend);
  }

  refreshSession(): Observable<TokenResponse> {
    if (!this.activeRefresh$) {
      this.activeRefresh$ = this.http
        .post<TokenResponse>('/api/v1/auth/refresh', {}, { withCredentials: true })
        .pipe(
          tap((tokenResp) => {
            this.tokenStore.setToken(tokenResp.accessToken, tokenResp.expiresIn);
          }),
          catchError((err) => {
            this.tokenStore.clear();
            return throwError(() => err);
          }),
          shareReplay(1),
          finalize(() => {
            this.activeRefresh$ = null;
          }),
        );
    }
    return this.activeRefresh$;
  }
}

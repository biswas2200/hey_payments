/**
 * Functional HTTP Interceptor for Authentication
 * Attaches Bearer token from TokenStore, enables credentials, and auto-refreshes on 401 via TokenRefreshService.
 * Completely dependency-free from AuthStore to eliminate circular DI cycles (NG0200).
 */

import { inject } from '@angular/core';
import {
  HttpRequest,
  HttpHandlerFn,
  HttpEvent,
  HttpErrorResponse,
  type HttpInterceptorFn,
} from '@angular/common/http';
import { Observable, catchError, switchMap, throwError } from 'rxjs';
import { TokenStore } from './token.store';
import { TokenRefreshService } from './token-refresh.service';

const PUBLIC_URL_PATTERNS = [
  '/auth/login',
  '/auth/register',
  '/auth/verify-otp',
  '/auth/refresh',
  '/keys/jwks',
  '/health',
];

export function authInterceptor(
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  injectedTokenStore?: TokenStore,
  injectedRefreshService?: TokenRefreshService,
): Observable<HttpEvent<unknown>> {
  const tokenStore = injectedTokenStore ?? inject(TokenStore);
  const refreshService = injectedRefreshService ?? inject(TokenRefreshService);
  const token = tokenStore.accessToken();

  const isPublic = PUBLIC_URL_PATTERNS.some((pattern) => req.url.includes(pattern));

  let modifiedReq = req.clone({
    withCredentials: true,
  });

  if (token && !isPublic) {
    modifiedReq = modifiedReq.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
  }

  return next(modifiedReq).pipe(
    catchError((err: unknown) => {
      if (
        err instanceof HttpErrorResponse &&
        err.status === 401 &&
        !req.url.includes('/auth/refresh')
      ) {
        return refreshService.refreshSession().pipe(
          switchMap((newTokenResp) => {
            const retriedReq = modifiedReq.clone({
              setHeaders: {
                Authorization: `Bearer ${newTokenResp.accessToken}`,
              },
            });
            return next(retriedReq);
          }),
          catchError((refreshErr) => throwError(() => refreshErr)),
        );
      }
      return throwError(() => err);
    }),
  );
}

export const provideAuthInterceptor: HttpInterceptorFn = (req, next) => {
  return authInterceptor(req, next);
};

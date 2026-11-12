/**
 * Functional HTTP Interceptor for Authentication
 * Attaches Bearer token, enables credentials, and auto-refreshes on 401.
 */

import { inject } from '@angular/core';
import {
  HttpRequest,
  HttpHandlerFn,
  HttpEvent,
  HttpErrorResponse,
  type HttpInterceptorFn,
} from '@angular/common/http';
import { Observable, catchError, switchMap, throwError, from, shareReplay, finalize } from 'rxjs';
import { AuthStore } from './auth.store';
import type { TokenResponse } from '../api/models';

let activeRefresh$: Observable<TokenResponse> | null = null;

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
  injectedAuthStore?: AuthStore,
): Observable<HttpEvent<unknown>> {
  const store = injectedAuthStore ?? inject(AuthStore);
  const token = store.accessToken();

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
        if (!activeRefresh$) {
          activeRefresh$ = from(store.refreshSession()).pipe(
            shareReplay(1),
            finalize(() => {
              activeRefresh$ = null;
            }),
          );
        }

        return activeRefresh$.pipe(
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

/**
 * Functional Route Guard for Authenticated Routes
 */

import { inject } from '@angular/core';
import { Router, type CanActivateFn, type UrlTree } from '@angular/router';
import { AuthStore } from './auth.store';

export function authGuard(
  injectedAuthStore?: AuthStore,
  injectedRouter?: Router,
): boolean | UrlTree {
  const store = injectedAuthStore ?? inject(AuthStore);
  const router = injectedRouter ?? inject(Router);

  if (store.isAuthenticated()) {
    return true;
  }

  return router.createUrlTree(['/auth/login']);
}

export const canActivateAuth: CanActivateFn = () => {
  return authGuard();
};

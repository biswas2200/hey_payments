import { describe, expect, it, vi } from 'vitest';
import { authGuard } from './auth.guard';
import { AuthStore } from './auth.store';
import { Router } from '@angular/router';

describe('authGuard', () => {
  it('should allow access when user is authenticated', () => {
    const authStoreMock = {
      isAuthenticated: vi.fn().mockReturnValue(true),
    } as unknown as AuthStore;
    const routerMock = {
      createUrlTree: vi.fn(),
    } as unknown as Router;

    const result = authGuard(authStoreMock, routerMock);
    expect(result).toBe(true);
  });

  it('should redirect to /auth/login when user is unauthenticated', () => {
    const authStoreMock = {
      isAuthenticated: vi.fn().mockReturnValue(false),
    } as unknown as AuthStore;
    const routerMock = {
      createUrlTree: vi.fn().mockReturnValue('/auth/login'),
    } as unknown as Router;

    const result = authGuard(authStoreMock, routerMock);
    expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/auth/login']);
    expect(result).toBe('/auth/login');
  });
});

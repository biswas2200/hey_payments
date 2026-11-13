import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { describe, expect, it } from 'vitest';
import { appConfig } from '../../app.config';
import { AuthStore } from './auth.store';
import { LoginComponent } from '../../features/auth/login/login.component';

describe('Auth Dependency Injection & Routing Configuration', () => {
  it('should instantiate AuthStore and LoginComponent without circular DI error using real appConfig providers', async () => {
    TestBed.configureTestingModule({
      providers: [...appConfig.providers],
    });

    const router = TestBed.inject(Router);
    const store = TestBed.inject(AuthStore);
    expect(store).toBeTruthy();

    const loginFixture = TestBed.createComponent(LoginComponent);
    expect(loginFixture.componentInstance).toBeTruthy();

    // Test routing navigation to all auth routes
    const publicRoutes = ['/auth/login', '/auth/register', '/auth/verify-otp'];
    for (const route of publicRoutes) {
      const navigated = await router.navigateByUrl(route);
      expect(navigated).toBe(true);
    }

    // Set authenticated user to test protected auth routes
    store.currentUser.set({
      id: '01931234-0000-7000-8000-000000000001',
      phone: '+919876543210',
      email: 'payer@example.com',
      role: 'PAYER',
      status: 'ACTIVE',
      createdAt: '2026-11-12T10:00:00Z',
    });

    const protectedRoutes = ['/auth/device-register', '/auth/set-pin'];
    for (const route of protectedRoutes) {
      const navigated = await router.navigateByUrl(route);
      expect(navigated).toBe(true);
    }
  });
});

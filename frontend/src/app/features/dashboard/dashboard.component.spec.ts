import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { DashboardComponent } from './dashboard.component';
import { AuthStore } from '../../core/auth/auth.store';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let authStoreMock: { logout: ReturnType<typeof vi.fn> };
  let routerMock: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    authStoreMock = {
      logout: vi.fn().mockResolvedValue(undefined),
    };
    routerMock = {
      navigate: vi.fn().mockResolvedValue(true),
    };

    await TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        { provide: AuthStore, useValue: authStoreMock },
        { provide: Router, useValue: routerMock },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should render logout control with log-out icon and label', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    const logoutBtn = compiled.querySelector('#logout-btn');
    expect(logoutBtn).not.toBeNull();
    expect(logoutBtn?.textContent).toContain('Log Out');
    expect(logoutBtn?.querySelector('svg[lucideLogOut]')).not.toBeNull();
  });

  it('should call auth.logout and navigate to /auth/login on logout click', async () => {
    await component.onLogout();
    expect(authStoreMock.logout).toHaveBeenCalled();
    expect(routerMock.navigate).toHaveBeenCalledWith(['/auth/login']);
  });
});

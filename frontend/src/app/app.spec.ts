import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { App } from './app';
import { ConnectivityService } from './core/connectivity/connectivity.service';

describe('App', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [App],
      providers: [provideHttpClient(), provideHttpClientTesting(), ConnectivityService],
    }).compileComponents();
  });

  it('should create the app', () => {
    const fixture = TestBed.createComponent(App);
    const app = fixture.componentInstance;
    expect(app).toBeTruthy();
  });

  it('should render title', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Hey Payments');
  });

  it('should render connectivity banner with status', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await fixture.whenStable();
    const banner = fixture.nativeElement.querySelector('.connectivity-banner');
    expect(banner).toBeTruthy();
    expect(banner?.getAttribute('role')).toBe('status');
  });

  it('should reflect offline status when connectivity changes', async () => {
    const fixture = TestBed.createComponent(App);
    const connectivityService = TestBed.inject(ConnectivityService);
    connectivityService.setStatus('offline');
    fixture.detectChanges();
    await fixture.whenStable();
    const banner = fixture.nativeElement.querySelector('.connectivity-banner');
    expect(banner?.textContent).toContain('Offline');
  });
});

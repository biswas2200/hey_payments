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

  it('should render brand logo image', async () => {
    const fixture = TestBed.createComponent(App);
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    const logo = compiled.querySelector('img.app-logo');
    expect(logo).toBeTruthy();
    expect(logo?.getAttribute('src')).toBe('brand/logo-wordmark-light.svg');
  });

  it('should render online status banner with approved language: Online · synced', async () => {
    const fixture = TestBed.createComponent(App);
    const connectivityService = TestBed.inject(ConnectivityService);
    connectivityService.setStatus('online');
    fixture.detectChanges();
    await fixture.whenStable();
    const banner = fixture.nativeElement.querySelector('.connectivity-banner');
    expect(banner?.textContent).toContain('Online · synced');
  });

  it('should render offline status banner with approved language: Offline · payments still work', async () => {
    const fixture = TestBed.createComponent(App);
    const connectivityService = TestBed.inject(ConnectivityService);
    connectivityService.setStatus('offline');
    fixture.detectChanges();
    await fixture.whenStable();
    const banner = fixture.nativeElement.querySelector('.connectivity-banner');
    expect(banner?.textContent).toContain('Offline · payments still work');
  });

  it('should never expose raw developer instructions or localhost:8080 in UI', async () => {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    await fixture.whenStable();
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).not.toContain('http://localhost:8080');
    expect(compiled.textContent).not.toContain('./mvnw spring-boot:run');
  });
});

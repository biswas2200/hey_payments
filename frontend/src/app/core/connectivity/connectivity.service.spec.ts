import { TestBed } from '@angular/core/testing';
import { ConnectivityService } from './connectivity.service';

describe('ConnectivityService', () => {
  let service: ConnectivityService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ConnectivityService],
    });
    service = TestBed.inject(ConnectivityService);
  });

  it('should be created and report current navigator online status', () => {
    expect(service).toBeTruthy();
    expect(service.status()).toBe(navigator.onLine ? 'online' : 'offline');
    expect(service.isOnline()).toBe(navigator.onLine);
  });

  it('should switch to offline when window offline event is dispatched', () => {
    window.dispatchEvent(new Event('offline'));
    expect(service.status()).toBe('offline');
    expect(service.isOnline()).toBe(false);
  });

  it('should switch to online when window online event is dispatched', () => {
    window.dispatchEvent(new Event('offline'));
    expect(service.status()).toBe('offline');

    window.dispatchEvent(new Event('online'));
    expect(service.status()).toBe('online');
    expect(service.isOnline()).toBe(true);
  });

  it('should allow setting status explicitly', () => {
    service.setStatus('degraded');
    expect(service.status()).toBe('degraded');
    expect(service.isOnline()).toBe(false);
  });
});

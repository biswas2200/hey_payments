import { DestroyRef, Injectable, computed, inject, signal } from '@angular/core';

export type ConnectivityStatus = 'online' | 'offline' | 'degraded';

@Injectable({
  providedIn: 'root',
})
export class ConnectivityService {
  private readonly _status = signal<ConnectivityStatus>(
    typeof navigator !== 'undefined' && navigator.onLine ? 'online' : 'offline',
  );

  public readonly status = this._status.asReadonly();
  public readonly isOnline = computed(() => this.status() === 'online');

  constructor() {
    if (typeof window !== 'undefined') {
      const handleOnline = (): void => {
        this._status.set('online');
      };
      const handleOffline = (): void => {
        this._status.set('offline');
      };

      window.addEventListener('online', handleOnline);
      window.addEventListener('offline', handleOffline);

      const destroyRef = inject(DestroyRef);
      destroyRef.onDestroy(() => {
        window.removeEventListener('online', handleOnline);
        window.removeEventListener('offline', handleOffline);
      });
    }
  }

  public setStatus(newStatus: ConnectivityStatus): void {
    this._status.set(newStatus);
  }
}

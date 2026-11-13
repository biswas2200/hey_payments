/**
 * Dependency-free TokenStore (Angular 22 Signals).
 * Manages access token and expiry state without depending on HttpClient or AuthStore,
 * preventing circular dependency cycles (NG0200).
 */
import { Injectable, signal } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class TokenStore {
  readonly accessToken = signal<string | null>(null);
  readonly expiresAt = signal<number | null>(null);

  setToken(token: string | null, expiresInSeconds?: number): void {
    this.accessToken.set(token);
    if (token && expiresInSeconds) {
      this.expiresAt.set(Date.now() + expiresInSeconds * 1000);
    } else {
      this.expiresAt.set(null);
    }
  }

  clear(): void {
    this.accessToken.set(null);
    this.expiresAt.set(null);
  }
}

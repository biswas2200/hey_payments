import { ApplicationConfig, provideBrowserGlobalErrorListeners, isDevMode } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { provideServiceWorker } from '@angular/service-worker';
import { provideAuthInterceptor } from './core/auth/auth.interceptor';
import { KeyService } from './core/crypto/key.service';
import { provideApiConfiguration } from './core/api/generated/api-configuration';

export const appConfig: ApplicationConfig = {
  providers: [
    provideApiConfiguration('/api/v1'),
    KeyService,
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideHttpClient(withFetch(), withInterceptors([provideAuthInterceptor])),
    provideServiceWorker('ngsw-worker.js', {
      enabled: !isDevMode(),
      registrationStrategy: 'registerWhenStable:30000',
    }),
  ],
};

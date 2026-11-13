import { Routes } from '@angular/router';
import { canActivateAuth } from './core/auth/auth.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'auth/login',
  },
  {
    path: 'auth',
    children: [
      {
        path: '',
        pathMatch: 'full',
        redirectTo: 'login',
      },
      {
        path: 'login',
        loadComponent: () =>
          import('./features/auth/login/login.component').then((m) => m.LoginComponent),
      },
      {
        path: 'register',
        loadComponent: () =>
          import('./features/auth/register/register.component').then((m) => m.RegisterComponent),
      },
      {
        path: 'verify-otp',
        loadComponent: () =>
          import('./features/auth/verify-otp/verify-otp.component').then(
            (m) => m.VerifyOtpComponent,
          ),
      },
      {
        path: 'set-pin',
        canActivate: [canActivateAuth],
        loadComponent: () =>
          import('./features/auth/set-pin/set-pin.component').then((m) => m.SetPinComponent),
      },
      {
        path: 'device-register',
        canActivate: [canActivateAuth],
        loadComponent: () =>
          import('./features/auth/device-register/device-register.component').then(
            (m) => m.DeviceRegisterComponent,
          ),
      },
    ],
  },
  {
    path: 'pay',
    canActivate: [canActivateAuth],
    loadComponent: () => import('./features/home/home.component').then((m) => m.HomeComponent),
  },
  {
    path: 'wallet/fund',
    canActivate: [canActivateAuth],
    loadComponent: () =>
      import('./features/wallet/fund/fund.component').then((m) => m.FundComponent),
  },
  {
    path: 'merchant',
    canActivate: [canActivateAuth],
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
  },
];

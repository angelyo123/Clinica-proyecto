import { Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';

// Componentes públicos
import { LoginComponent } from './shared/components/login/login.component';
import { RegisterComponent } from './shared/components/register/register.component';

// (opcional) dashboard general o público
import { Dashboard } from './shared/components/dashboard/dashboard';
import { PACIENTE_ROUTES } from './features/paciente/paciente.routes';

export const APP_ROUTES: Routes = [
  // 👇 Página de login por defecto
  { path: 'login', component: LoginComponent },

  // 👇 Página de registro
  { path: 'register', component: RegisterComponent },

  // 👇 Módulo paciente protegido
  {
    path: 'paciente',
    canActivate: [AuthGuard],
    children: PACIENTE_ROUTES
  },

  // 👇 Redirección por defecto al login
  { path: '', redirectTo: '/login', pathMatch: 'full' },

  // 👇 Redirección en caso de rutas no válidas
  { path: '**', redirectTo: '/login' }
];
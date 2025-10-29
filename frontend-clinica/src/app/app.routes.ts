import { Routes } from '@angular/router';
import { LoginComponent } from './shared/components/login/login.component';
import { Dashboard } from './shared/components/dashboard/dashboard';
import { AuthGuard } from './core/guards/auth.guard';
import { PACIENTES_ROUTES } from './features/pacientes/pacientes.routes';
import { MEDICOS_ROUTES } from './features/medicos/medicos.routes';
import { USUARIOS_ROUTES } from './features/usuarios/usuarios.routes';
import { CITA_ROUTES } from './features/citas/citas.routes';
import { RegisterComponent } from './shared/components/register/register.component';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'dashboard', component: Dashboard, canActivate: [AuthGuard] },
  { path: 'usuarios', canActivate: [AuthGuard], children: USUARIOS_ROUTES },
  { path: 'medicos', canActivate: [AuthGuard], children: MEDICOS_ROUTES },
  { path: 'pacientes', canActivate: [AuthGuard], children: PACIENTES_ROUTES },
  { path: 'citas', canActivate: [AuthGuard], children: CITA_ROUTES },
];

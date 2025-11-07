import { Routes } from '@angular/router';
import { LoginComponent } from './shared/components/login/login.component';
import { Dashboard } from './shared/components/dashboard/dashboard';
import { AuthGuard } from './core/guards/auth.guard';
import { PACIENTES_ROUTES } from './features/pacientes/pacientes.routes';
import { MEDICOS_ROUTES } from './features/medicos/medicos.routes';
import { USUARIOS_ROUTES } from './features/usuarios/usuarios.routes';
import { CITA_ROUTES } from './features/citas/citas.routes';
import { RegisterComponent } from './shared/components/register/register.component';
import { HORARIO_ROUTES } from './features/horarios/horario.routes';
import { MisCitasComponent } from './features/citas/mis-citas/mis-citas.component';
import { MedicosDisponiblesComponent } from './features/medicos/medicos-disponibles/medicos-disponibles.component';
import { HorarioMedicoComponent } from './features/medicos-horarios/horario-medico.component/horario-medico.component';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', component: LoginComponent },
  { path: 'register', component: RegisterComponent },
  { path: 'dashboard', component: Dashboard, canActivate: [AuthGuard] },
  { path: 'usuarios', canActivate: [AuthGuard], children: USUARIOS_ROUTES },
  { path: 'medicos', canActivate: [AuthGuard], children: MEDICOS_ROUTES },
  { path: 'pacientes', canActivate: [AuthGuard], children: PACIENTES_ROUTES },
  { path: 'citas', canActivate: [AuthGuard], children: CITA_ROUTES },
  { path: 'horarios', canActivate: [AuthGuard], children: HORARIO_ROUTES },

  { path: 'mis-citas', canActivate: [AuthGuard], loadComponent: () => import('./features/citas/cita-list.component').then(m => m.CitaListComponent) },
{ path: 'mis-horarios', canActivate: [AuthGuard], loadComponent: () => import('./features/horarios/horario-list.component').then(m => m.HorarioListComponent) },
{ path: 'mis-citas-paciente', component: MisCitasComponent, canActivate: [AuthGuard] },
{ path: 'medicos-disponibles', component: MedicosDisponiblesComponent },
{ path: 'horarios/medico/:id', component: HorarioMedicoComponent },


];

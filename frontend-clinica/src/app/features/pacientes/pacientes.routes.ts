import { Routes } from '@angular/router';
import { PacienteListComponent } from './paciente-list.component';
import { PacienteFormComponent } from './paciente-form.component';
import { AuthGuard } from '../../core/guards/auth.guard';
import { PacienteChatComponent } from './paciente-chat.component';

export const PACIENTES_ROUTES: Routes = [
  { path: '', component: PacienteListComponent, canActivate: [AuthGuard] },
  { path: 'nuevo', component: PacienteFormComponent, canActivate: [AuthGuard] },
  { path: 'editar/:id', component: PacienteFormComponent, canActivate: [AuthGuard] },
   { path: 'chat', component: PacienteChatComponent }
];

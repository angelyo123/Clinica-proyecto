import { Routes } from '@angular/router';
import { MedicoListComponent } from './medico-list.component';
import { MedicoFormComponent } from './medico-form.component';
import { AuthGuard } from '../../core/guards/auth.guard';

export const MEDICOS_ROUTES: Routes = [
  { path: '', component: MedicoListComponent, canActivate: [AuthGuard] },
  { path: 'nuevo', component: MedicoFormComponent, canActivate: [AuthGuard] },
  { path: 'editar/:id', component: MedicoFormComponent, canActivate: [AuthGuard] },
];

import { Routes } from '@angular/router';
import { MedicoListComponent } from './medico-list.component';
import { MedicoFormComponent } from './medico-form.component';

export const MEDICOS_ROUTES: Routes = [
  { path: '', component: MedicoListComponent },
  { path: 'nuevo', component: MedicoFormComponent },
  { path: 'editar/:id', component: MedicoFormComponent }
];
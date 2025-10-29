import { Routes } from '@angular/router';
import { HorarioListComponent } from './horario-list.component';
import { HorarioFormComponent } from './horario-form.component';

export const HORARIO_ROUTES: Routes = [
  { path: '', component: HorarioListComponent },
  { path: 'nuevo', component: HorarioFormComponent },
  { path: 'editar/:id', component: HorarioFormComponent }
];

import { Routes } from '@angular/router';
import { CitaListComponent } from './cita-list.component';
import { CitaFormComponent } from './cita-form.component';

export const CITA_ROUTES: Routes = [
  { path: '', component: CitaListComponent },
  { path: 'nueva', component: CitaFormComponent },
  { path: 'editar/:id', component: CitaFormComponent }
];

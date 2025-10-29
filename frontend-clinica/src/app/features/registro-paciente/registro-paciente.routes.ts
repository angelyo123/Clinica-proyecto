// En: features/registro-paciente/registro-paciente.routes.ts

import { Routes } from '@angular/router';
import { RegistroPacienteFormComponent } from './registro-paciente-form.component';

export const REGISTRO_PACIENTE_ROUTES: Routes = [
  {
    path: '', // Esto se cargará en la ruta /registro-paciente
    component: RegistroPacienteFormComponent
  }
];
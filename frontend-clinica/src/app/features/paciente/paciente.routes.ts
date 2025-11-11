import { Routes } from '@angular/router';
import { MisCitas } from './pages/mis-citas/mis-citas';
import { MedicosDisponibles } from './pages/medicos-disponibles/medicos-disponibles';
import { CrearCita } from './pages/crear-cita/crear-cita';
import { EditarCita} from './pages/editar-cita/editar-cita';
import { PerfilPaciente } from './pages/perfil-paciente/perfil-paciente';
import { ChatIaPaciente } from './chat-ia/chat-ia-paciente/chat-ia-paciente';
import { PacienteLayout } from './paciente-layout/paciente-layout';

export const PACIENTE_ROUTES: Routes = [
  {
    path: '',
    component: PacienteLayout,
    children: [
      { path: 'perfil', component: PerfilPaciente },
      { path: 'mis-citas', component: MisCitas },
      { path: 'medicos-disponibles', component: MedicosDisponibles },
      { path: 'crear-cita', component: CrearCita },
      { path: 'editar-cita/:id', component: EditarCita },
      { path: 'chat-ia', component: ChatIaPaciente },
      { path: '', redirectTo: 'perfil', pathMatch: 'full' } // 👈 perfil será la página inicial
    ]
  }
];
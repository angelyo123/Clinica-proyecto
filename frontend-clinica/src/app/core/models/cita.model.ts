export interface Cita {
  id?: number;
  fechaCita: string | Date;
  estado: string;

  medico?: {
    id: number;
    nombre?: string;
    especialidad?: string;
    telefono?: string;
    dni?: string; // ✅ agregado
  };

  paciente?: {
    id: number;
    nombre?: string;
    telefono?: string;
    dni?: string; // ✅ agregado
  };

  idHorario?: number;
}

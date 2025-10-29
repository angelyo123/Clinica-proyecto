export interface Cita {
  id?: number;
  fechaHora: string; // formato ISO o HH:mm
  estado: string;
  medico: any;
  paciente?: any;
}

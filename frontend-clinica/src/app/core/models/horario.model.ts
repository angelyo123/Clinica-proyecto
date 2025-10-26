export interface Horario {
  id?: number;
  medicoId: number;
  diaSemana: string; // LUNES, MARTES, etc.
  horaInicio: string; // formato "HH:mm"
  horaFin: string;    // formato "HH:mm"
  disponible: boolean;
}

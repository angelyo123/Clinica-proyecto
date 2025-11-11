export interface Horario {
  id: number;
  medicoId: number;
  diaSemana: string;
  horaInicio: string;
  horaFin: string;
  fechaInicio: string; // LocalDate del backend
  fechaFin: string;
  disponible: boolean;
  pacientesPorHora: number;
}
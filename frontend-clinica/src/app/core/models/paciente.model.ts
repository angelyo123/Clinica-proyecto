import { Usuario } from "./usuario.model";

export interface Paciente {
  id?: number;
  nombre: string;
  dni: string;
  telefono: string;
  usuario?: Usuario;
}

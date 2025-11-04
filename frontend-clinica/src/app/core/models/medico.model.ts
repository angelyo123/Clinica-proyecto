/*private int id;

    private String nombre;
    private String especialidad;
    private String telefono;*/

import { Usuario } from "./usuario.model";

    export interface Medico{

        id?: number;
        nombre: string;
        especialidad: string;
        telefono: string;
        dni: string;
        usuario?: string;
        
    }
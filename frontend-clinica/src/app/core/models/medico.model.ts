/*private int id;

    private String nombre;
    private String especialidad;
    private String telefono;*/

import { Usuario } from "./usuario.model";

    export interface Medico{

        id?:number;
        nombre:String;
        especialidad: String;
        dni: String;
        telefono:string;
        usuario?:Usuario;
    }
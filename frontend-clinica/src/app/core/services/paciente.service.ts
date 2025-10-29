import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Paciente } from '../models/paciente.model';

@Injectable({ providedIn: 'root' })
export class PacienteService {
    
    private readonly apiUrl= 'http://localhost:8082/paciente'

    constructor(private http: HttpClient){}

       private getAuthHeaders(): HttpHeaders {
        const token = localStorage.getItem('token');
        return new HttpHeaders({
            'Authorization': `Bearer ${token}`,
            'Content-Type': 'application/json'
        });
    }

    
    listar():Observable<Paciente[]>{
        return this.http.get<Paciente[]>(`${this.apiUrl}/listar`);
    }

    obtener(id:number):Observable<Paciente>{
        return this.http.get<Paciente>(`${this.apiUrl}/obtener/${id}`)
    }

    crear(paciente: Paciente):Observable<Paciente>{
        return this.http.post<Paciente>(`${this.apiUrl}/crear`, paciente);
    }

    actualizar(id:number,paciente: Paciente):Observable<Paciente>{
        return this.http.put<Paciente>(`${this.apiUrl}/actualizar/${id}`, paciente);
    }

    eliminar(id: number): Observable<void> {
  return this.http.delete<void>(`${this.apiUrl}/eliminar/${id}`, { 
    headers: this.getAuthHeaders()  // ✅ Envía token al PacienteService
  });
}
       obtenerPerfil(): Observable<Paciente> {
        return this.http.get<Paciente>(`${this.apiUrl}/perfil`, { 
            headers: this.getAuthHeaders() 
        });
    }
  }


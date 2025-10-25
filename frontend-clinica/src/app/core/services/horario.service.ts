import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Horario } from '../../core/models/horario.model';

@Injectable({
  providedIn: 'root'
})
export class HorarioService {
  private apiUrl = 'http://localhost:8085/horarios'; // ⚡ Ajusta según tu backend

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  listar(): Observable<Horario[]> {
    return this.http.get<Horario[]>(this.apiUrl, { headers: this.getAuthHeaders() });
  }

  obtener(id: number): Observable<Horario> {
    return this.http.get<Horario>(`${this.apiUrl}/${id}`, { headers: this.getAuthHeaders() });
  }

  crear(horario: Horario): Observable<Horario> {
    return this.http.post<Horario>(this.apiUrl, horario, { headers: this.getAuthHeaders() });
  }

  actualizar(id: number, horario: Horario): Observable<Horario> {
    return this.http.put<Horario>(`${this.apiUrl}/${id}`, horario, { headers: this.getAuthHeaders() });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { headers: this.getAuthHeaders() });
  }

  listarPorMedico(medicoId: number): Observable<Horario[]> {
    return this.http.get<Horario[]>(`${this.apiUrl}/medico/${medicoId}`, { headers: this.getAuthHeaders() });
  }
}

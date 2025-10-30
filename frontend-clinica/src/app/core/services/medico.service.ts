import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Medico } from '../models/medico.model';

@Injectable({
  providedIn: 'root'
})
export class MedicoService {

  private readonly apiUrl = 'http://localhost:8083/medico';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  listar(): Observable<Medico[]> {
    return this.http.get<Medico[]>(`${this.apiUrl}/listar`, {
      headers: this.getAuthHeaders()
    });
  }

  obtener(id: number): Observable<Medico> {
    return this.http.get<Medico>(`${this.apiUrl}/obtener/${id}`, {
      headers: this.getAuthHeaders()
    });
  }

  crear(medico: Medico): Observable<Medico> {
    return this.http.post<Medico>(`${this.apiUrl}/crear`, medico, {
      headers: this.getAuthHeaders()
    });
  }

  actualizar(id: number, medico: Medico): Observable<Medico> {
    return this.http.put<Medico>(`${this.apiUrl}/actualizar/${id}`, medico, {
      headers: this.getAuthHeaders()
    });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/eliminar/${id}`, {
      headers: this.getAuthHeaders()
    });
  }

  obtenerPerfil(): Observable<Medico> {
    return this.http.get<Medico>(`${this.apiUrl}/perfil`, {
      headers: this.getAuthHeaders()
    });
  }

  listarPublico(): Observable<Medico[]> {
    return this.http.get<Medico[]>(`${this.apiUrl}/public/listar`);
  }
}
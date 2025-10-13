import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Cita } from '../../core/models/cita.model';

@Injectable({
  providedIn: 'root'
})
export class CitaService {
  private apiUrl = 'http://localhost:8080/cita'; // ✅ correcto

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  listar(): Observable<Cita[]> {
    return this.http.get<Cita[]>(this.apiUrl, { headers: this.getAuthHeaders() });
  }

  obtener(id: number): Observable<Cita> {
    return this.http.get<Cita>(`${this.apiUrl}/${id}`, { headers: this.getAuthHeaders() });
  }

  crearCita(cita: Cita): Observable<Cita> {
    // 👇 ya no duplicas /user/citas
    return this.http.post<Cita>(this.apiUrl, cita, { headers: this.getAuthHeaders() });
  }

  actualizar(id: number, cita: Cita): Observable<Cita> {
    return this.http.put<Cita>(`${this.apiUrl}/${id}`, cita, { headers: this.getAuthHeaders() });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { headers: this.getAuthHeaders() });
  }
}

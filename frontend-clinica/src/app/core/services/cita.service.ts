import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Cita } from '../../core/models/cita.model';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class CitaService {
  private apiUrl = 'http://localhost:8084/cita'; // ✅ correcto

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    });
  }

  listar(): Observable<Cita[]> {
    return this.http.get<Cita[]>(`${this.apiUrl}/listar`, { headers: this.getAuthHeaders() });
  }
actualizarEstado(id: number, estado: string): Observable<Cita> {
  const body = { nuevoEstado: estado };
  return this.http.put<Cita>(`${this.apiUrl}/actualizarEstado/${id}`, body, {
    headers: this.getAuthHeaders()
  });
}

  listarDetalles(): Observable<Cita[]> {
    // Llama al nuevo endpoint que creaste en el backend
    return this.http.get<Cita[]>(`${this.apiUrl}/listar/detalles`, { headers: this.getAuthHeaders() });
  }


listarDetallesPorPaciente(pacienteId: number): Observable<Cita[]> {
  const params = new HttpParams().set('pacienteId', pacienteId.toString());

  return this.http.get<Cita[]>(`${this.apiUrl}/listarPorPaciente/detalles`, {
    params,
    headers: this.getAuthHeaders()
  }).pipe(
    // 👇 Convertimos fechaCita a objeto Date para el pipe date
    map((citas: Cita[]) =>
      citas.map(c => ({
        ...c,
        fechaCita: new Date(c.fechaCita)
      }))
    )
  );
}


  // --- NUEVO MÉTODO PARA MÉDICO ---
  // Cambia 'any[]' por 'CitaMedicoDTO[]' si tienes ese modelo
  listarDetallesPorMedico(medicoId: number): Observable<any[]> {
    const params = new HttpParams().set('medicoId', medicoId.toString());
    // Asegúrate que el tipo de retorno <any[]> coincida con lo que devuelve el backend (CitaMedicoDTO)
    return this.http.get<any[]>(`${this.apiUrl}/listarPorMedico/detalles`, { params });
  }
  obtener(id: number): Observable<Cita> {
    return this.http.get<Cita>(`${this.apiUrl}/detalle/${id}`, { headers: this.getAuthHeaders() });
  }

  crearCita(cita: Cita): Observable<Cita> {
    // 👇 ya no duplicas /user/citas
    return this.http.post<Cita>(`${this.apiUrl}/crear`, cita, { headers: this.getAuthHeaders() });
  }

  actualizar(id: number, cita: Cita): Observable<Cita> {
    return this.http.put<Cita>(`${this.apiUrl}/actualizar/detalle/${id}`, cita, { headers: this.getAuthHeaders() });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/eliminar/${id}`, { headers: this.getAuthHeaders() });
  }
  
}

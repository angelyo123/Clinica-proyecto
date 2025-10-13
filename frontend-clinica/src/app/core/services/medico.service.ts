import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { Medico } from '../models/medico.model';

@Injectable({
  providedIn: 'root'
})
export class MedicoService {
  private apiUrl = 'http://localhost:8080/admin/medico';

  constructor(private http:HttpClient){
  }

  listar():Observable<Medico[]>{
    return this.http.get<Medico[]>(`${this.apiUrl}/listar`);
  }

  crear(medico:Medico):Observable<Medico>{
    return this.http.post<Medico>(`${this.apiUrl}/crear`, medico);
  }

  obtener(id:number):Observable<Medico>{
    return this.http.get<Medico>(`${this.apiUrl}/${id}`)
  }

  actualizar(id: number, medico: Medico): Observable<Medico> {
    return this.http.put<Medico>(`${this.apiUrl}/actualizar/${id}`, medico);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/eliminar/${id}`);
  }


  listarAdmin() {
  return this.http.get<Medico[]>('http://localhost:8080/admin/medicos/listar');
}

listarPublico() {
  return this.http.get<Medico[]>('http://localhost:8080/paciente/medicos');
}


}

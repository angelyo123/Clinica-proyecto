import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';
import { Medico } from '../models/medico.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly apiUrl = 'http://localhost:8081/auth';

  constructor(private http: HttpClient, private router: Router) {}

  login(username: string, password: string): Observable<{ token: string }> {
    return this.http.post<{ token: string }>(`${this.apiUrl}/login`, { username, password });
  }

  registerPaciente(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register/paciente`, data);
  }

  registerMedico(data: any): Observable<any> {
    return this.http.post(`${this.apiUrl}/register/medico`, data);
  }

  saveToken(token: string): void {
    localStorage.setItem('token', token);
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  logout(): void {
    localStorage.removeItem('token');
    this.router.navigate(['/login']);
  }

  isAuthenticated(): boolean {
    return !!this.getToken();
  }
 getUsername(): string | null {
    const token = this.getToken();
    if (!token) return null;

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.sub;
    } catch {
      return null;
    }
  }
  getUserRoles(): string[] {
    const token = this.getToken();
    if (!token) return [];

    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      return payload.roles || [];
    } catch {
      return [];
    }
  }

  getUserId(): number | null {
  const token = this.getToken();
  if (!token) return null;

  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    // normalmente el ID del usuario se guarda en "id" o "userId" dentro del payload JWT
    return payload.id || payload.userId || null;
  } catch (error) {
    console.error('Error al decodificar token:', error);
    return null;
  }
}


  isAdmin(): boolean {
  return this.getUserRoles().includes('ROLE_ADMIN');
}

isPaciente(): boolean {
  return this.getUserRoles().includes('ROLE_PACIENTE');
}

isMedico(): boolean {
    return this.getUserRoles().includes('ROLE_MEDICO');
  }

obtenerPerfilMedico(): Observable<Medico> {
  const token = localStorage.getItem('token');
  return this.http.get<Medico>('http://localhost:8083/medico/perfil', {
    headers: new HttpHeaders({
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    })
  });
}



    
}

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable } from 'rxjs';

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

  isAdmin(): boolean {
  return this.getUserRoles().includes('ROLE_ADMIN');
}

isPaciente(): boolean {
  return this.getUserRoles().includes('ROLE_PACIENTE');
}

isMedico(): boolean {
    return this.getUserRoles().includes('ROLE_MEDICO');
  }


    
}

/*
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, of, tap, map, switchMap, catchError, BehaviorSubject, firstValueFrom } from 'rxjs';

// --- INTERFACES ACTUALIZADAS ---
// Basado en tu Paciente.java
interface PacienteProfile {
  id: number;           // Obligatorio, es el pacienteId
  nombre?: string;
  dni?: string;
  telefono?: string;
  usuario?: string;     // username asociado
}

// Basado en tu Medico.java
interface MedicoProfile {
  id: number;           // Obligatorio, es el medicoId
  nombre?: string;
  especialidad?: string;
  telefono?: string;
  dni?: string;
  usuario?: string;     // username asociado
}
// --- FIN INTERFACES ---

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly authApiUrl = 'http://localhost:8081/auth';
  private readonly pacienteApiUrl = 'http://localhost:8082/paciente';
  private readonly medicoApiUrl = 'http://localhost:8083/medico'; // Ajusta puerto si es necesario

  private isAuthenticatedSubject = new BehaviorSubject<boolean>(this.hasToken());
  public isAuthenticated$ = this.isAuthenticatedSubject.asObservable();

  private userRoles: string[] = [];
  private userId: number | null = null; // ID general del Usuario (login)
  private specificId: number | null = null; // ID específico (Paciente o Médico)

  constructor(private http: HttpClient, private router: Router) {
    if (this.hasToken()) {
      console.log("AuthService Init: Token found, loading user data...");
      this.loadUserFromToken();
      if (this.specificId === null && (this.isPaciente() || this.isMedico())) {
        console.log("AuthService Init: Fetching specific ID on startup...");
        firstValueFrom(this.fetchAndStoreSpecificId())
          .then(id => console.log('Specific ID loaded on init:', id))
          .catch(err => console.error('Failed to load specific ID on init:', err))
          .finally(() => this.isAuthenticatedSubject.next(true));
      } else {
        this.isAuthenticatedSubject.next(true);
      }
    } else {
      console.log("AuthService Init: No token found.");
    }
  }

  private hasToken(): boolean {
    return !!localStorage.getItem('token');
  }

  saveToken(token: string): void {
    localStorage.setItem('token', token);
    this.loadUserFromToken();
    // No notificamos isAuthenticatedSubject aquí, se hace post-fetch
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  login(username: string, password: string): Observable<{ token: string }> {
    return this.http.post<{ token: string }>(`${this.authApiUrl}/login`, { username, password })
      .pipe(
        tap(response => {
          this.saveToken(response.token);
          console.log("Login successful. Token saved. Roles:", this.userRoles, "UserID:", this.userId);
        }),
        switchMap(response =>
          this.fetchAndStoreSpecificId().pipe(
            tap(id => {
              console.log("Specific ID fetched after login:", id);
              this.isAuthenticatedSubject.next(true); // Notifica auth DESPUÉS de obtener ID
            }),
            map(() => response),
            catchError(err => {
              console.error("Error fetching specific ID after login, continuing anyway:", err);
              this.isAuthenticatedSubject.next(true); // Aún autenticado
              return of(response);
            })
          )
        )
      );
  }

  private loadUserFromToken(): void {
    const token = this.getToken();
    if (!token) {
      this.clearLocalData(false);
      return;
    }
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      this.userRoles = payload.roles || [];
      const userIdFromToken = payload.userId; // Asume que el backend añade 'userId'
      this.userId = userIdFromToken ? Number(userIdFromToken) : null;
      this.specificId = null; // Limpia ID específico, se recargará
      console.log("Loaded from token:", { roles: this.userRoles, userId: this.userId });
    } catch (e) {
      console.error('Error loading user data from token:', e);
      this.clearLocalData(false);
    }
  }

  fetchAndStoreSpecificId(): Observable<number | null> {
    let profileObservable: Observable<PacienteProfile | MedicoProfile> | null = null; // Usa las interfaces
    let profileType = '';

    if (this.isPaciente()) {
      console.log("Fetching paciente profile...");
      profileType = 'Paciente';
      // Llama a /paciente/perfil y espera la estructura PacienteProfile
      profileObservable = this.http.get<PacienteProfile>(`${this.pacienteApiUrl}/perfil`);
    } else if (this.isMedico()) {
      console.log("Fetching medico profile...");
      profileType = 'Medico';
      // Llama a /medico/perfil y espera la estructura MedicoProfile
      profileObservable = this.http.get<MedicoProfile>(`${this.medicoApiUrl}/perfil`);
    } else {
      console.log("User is admin or other role, no specific ID needed.");
      this.specificId = null;
      return of(null);
    }

    return profileObservable.pipe(
      map(profile => profile.id), // Extrae solo el ID
      tap(id => {
        this.specificId = id;
        console.log(`${profileType} Specific ID stored:`, this.specificId);
      }),
      catchError(err => {
        console.error(`Error fetching ${profileType} profile, specific ID could not be set:`, err);
        this.specificId = null;
        return of(null);
      })
    );
  }

  private clearLocalData(navigate = true): void {
    localStorage.removeItem('token');
    this.userRoles = [];
    this.userId = null;
    this.specificId = null;
    if (this.isAuthenticatedSubject.value) {
      this.isAuthenticatedSubject.next(false);
    }
    if (navigate) {
      this.router.navigate(['/login']);
    }
  }

  logout(): void {
    console.log("Logging out...");
    this.clearLocalData(true);
  }

  isAuthenticated(): boolean {
    return this.isAuthenticatedSubject.value;
  }

  getUserRoles(): string[] { return this.userRoles; }
  isAdmin(): boolean { return this.userRoles.includes('ROLE_ADMIN'); }
  isMedico(): boolean { return this.userRoles.includes('ROLE_MEDICO'); }
  isPaciente(): boolean { return this.userRoles.includes('ROLE_PACIENTE'); }

  getCurrentUserId(): number | null { return this.userId; }
  getCurrentSpecificId(): number | null { return this.specificId; }

  registerPaciente(data: any): Observable<any> {
    return this.http.post(`${this.authApiUrl}/register/paciente`, data);
  }

  registerMedico(data: any): Observable<any> {
    return this.http.post(`${this.authApiUrl}/register/medico`, data);
  }
}
*/


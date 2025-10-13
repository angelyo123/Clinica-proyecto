import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MedicoService } from '../../core/services/medico.service';
import { Medico } from '../../core/models/medico.model';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-medico-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <h2>Médicos</h2>
    
    <!-- Mostrar botón solo si el usuario es ADMIN -->
    <button *ngIf="authService.isAdmin()" (click)="nuevoMedico()">+ Nuevo Médico</button>

    <p *ngIf="error" class="text-danger">{{ error }}</p>

    <ul>
      <li *ngFor="let medico of medicos">
        {{ medico.nombre }} - {{ medico.especialidad }}
        <!-- Mostrar botón para agendar cita solo si el usuario es PACIENTE -->
        <button *ngIf="authService.isPaciente()" (click)="agendarCita(medico)">Agendar cita</button>
      </li>
    </ul>
  `
})
export class MedicoListComponent implements OnInit {
  medicos: Medico[] = [];
  loading = false;
  error = '';

  // ✅ Inyecta los servicios correctamente
  constructor(
    private medicoService: MedicoService,
    public authService: AuthService,
    private router: Router
  ) {}

  ngOnInit() {
    this.cargarMedicos();
  }

  cargarMedicos() {
    this.loading = true;
    const roles = this.authService.getUserRoles();

    if (roles.includes('ROLE_ADMIN')) {
      this.medicoService.listarAdmin().subscribe({
        next: (data) => {
          this.medicos = data;
          this.loading = false;
        },
        error: () => {
          this.error = 'Error al cargar médicos (admin)';
          this.loading = false;
        }
      });
    } else if (roles.includes('ROLE_PACIENTE')) {
      this.medicoService.listarPublico().subscribe({
        next: (data) => {
          this.medicos = data;
          this.loading = false;
        },
        error: () => {
          this.error = 'Error al cargar médicos (paciente)';
          this.loading = false;
        }
      });
    }
  }

  nuevoMedico() {
    this.router.navigate(['/medicos/nuevo']);
  }

  agendarCita(medico: Medico) {
    this.router.navigate(['/citas/nueva'], { queryParams: { medicoId: medico.id } });
  }
}

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CitaService } from '../../core/services/cita.service';
import { Cita } from '../../core/models/cita.model';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-cita-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <h2>Listado de Citas</h2>

    <!-- Solo ADMIN puede crear citas manualmente -->
    <button *ngIf="esAdmin" routerLink="/citas/nueva">Nueva Cita</button>

    <table border="1" style="margin-top: 10px; border-collapse: collapse; width: 100%;">
      <thead>
        <tr>
          <th>ID</th>
          <th>Fecha y Hora</th>
          <th>Estado</th>
          <th>Médico</th>
          <th *ngIf="esAdmin">Paciente</th>
          <th *ngIf="esAdmin">Acciones</th>
        </tr>
      </thead>
      <tbody>
        <tr *ngFor="let c of citas">
          <td>{{ c.id }}</td>
          <td>{{ c.fechaHora | date: 'short' }}</td>
          <td>{{ c.estado }}</td>

          <!-- Mostrar nombre del médico -->
          <td>
            {{ c.medico?.nombre || '—' }}
            <span *ngIf="c.medico?.especialidad">
              ({{ c.medico.especialidad }})
            </span>
          </td>

          <!-- Mostrar nombre del paciente solo para admin -->
          <td *ngIf="esAdmin">
            {{ c.paciente?.nombre || '—' }}
          </td>

          <!-- Botones solo para admin -->
          <td *ngIf="esAdmin">
            <button [routerLink]="['/citas/editar', c.id]">Editar</button>
            <button (click)="eliminar(c.id!)">Eliminar</button>
          </td>
        </tr>
      </tbody>
    </table>

    <p *ngIf="!citas.length">No hay citas registradas.</p>
  `
})
export class CitaListComponent implements OnInit {
  citas: Cita[] = [];
  esAdmin = false;
  esPaciente = false;

  constructor(
    private citaService: CitaService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.detectarRol();
    this.cargarCitas();
  }

  detectarRol(): void {
    this.esAdmin = this.authService.isAdmin();
    this.esPaciente = this.authService.isPaciente();
  }

  cargarCitas(): void {
    this.citaService.listar().subscribe({
      next: (data) => (this.citas = data),
      error: (err) => console.error('Error al listar citas:', err)
    });
  }

  eliminar(id: number): void {
    if (confirm('¿Estás seguro de eliminar esta cita?')) {
      this.citaService.eliminar(id).subscribe({
        next: () => {
          alert('Cita eliminada correctamente');
          this.citas = this.citas.filter((c) => c.id !== id);
        },
        error: (err) => console.error('Error al eliminar cita:', err)
      });
    }
  }
}

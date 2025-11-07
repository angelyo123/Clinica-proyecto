import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CitaService } from '../../../core/services/cita.service';
import { PacienteService } from '../../../core/services/paciente.service';
import { AuthService } from '../../../core/services/auth.service';
import { Cita } from '../../../core/models/cita.model';

@Component({
  selector: 'app-mis-citas',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="max-w-6xl mx-auto px-8 py-6">
      <!-- Encabezado -->
      <div class="flex justify-between items-center mb-8">
        <h2 class="text-3xl font-bold text-gray-800 tracking-tight">
          🩺 Mis Citas
        </h2>

        <div class="flex gap-3">
          <button
            routerLink="/medicos-disponibles"
            class="bg-blue-600 hover:bg-blue-700 text-white font-medium px-4 py-2 rounded-md shadow-sm transition">
            ➕ Reservar cita
          </button>

          <button
            routerLink="/pacientes/chat"
            class="bg-green-600 hover:bg-green-700 text-white font-medium px-4 py-2 rounded-md shadow-sm transition">
            💬 Chat con IA
          </button>

          <button
            (click)="cerrarSesion()"
            class="bg-red-600 hover:bg-red-700 text-white font-medium px-4 py-2 rounded-md shadow-sm transition">
            🔒 Cerrar sesión
          </button>
        </div>
      </div>

      <!-- Estado de carga -->
      <div *ngIf="cargando" class="text-center py-10">
        <div class="inline-block w-10 h-10 border-4 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
        <p class="text-gray-500 mt-3">Cargando citas...</p>
      </div>

      <!-- Sin citas -->
      <div *ngIf="!cargando && !citas.length" class="text-center text-gray-500 py-16">
        <p class="text-lg mb-4">No tienes citas registradas.</p>
        <button
          routerLink="/medicos-disponibles"
          class="bg-blue-600 hover:bg-blue-700 text-white px-6 py-2 rounded-md shadow-sm transition">
          🩺 Reservar nueva cita
        </button>
      </div>

      <!-- Tabla de citas -->
      <div *ngIf="!cargando && citas.length"
           class="bg-white rounded-lg shadow-md border border-gray-200 overflow-hidden">
        <table class="min-w-full text-sm text-gray-700">
          <thead class="bg-gray-100 border-b border-gray-300">
            <tr>
              <th class="px-5 py-3 text-left font-semibold">Fecha</th>
              <th class="px-5 py-3 text-left font-semibold">Hora</th>
              <th class="px-5 py-3 text-left font-semibold">Médico</th>
              <th class="px-5 py-3 text-left font-semibold">Especialidad</th>
              <th class="px-5 py-3 text-left font-semibold">Estado</th>
              <th class="px-5 py-3 text-center font-semibold">Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let c of citas" class="border-b hover:bg-gray-50 transition">
              <td class="px-5 py-3 whitespace-nowrap">{{ c.fechaCita | date: 'dd/MM/yyyy' }}</td>
              <td class="px-5 py-3 whitespace-nowrap">{{ c.fechaCita | date: 'HH:mm' }}</td>
              <td class="px-5 py-3 whitespace-nowrap font-medium text-gray-800">
                {{ c.medico?.nombre || '—' }}
              </td>
              <td class="px-5 py-3 whitespace-nowrap">{{ c.medico?.especialidad || '—' }}</td>
              <td class="px-5 py-3 whitespace-nowrap">
                <span [ngClass]="getEstadoClass(c.estado)"
                      class="px-3 py-1 rounded-full text-xs font-semibold uppercase">
                  {{ c.estado }}
                </span>
              </td>
              <td class="px-5 py-3 text-center">
                <button
                  *ngIf="c.estado === 'PENDIENTE'"
                  (click)="cancelarCita(c.id!)"
                  class="bg-red-500 hover:bg-red-600 text-white px-3 py-1.5 rounded-md text-xs transition">
                  ❌ Cancelar
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `
})
export class MisCitasComponent implements OnInit {
  citas: Cita[] = [];
  cargando = true;

  constructor(
    private citaService: CitaService,
    private pacienteService: PacienteService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.cargarCitas();
  }

  cargarCitas(): void {
    this.cargando = true;
    this.pacienteService.obtenerPerfil().subscribe({
      next: paciente => {
        if (paciente.id) {
          this.citaService.listarDetallesPorPaciente(paciente.id).subscribe({
            next: data => {
              this.citas = data;
              this.cargando = false;
            },
            error: err => {
              console.error('Error al listar citas:', err);
              this.cargando = false;
            }
          });
        } else {
          this.cargando = false;
        }
      },
      error: err => {
        console.error('Error al obtener perfil del paciente:', err);
        this.cargando = false;
      }
    });
  }

  cancelarCita(id: number): void {
    if (confirm('¿Deseas cancelar esta cita?')) {
      this.citaService.actualizarEstado(id, 'CANCELADA').subscribe({
        next: () => {
          alert('Cita cancelada correctamente');
          this.cargarCitas();
        },
        error: err => console.error('Error al cancelar cita:', err)
      });
    }
  }

  cerrarSesion(): void {
    if (confirm('¿Seguro que deseas cerrar sesión?')) {
      this.authService.logout();
    }
  }

  getEstadoClass(estado: string): string {
    switch ((estado || '').toUpperCase()) {
      case 'PENDIENTE': return 'bg-yellow-100 text-yellow-800 border border-yellow-300';
      case 'CONFIRMADA': return 'bg-green-100 text-green-800 border border-green-300';
      case 'CANCELADA': return 'bg-red-100 text-red-800 border border-red-300';
      case 'COMPLETADA': return 'bg-blue-100 text-blue-800 border border-blue-300';
      default: return 'bg-gray-100 text-gray-700 border border-gray-300';
    }
  }
}

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CitaService } from '../../../core/services/cita.service';
import { PacienteService } from '../../../core/services/paciente.service';
import { Cita } from '../../../core/models/cita.model';
import { AuthService } from '../../../core/services/auth.service'; // 👈 Importamos AuthService

@Component({
  selector: 'app-mis-citas',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="max-w-5xl mx-auto p-6">
      <div class="flex justify-between items-center mb-4">
        <h2 class="text-2xl font-bold text-gray-800">🩺 Mis Citas</h2>

        <div class="flex gap-3">
          <button
            class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg shadow transition"
            routerLink="/medicos-disponibles">
            ➕ Reservar nueva cita
          </button>

          <button
            class="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded-lg shadow transition"
            routerLink="/pacientes/chat">
            💬 Chat con IA
          </button>

          <!-- 🔒 Botón de cerrar sesión -->
          <button
            class="bg-red-600 hover:bg-red-700 text-white px-4 py-2 rounded-lg shadow transition"
            (click)="cerrarSesion()">
            🔓 Cerrar sesión
          </button>
        </div>
      </div>

      <div *ngIf="!citas.length" class="text-center text-gray-500">
        No tienes citas registradas.
      </div>

      <div class="overflow-x-auto bg-white shadow-md rounded-lg" *ngIf="citas.length">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-100">
            <tr>
              <th class="px-4 py-2 text-left text-sm font-semibold text-gray-600">Fecha</th>
              <th class="px-4 py-2 text-left text-sm font-semibold text-gray-600">Hora</th>
              <th class="px-4 py-2 text-left text-sm font-semibold text-gray-600">Médico</th>
              <th class="px-4 py-2 text-left text-sm font-semibold text-gray-600">Estado</th>
              <th class="px-4 py-2 text-center text-sm font-semibold text-gray-600">Acción</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr *ngFor="let c of citas" class="hover:bg-gray-50">
              <td class="px-4 py-2">{{ c.fechaHora | date: 'dd/MM/yyyy' }}</td>
              <td class="px-4 py-2">{{ c.fechaHora | date: 'HH:mm' }}</td>
              <td class="px-4 py-2">{{ c.medico?.nombre }} ({{ c.medico?.especialidad }})</td>
              <td class="px-4 py-2">
                <span [ngClass]="getEstadoClass(c.estado)"
                      class="px-3 py-1 rounded-full text-xs font-medium">
                  {{ c.estado }}
                </span>
              </td>
              <td class="px-4 py-2 text-center">
                <button *ngIf="c.estado === 'PENDIENTE'"
                        (click)="cancelarCita(c.id!)"
                        class="bg-red-500 hover:bg-red-600 text-white px-3 py-1 rounded-md text-sm transition">
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

  constructor(
    private citaService: CitaService,
    private pacienteService: PacienteService,
    private authService: AuthService // 👈 Inyectamos AuthService
  ) {}

  ngOnInit(): void {
    this.cargarCitas();
  }

  cargarCitas(): void {
    this.pacienteService.obtenerPerfil().subscribe({
      next: paciente => {
        if (paciente.id) {
          this.citaService.listarDetallesPorPaciente(paciente.id).subscribe({
            next: data => this.citas = data,
            error: err => console.error('Error al listar citas:', err)
          });
        }
      },
      error: err => console.error('Error al obtener perfil del paciente:', err)
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

  // ✅ Método para cerrar sesión
  cerrarSesion(): void {
    if (confirm('¿Seguro que deseas cerrar sesión?')) {
      this.authService.logout();
    }
  }

  getEstadoClass(estado: string): string {
    switch ((estado || '').toUpperCase()) {
      case 'PENDIENTE': return 'bg-yellow-100 text-yellow-800';
      case 'CONFIRMADA': return 'bg-green-100 text-green-800';
      case 'CANCELADA': return 'bg-red-100 text-red-800';
      case 'COMPLETADA': return 'bg-blue-100 text-blue-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }
}

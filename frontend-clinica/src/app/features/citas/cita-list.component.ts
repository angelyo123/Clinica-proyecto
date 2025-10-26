import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { CitaService } from '../../core/services/cita.service';
import { PacienteService } from '../../core/services/paciente.service';
import { MedicoService } from '../../core/services/medico.service';
import { Cita } from '../../core/models/cita.model';
import { Paciente } from '../../core/models/paciente.model';
import { Medico } from '../../core/models/medico.model';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-cita-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="container mx-auto px-4 py-8 max-w-7xl">
      <!-- Header -->
      <div class="flex justify-between items-center mb-6">
        <h2 class="text-3xl font-bold text-gray-800">📋 Listado de Citas</h2>
        
        <!-- Botón Nueva Cita (solo admin) -->
        <button 
          *ngIf="esAdmin" 
          routerLink="/citas/nueva"
          class="bg-green-500 hover:bg-green-600 text-white font-semibold px-6 py-2.5 rounded-lg shadow-md transition duration-300 ease-in-out transform hover:scale-105">
          ➕ Nueva Cita
        </button>
      </div>

      <!-- Info del usuario logueado (Paciente) -->
      <div *ngIf="pacienteLogueado" 
           class="bg-green-50 border-l-4 border-green-500 p-4 mb-6 rounded-lg shadow-sm">
        <div class="flex items-center">
          <span class="text-2xl mr-3">👤</span>
          <div>
            <p class="text-sm text-gray-600 font-medium">Paciente</p>
            <p class="text-lg font-bold text-gray-800">{{ pacienteLogueado.nombre }}</p>
            <p class="text-sm text-gray-500">DNI: {{ pacienteLogueado.dni }}</p>
          </div>
        </div>
      </div>

      <!-- Info del usuario logueado (Médico) -->
      <div *ngIf="medicoLogueado" 
           class="bg-blue-50 border-l-4 border-blue-500 p-4 mb-6 rounded-lg shadow-sm">
        <div class="flex items-center">
          <span class="text-2xl mr-3">👨‍⚕️</span>
          <div>
            <p class="text-sm text-gray-600 font-medium">Médico</p>
            <p class="text-lg font-bold text-gray-800">{{ medicoLogueado.nombre }}</p>
            <p class="text-sm text-gray-500">{{ medicoLogueado.especialidad }}</p>
          </div>
        </div>
      </div>

      <!-- Tabla de citas -->
      <div class="bg-white rounded-lg shadow-lg overflow-hidden">
        <div class="overflow-x-auto">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  ID
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Fecha y Hora
                </th>
                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Estado
                </th>
                
                <!-- Columna Médico (solo para paciente y admin) -->
                <th *ngIf="esPaciente || esAdmin" 
                    class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Médico
                </th>
                
                <!-- Columna Paciente (solo para médico y admin) -->
                <th *ngIf="esMedico || esAdmin" 
                    class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Paciente
                </th>
                
                <th *ngIf="esAdmin" 
                    class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Acciones
                </th>
              </tr>
            </thead>
            <tbody class="bg-white divide-y divide-gray-200">
              <tr *ngFor="let c of citas" class="hover:bg-gray-50 transition duration-150">
                <!-- ID -->
                <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                  #{{ c.id }}
                </td>

                <!-- Fecha y Hora -->
                <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                  <div class="flex flex-col">
                    <span class="font-medium">{{ c.fechaHora | date: 'dd/MM/yyyy' }}</span>
                    <span class="text-xs text-gray-500">{{ c.fechaHora | date: 'HH:mm' }}</span>
                  </div>
                </td>

                <!-- Estado -->
                <td class="px-6 py-4 whitespace-nowrap">
                  <span [ngClass]="getEstadoClasses(c.estado)" 
                        class="px-3 py-1 inline-flex text-xs leading-5 font-semibold rounded-full">
                    {{ c.estado }}
                  </span>
                </td>

                <!-- Médico (solo paciente y admin) -->
                <td *ngIf="esPaciente || esAdmin" 
                    class="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                  <div class="flex flex-col">
                    <span class="font-medium">{{ c.medico?.nombre || '—' }}</span>
                    <span *ngIf="c.medico?.especialidad" class="text-xs text-gray-500">
                      {{ c.medico.especialidad }}
                    </span>
                  </div>
                </td>

                <!-- Paciente (solo médico y admin) -->
                <td *ngIf="esMedico || esAdmin" 
                    class="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                  <div class="flex flex-col">
                    <span class="font-medium">{{ c.paciente?.nombre || '—' }}</span>
                    <span *ngIf="c.paciente?.dni" class="text-xs text-gray-500">
                      DNI: {{ c.paciente.dni }}
                    </span>
                  </div>
                </td>

                <!-- Acciones (solo admin) -->
                <td *ngIf="esAdmin" class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                  <div class="flex space-x-2">
                    <button 
                      [routerLink]="['/citas/editar', c.id]"
                      class="bg-blue-500 hover:bg-blue-600 text-white px-3 py-1.5 rounded-md transition duration-200 text-xs font-medium">
                      ✏️ Editar
                    </button>
                    <button 
                      (click)="eliminar(c.id!)"
                      class="bg-red-500 hover:bg-red-600 text-white px-3 py-1.5 rounded-md transition duration-200 text-xs font-medium">
                      🗑️ Eliminar
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Mensaje cuando no hay citas -->
        <div *ngIf="!citas.length" 
             class="text-center py-12">
          <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" 
                  d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
          </svg>
          <p class="mt-4 text-lg text-gray-500 font-medium">No hay citas registradas</p>
          <p class="mt-2 text-sm text-gray-400">
            <span *ngIf="esPaciente">No tienes citas programadas</span>
            <span *ngIf="esMedico">No tienes citas asignadas</span>
            <span *ngIf="esAdmin">Las citas aparecerán aquí cuando se creen</span>
          </p>
        </div>
      </div>
    </div>
  `
})
export class CitaListComponent implements OnInit {
  citas: Cita[] = [];
  esAdmin = false;
  esPaciente = false;
  esMedico = false;
  
  pacienteLogueado: Paciente | null = null;
  medicoLogueado: Medico | null = null;

  constructor(
    private citaService: CitaService,
    private pacienteService: PacienteService,
    private medicoService: MedicoService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.detectarRol();
    this.cargarCitas();
  }

  detectarRol(): void {
    this.esAdmin = this.authService.isAdmin();
    this.esPaciente = this.authService.isPaciente();
    this.esMedico = this.authService.isMedico();
  }

  cargarCitas(): void {
    if (this.esAdmin) {
      // ✅ Admin ve TODAS las citas
      console.log('🔹 Cargando todas las citas (ADMIN)');
      this.citaService.listarDetalles().subscribe({
        next: (data) => {
          this.citas = data;
          console.log('✅ Citas cargadas:', data);
        },
        error: (err) => console.error('❌ Error al listar citas:', err)
      });
    } 
    else if (this.esPaciente) {
      // ✅ Paciente ve solo SUS citas (con datos del médico)
      console.log('🔹 Cargando citas del paciente');
      this.pacienteService.obtenerPerfil().subscribe({
        next: (paciente) => {
          this.pacienteLogueado = paciente;
          console.log('✅ Paciente logueado:', paciente);
          
          if (paciente.id) {
            this.citaService.listarDetallesPorPaciente(paciente.id).subscribe({
              next: (data) => {
                this.citas = data;
                console.log('✅ Citas del paciente:', data);
              },
              error: (err) => console.error('❌ Error al listar citas del paciente:', err)
            });
          } else {
            console.error('❌ El paciente no tiene ID');
          }
        },
        error: (err) => console.error('❌ Error al obtener perfil del paciente:', err)
      });
    } 
    else if (this.esMedico) {
      // ✅ Médico ve solo citas ASIGNADAS a él (con datos del paciente)
      console.log('🔹 Cargando citas del médico');
      this.medicoService.obtenerPerfil().subscribe({
        next: (medico) => {
          this.medicoLogueado = medico;
          console.log('✅ Médico logueado:', medico);
          
          if (medico.id) {
            this.citaService.listarDetallesPorMedico(medico.id).subscribe({
              next: (data) => {
                this.citas = data;
                console.log('✅ Citas del médico (con pacientes):', data);
              },
              error: (err) => console.error('❌ Error al listar citas del médico:', err)
            });
          } else {
            console.error('❌ El médico no tiene ID');
          }
        },
        error: (err) => console.error('❌ Error al obtener perfil del médico:', err)
      });
    }
  }

  eliminar(id: number): void {
    if (confirm('¿Estás seguro de eliminar esta cita?')) {
      this.citaService.eliminar(id).subscribe({
        next: () => {
          alert('Cita eliminada correctamente');
          this.citas = this.citas.filter((c) => c.id !== id);
        },
        error: (err) => console.error('❌ Error al eliminar cita:', err)
      });
    }
  }

  getEstadoClasses(estado: string): string {
    switch (estado?.toUpperCase()) {
      case 'PENDIENTE':
        return 'bg-yellow-100 text-yellow-800';
      case 'CONFIRMADA':
        return 'bg-green-100 text-green-800';
      case 'CANCELADA':
        return 'bg-red-100 text-red-800';
      case 'COMPLETADA':
        return 'bg-blue-100 text-blue-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }
}

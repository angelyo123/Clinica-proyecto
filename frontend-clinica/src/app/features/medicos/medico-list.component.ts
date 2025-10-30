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
    <div class="p-6">
      <!-- Header -->
      <div class="flex flex-col md:flex-row md:justify-between md:items-center mb-6 gap-3">
        <h2 class="text-2xl font-bold text-gray-800">Gestión de Médicos</h2>

        <!-- Botón Crear -->
        <a
          *ngIf="esAdmin"
          routerLink="/medicos/nuevo"
          class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg shadow transition"
        >
          ➕ Nuevo Médico
        </a>
      </div>

      <!-- Tabla -->
      <div class="overflow-x-auto bg-white shadow-md rounded-lg">
        <table class="min-w-full border border-gray-200">
          <thead class="bg-gray-100">
  <tr class="text-left text-gray-700">
    <th class="py-3 px-4 border-b">ID</th>
    <th class="py-3 px-4 border-b">Nombre</th>
    <th class="py-3 px-4 border-b">Especialidad</th>
    <th class="py-3 px-4 border-b">Teléfono</th>
    <th class="py-3 px-4 border-b">DNI</th>
    @if (esAdmin) {
      <th class="py-3 px-4 border-b text-center">Acciones</th>
    }
  </tr>
</thead>

<tbody>
  @for (m of medicos; track m.id) {
    <tr class="border-b hover:bg-gray-50 transition">
      <td class="py-2 px-4">{{ m.id }}</td>
      <td class="py-2 px-4 font-medium">{{ m.nombre }}</td>
      <td class="py-2 px-4">{{ m.especialidad }}</td>
      <td class="py-2 px-4">{{ m.telefono }}</td>
      <td class="py-2 px-4">{{ m.dni }}</td>

      @if (esAdmin) {
        <td class="py-2 px-4 text-center flex justify-center gap-3">
          <a [routerLink]="['/medicos/editar', m.id]"
             class="inline-flex items-center gap-1 px-3 py-1.5 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition">
            ✏️ Editar
          </a>
          <button (click)="eliminar(m.id!)"
                  class="inline-flex items-center gap-1 px-3 py-1.5 bg-red-500 text-white rounded-lg hover:bg-red-600 transition">
            🗑️ Eliminar
          </button>
        </td>
      }
    </tr>
  }
</tbody>
        </table>
      </div>

      <!-- Sin registros -->
      <p *ngIf="!medicos.length" class="text-gray-500 text-center mt-4 italic">
        No hay médicos registrados.
      </p>
    </div>
  `
})
export class MedicoListComponent implements OnInit {
  medicos: Medico[] = [];
  esAdmin = false;

  constructor(
    private medicoService: MedicoService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.esAdmin = this.authService.isAdmin();
    this.cargarMedicos();
  }

  cargarMedicos(): void {
    this.medicoService.listar().subscribe({
      next: (data) => this.medicos = data,
      error: (err) => console.error('Error al listar médicos:', err)
    });
  }

  eliminar(id: number): void {
    if (confirm('¿Seguro que desea eliminar este médico?')) {
      this.medicoService.eliminar(id).subscribe({
        next: () => {
          alert('Médico eliminado correctamente');
          this.medicos = this.medicos.filter(m => m.id !== id);
        },
        error: (err) => console.error('Error al eliminar médico:', err)
      });
    }
  }
}
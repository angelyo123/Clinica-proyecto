import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PacienteService } from '../../core/services/paciente.service';
import { Paciente } from '../../core/models/paciente.model';

@Component({
  selector: 'app-paciente-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
 template: `
    <div class="p-6 md:p-10 max-w-7xl mx-auto">
      
      <div class="flex justify-between items-center mb-6">
        <h2 class="text-3xl font-bold text-gray-800">
          Lista de Pacientes
        </h2>
        <button routerLink="/pacientes/nuevo" 
                class="bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-4 rounded-lg shadow-md transition-colors duration-200">
          Nuevo Paciente
        </button>
      </div>

      <div class="shadow-lg rounded-lg overflow-hidden border border-gray-200">
        <table class="min-w-full divide-y divide-gray-200">
          
          <thead class="bg-gray-100">
            <tr>
              <th scope="col" class="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                ID
              </th>
              <th scope="col" class="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                Nombre
              </th>
              <th scope="col" class="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                DNI
              </th>
              <th scope="col" class="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                Teléfono
              </th>
              <th scope="col" class="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                Usuario
              </th>
              <th scope="col" class="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">
                Acciones
              </th>
            </tr>
          </thead>
          
          <tbody class="bg-white divide-y divide-gray-200">
            <tr *ngFor="let p of pacientes" class="hover:bg-gray-50 transition-colors duration-150">
              
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                {{ p.id }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                {{ p.nombre }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                {{ p.dni }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                {{ p.telefono }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-700">
                {{ p.usuario|| 'N/A' }} </td>
              
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                <div class="flex items-center gap-3">
                  <button [routerLink]="['/pacientes/editar', p.id]" 
                          class="bg-yellow-500 hover:bg-yellow-600 text-white font-semibold py-1 px-3 rounded-md text-xs transition-colors duration-200">
                    Editar
                  </button>
                  <button (click)="eliminar(p.id!)" 
                          class="bg-red-600 hover:bg-red-700 text-white font-semibold py-1 px-3 rounded-md text-xs transition-colors duration-200">
                    Eliminar
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <p *ngIf="!pacientes.length" class="text-center text-gray-500 py-10">
          No hay pacientes registrados.
        </p>
      </div>

    </div>
  `
})
export class PacienteListComponent implements OnInit {
  pacientes: Paciente[] = [];

  constructor(private pacienteService: PacienteService) {}

  ngOnInit(): void {
    this.cargarPacientes();
  }

  cargarPacientes(): void {
    this.pacienteService.listar().subscribe({
      next: data => (this.pacientes = data),
      error: err => console.error('Error al listar pacientes:', err)
    });
  }

  eliminar(id: number): void {
    if (confirm('¿Seguro que deseas eliminar este paciente?')) {
      this.pacienteService.eliminar(id).subscribe({
        next: () => {
          alert('Paciente eliminado');
          this.pacientes = this.pacientes.filter(p => p.id !== id);
        },
        error: err => console.error('Error al eliminar paciente:', err)
      });
    }
  }
}

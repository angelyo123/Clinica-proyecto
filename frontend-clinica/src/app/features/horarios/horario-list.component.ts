import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { HorarioService } from '../../core/services/horario.service';
import { Horario } from '../../core/models/horario.model';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-horario-list',
  standalone: true,
  imports: [CommonModule,FormsModule, RouterLink],
  template: `
    <div class="p-6">
      <!-- Header -->
      <div class="flex flex-col md:flex-row md:justify-between md:items-center mb-6 gap-3">
        <h2 class="text-2xl font-bold text-gray-800">Gestión de Horarios</h2>

        <!-- Solo ADMIN puede crear -->
        <a
          *ngIf="esAdmin"
          routerLink="/horarios/nuevo"
          class="bg-blue-600 hover:bg-blue-700 text-white px-4 py-2 rounded-lg shadow transition"
        >
          ➕ Nuevo Horario
        </a>
      </div>

      <!-- Filtros -->
      <div class="bg-gray-50 p-4 rounded-lg shadow mb-4 flex flex-col md:flex-row gap-4">
        <div class="flex-1">
          <label class="block text-gray-700 text-sm font-medium mb-1">Médico ID</label>
          <input
            type="number"
            [(ngModel)]="filtroMedicoId"
            placeholder="Ej: 1"
            class="border rounded-md px-3 py-2 w-full"
          />
        </div>

        <div class="flex-1">
          <label class="block text-gray-700 text-sm font-medium mb-1">Día de Semana</label>
          <select
            [(ngModel)]="filtroDia"
            class="border rounded-md px-3 py-2 w-full"
          >
            <option value="">Todos</option>
            <option *ngFor="let dia of dias">{{ dia }}</option>
          </select>
        </div>

        <div class="flex items-end gap-2">
          <button
            (click)="aplicarFiltros()"
            class="bg-green-600 hover:bg-green-700 text-white px-4 py-2 rounded-lg"
          >
            Filtrar
          </button>
          <button
            (click)="limpiarFiltros()"
            class="bg-gray-400 hover:bg-gray-500 text-white px-4 py-2 rounded-lg"
          >
            Limpiar
          </button>
        </div>
      </div>

      <!-- Tabla -->
      <div class="overflow-x-auto bg-white shadow-md rounded-lg">
        <table class="min-w-full border border-gray-200">
          <thead class="bg-gray-100">
            <tr class="text-left text-gray-700">
              <th class="py-3 px-4 border-b">#</th>
              <th class="py-3 px-4 border-b">Médico ID</th>
              <th class="py-3 px-4 border-b">Día</th>
              <th class="py-3 px-4 border-b">Inicio</th>
              <th class="py-3 px-4 border-b">Fin</th>
              <th class="py-3 px-4 border-b">Disponible</th>
              <th *ngIf="esAdmin" class="py-3 px-4 border-b text-center">Acciones</th>
            </tr>
          </thead>

          <tbody>
            <tr
              *ngFor="let h of horariosFiltrados | slice:(paginaActual-1)*filasPorPagina:(paginaActual)*filasPorPagina"
              class="border-b hover:bg-gray-50 transition"
            >
              <td class="py-2 px-4">{{ h.id }}</td>
              <td class="py-2 px-4">{{ h.medicoId }}</td>
              <td class="py-2 px-4 font-medium">{{ h.diaSemana }}</td>
              <td class="py-2 px-4">{{ h.horaInicio }}</td>
              <td class="py-2 px-4">{{ h.horaFin }}</td>
              <td class="py-2 px-4">
                <span
                  [ngClass]="h.disponible ? 'bg-green-100 text-green-700' : 'bg-red-100 text-red-700'"
                  class="px-2 py-1 rounded text-sm font-semibold"
                >
                  {{ h.disponible ? 'Sí' : 'No' }}
                </span>
              </td>

              <td *ngIf="esAdmin" class="py-2 px-4 text-center flex justify-center gap-3">
  <!-- Botón Editar -->
  <a
    [routerLink]="['/horarios/editar', h.id]"
    class="inline-flex items-center gap-1 px-3 py-1.5 bg-blue-500 text-white rounded-lg shadow hover:bg-blue-600 transition-all"
  >
    <svg
      xmlns="http://www.w3.org/2000/svg"
      class="w-4 h-4"
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
      stroke-width="2"
    >
      <path
        stroke-linecap="round"
        stroke-linejoin="round"
        d="M16.862 4.487l1.651 1.651a1.5 1.5 0 010 2.122l-8.486 8.486a1.5 1.5 0 01-.53.35l-3.604 1.201a.5.5 0 01-.63-.63l1.201-3.604a1.5 1.5 0 01.35-.53l8.486-8.486a1.5 1.5 0 012.122 0z"
      />
    </svg>
    Editar
  </a>

  <!-- Botón Eliminar -->
  <button
    (click)="eliminar(h.id!)"
    class="inline-flex items-center gap-1 px-3 py-1.5 bg-red-500 text-white rounded-lg shadow hover:bg-red-600 transition-all"
  >
    <svg
      xmlns="http://www.w3.org/2000/svg"
      class="w-4 h-4"
      fill="none"
      viewBox="0 0 24 24"
      stroke="currentColor"
      stroke-width="2"
    >
      <path
        stroke-linecap="round"
        stroke-linejoin="round"
        d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6M9 7V4a1 1 0 011-1h4a1 1 0 011 1v3m-7 0h8"
      />
    </svg>
    Eliminar
  </button>
</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Sin registros -->
      <p
        *ngIf="!horariosFiltrados.length"
        class="text-gray-500 text-center mt-4 italic"
      >
        No hay horarios registrados.
      </p>

      <!-- Paginación -->
      <div class="flex justify-center items-center gap-2 mt-4">
        <button
          (click)="cambiarPagina(-1)"
          [disabled]="paginaActual === 1"
          class="px-3 py-1 bg-gray-300 hover:bg-gray-400 rounded disabled:opacity-50"
        >
          ◀
        </button>

        <span class="text-gray-700">Página {{ paginaActual }} / {{ totalPaginas }}</span>

        <button
          (click)="cambiarPagina(1)"
          [disabled]="paginaActual === totalPaginas"
          class="px-3 py-1 bg-gray-300 hover:bg-gray-400 rounded disabled:opacity-50"
        >
          ▶
        </button>
      </div>
    </div>
  `
})
export class HorarioListComponent implements OnInit {
  horarios: Horario[] = [];
  horariosFiltrados: Horario[] = [];
  dias = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

  filtroMedicoId: number | null = null;
  filtroDia: string = '';

  esAdmin = false;

  // paginación
  paginaActual = 1;
  filasPorPagina = 5;

  get totalPaginas(): number {
    return Math.ceil(this.horariosFiltrados.length / this.filasPorPagina);
  }

  constructor(
    private horarioService: HorarioService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.detectarRol();
    this.cargarHorarios();
  }

  detectarRol(): void {
    this.esAdmin = this.authService.isAdmin();
  }

  cargarHorarios(): void {
    this.horarioService.listar().subscribe({
      next: (data) => {
        this.horarios = data;
        this.horariosFiltrados = [...data];
      },
      error: (err) => console.error('Error al listar horarios:', err)
    });
  }

  aplicarFiltros(): void {
    this.horariosFiltrados = this.horarios.filter((h) => {
      const coincideMedico = this.filtroMedicoId ? h.medicoId === +this.filtroMedicoId : true;
      const coincideDia = this.filtroDia ? h.diaSemana === this.filtroDia : true;
      return coincideMedico && coincideDia;
    });
    this.paginaActual = 1;
  }

  limpiarFiltros(): void {
    this.filtroMedicoId = null;
    this.filtroDia = '';
    this.horariosFiltrados = [...this.horarios];
    this.paginaActual = 1;
  }

  cambiarPagina(delta: number): void {
    const nueva = this.paginaActual + delta;
    if (nueva >= 1 && nueva <= this.totalPaginas) this.paginaActual = nueva;
  }

  eliminar(id: number): void {
    if (confirm('¿Estás seguro de eliminar este horario?')) {
      this.horarioService.eliminar(id).subscribe({
        next: () => {
          alert('Horario eliminado correctamente');
          this.horarios = this.horarios.filter((h) => h.id !== id);
          this.aplicarFiltros();
        },
        error: (err) => console.error('Error al eliminar horario:', err)
      });
    }
  }
}

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { HorarioService } from '../../../core/services/horario.service';
import { Horario } from '../../../core/models/horario.model';
import { MedicoService } from '../../../core/services/medico.service';
import { Medico } from '../../../core/models/medico.model';


@Component({
  selector: 'app-horario-medico',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="max-w-5xl mx-auto p-6">
      <!-- Encabezado -->
      <div class="flex justify-between items-center mb-6">
        <h2 class="text-2xl font-bold text-gray-800">
          🩺 Horarios disponibles del Dr. {{ medico?.nombre || '...' }}
        </h2>
        <button
          routerLink="/medicos-disponibles"
          class="bg-gray-600 hover:bg-gray-700 text-white px-4 py-2 rounded-lg shadow transition">
          ← Volver a Médicos
        </button>
      </div>

      <!-- Loading -->
      <div *ngIf="cargando" class="text-center py-8">
        <div class="w-10 h-10 mx-auto border-4 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
        <p class="text-gray-500 mt-3">Cargando horarios...</p>
      </div>

      <!-- Sin horarios -->
      <div *ngIf="!cargando && !horarios.length" class="text-center text-gray-500 py-10">
        <p>No hay horarios disponibles para este médico.</p>
      </div>

      <!-- Tabla de horarios -->
      <div *ngIf="!cargando && horarios.length" class="bg-white shadow-md rounded-lg overflow-hidden">
        <table class="min-w-full text-sm text-gray-700">
          <thead class="bg-gray-100 border-b border-gray-300">
            <tr>
              <th class="px-4 py-3 text-left font-semibold">Día</th>
              <th class="px-4 py-3 text-left font-semibold">Hora Inicio</th>
              <th class="px-4 py-3 text-left font-semibold">Hora Fin</th>
              <th class="px-4 py-3 text-center font-semibold">Acción</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let h of horarios" class="border-b hover:bg-gray-50 transition">
              <td class="px-4 py-2">{{ h.diaSemana | titlecase }}</td>
              <td class="px-4 py-2">{{ h.horaInicio }}</td>
              <td class="px-4 py-2">{{ h.horaFin }}</td>
              <td class="px-4 py-2 text-center">
                <button
                  (click)="reservarCita(h.id!)"
                  class="bg-green-500 hover:bg-green-600 text-white px-3 py-1 rounded text-xs transition">
                  📅 Reservar cita
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  `
})
export class HorarioMedicoComponent implements OnInit {
  medicoId!: number;
  medico?: Medico;
  horarios: Horario[] = [];
  cargando = true;

  constructor(
    private route: ActivatedRoute,
    private horarioService: HorarioService,
    private medicoService: MedicoService
  ) {}

  ngOnInit(): void {
    this.medicoId = Number(this.route.snapshot.paramMap.get('id'));
    if (!this.medicoId) return;

    this.cargarDatos();
  }

  cargarDatos(): void {
    this.cargando = true;

    // Cargamos el médico
    this.medicoService.obtener(this.medicoId).subscribe({
      next: (m) => (this.medico = m),
      error: (err) => console.error('Error al obtener médico:', err)
    });

    // Cargamos los horarios
    this.horarioService.listarPorMedico(this.medicoId).subscribe({
      next: (data) => {
        this.horarios = data.filter((h) => h.disponible === true);
        this.cargando = false;
      },
      error: (err) => {
        console.error('Error al listar horarios:', err);
        this.cargando = false;
      }
    });
  }

  reservarCita(horarioId: number): void {
    alert(`🩺 Reservando cita en horario ID ${horarioId}`);
    // En producción: 
    // this.router.navigate(['/citas/nueva'], { queryParams: { medicoId: this.medicoId, horarioId } });
  }
}

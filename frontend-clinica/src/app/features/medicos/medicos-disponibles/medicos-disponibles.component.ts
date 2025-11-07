import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MedicoService } from '../../../core/services/medico.service';
import { HorarioService } from '../../../core/services/horario.service';
import { Medico } from '../../../core/models/medico.model';
import { Horario } from '../../../core/models/horario.model';

@Component({
  selector: 'app-medicos-disponibles',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="max-w-6xl mx-auto p-6">
      <!-- Encabezado -->
      <div class="flex justify-between items-center mb-8">
        <h2 class="text-3xl font-bold text-gray-800">👨‍⚕️ Médicos con horarios disponibles</h2>
        <button
          class="bg-gray-600 hover:bg-gray-700 text-white px-4 py-2 rounded-lg shadow transition"
          routerLink="/mis-citas-paciente">
          ← Volver a mis citas
        </button>
      </div>

      <!-- Cargando -->
      <div *ngIf="cargando" class="text-center py-10">
        <div class="w-10 h-10 mx-auto border-4 border-blue-500 border-t-transparent rounded-full animate-spin"></div>
        <p class="text-gray-500 mt-3">Cargando médicos...</p>
      </div>

      <!-- Sin médicos -->
      <div *ngIf="!cargando && !medicos.length" class="text-center text-gray-500 py-12">
        <p class="text-lg">No hay médicos con horarios disponibles por el momento.</p>
      </div>

      <!-- Lista de médicos -->
      <div *ngIf="!cargando && medicos.length" class="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div
          *ngFor="let m of medicos"
          class="bg-white border border-gray-200 rounded-xl shadow-sm hover:shadow-md transition p-5"
        >
          <div class="mb-3">
            <h3 class="text-xl font-semibold text-gray-800 mb-1">{{ m.nombre }}</h3>
            <p class="text-sm text-gray-600"><strong>Especialidad:</strong> {{ m.especialidad }}</p>
            <p class="text-sm text-gray-600"><strong>Teléfono:</strong> {{ m.telefono }}</p>
          </div>

          <ng-template #verHorariosBtn>
            <button
              class="bg-blue-600 hover:bg-blue-700 text-white w-full py-2 rounded-lg transition mt-2"
              [routerLink]="['/horarios/medico', m.id]"
            >
              👁️ Ver horarios
            </button>
          </ng-template>
        </div>
      </div>
    </div>
  `,
})
export class MedicosDisponiblesComponent implements OnInit {
  medicos: Medico[] = [];
  horarios: { [medicoId: number]: Horario[] } = {};
  horariosVisibles: { [medicoId: number]: boolean } = {};
  cargando = true;

  constructor(
    private medicoService: MedicoService,
    private horarioService: HorarioService
  ) {}

  ngOnInit(): void {
    this.cargarMedicosConHorarios();
  }

  cargarMedicosConHorarios(): void {
    this.cargando = true;
    this.medicoService.listarPublico().subscribe({
      next: (data) => {
        // Paso 1: Filtrar médicos con horarios disponibles
        const peticiones = data.map((m) =>
          this.horarioService.listarPorMedico(m.id!).toPromise()
        );

        Promise.all(peticiones)
          .then((respuestas) => {
            this.medicos = data.filter((_, i) =>
              (respuestas[i] ?? []).some((h) => h.disponible === true)
            );

            this.medicos.forEach((m, i) => {
              const horariosMedico = (respuestas[i] ?? []).filter(
                (h) => h.disponible === true
              );
              this.horarios[m.id!] = horariosMedico;
            });

          })
          .finally(() => (this.cargando = false));
      },
      error: (err) => {
        console.error('Error al listar médicos:', err);
        this.cargando = false;
      },
    });
  }


  cerrarHorarios(medicoId: number): void {
    this.horariosVisibles[medicoId] = false;
  }

  reservarCita(medicoId: number, horarioId: number): void {
    alert(`🩺 Reservando cita con médico ${medicoId} en horario ${horarioId}`);
    // aquí podrías redirigir a un formulario real:
    // this.router.navigate(['/citas/nueva'], { queryParams: { medicoId, horarioId } });
  }
}

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router'; // 👈 agrega RouterLink
import { MedicoService } from '../../../core/services/medico.service';
import { Medico } from '../../../core/models/medico.model';

@Component({
  selector: 'app-medicos-disponibles',
  standalone: true,
  imports: [CommonModule, RouterLink], // 👈 agrégalo aquí
  template: `
    <div class="max-w-6xl mx-auto p-6">
      <div class="flex justify-between items-center mb-4">
        <h2 class="text-2xl font-bold text-gray-800">👨‍⚕️ Médicos Disponibles</h2>
        <button
          class="bg-gray-600 hover:bg-gray-700 text-white px-4 py-2 rounded-lg shadow"
          routerLink="/mis-citas-paciente">
          ← Volver a mis citas
        </button>
      </div>

      <div *ngIf="!medicos.length" class="text-center text-gray-500">
        No hay médicos disponibles.
      </div>

      <div class="grid md:grid-cols-2 lg:grid-cols-3 gap-6">
        <div *ngFor="let m of medicos"
             class="bg-white p-5 rounded-lg shadow hover:shadow-lg transition">
          <h3 class="text-xl font-bold text-gray-800 mb-1">{{ m.nombre }}</h3>
          <p class="text-sm text-gray-600 mb-1"><strong>Especialidad:</strong> {{ m.especialidad }}</p>
          <p class="text-sm text-gray-600 mb-1"><strong>DNI:</strong> {{ m.dni }}</p>
          <p class="text-sm text-gray-600 mb-3"><strong>Teléfono:</strong> {{ m.telefono }}</p>

          <button (click)="reservarCita(m.id!)"
                  class="bg-green-500 hover:bg-green-600 text-white font-medium px-4 py-2 rounded-lg transition">
            📅 Reservar cita
          </button>
        </div>
      </div>
    </div>
  `
})
export class MedicosDisponiblesComponent implements OnInit {
  medicos: Medico[] = [];

  constructor(private medicoService: MedicoService, private router: Router) {}

  ngOnInit(): void {
    this.medicoService.listarPublico().subscribe({
      next: data => this.medicos = data,
      error: err => console.error('Error al listar médicos:', err)
    });
  }

  reservarCita(medicoId: number): void {
    this.router.navigate(['/citas/nueva'], { queryParams: { medicoId } });
  }
}

import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HorarioService } from '../../core/services/horario.service';
import { Horario } from '../../core/models/horario.model';

@Component({
  selector: 'app-horario-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-gray-50 py-10 px-4">
      <div class="bg-white shadow-xl rounded-2xl w-full max-w-lg p-8">
        <h2 class="text-2xl font-semibold text-gray-800 text-center mb-6">
          {{ editMode ? 'Editar Horario' : 'Nuevo Horario' }}
        </h2>

        <form *ngIf="rolCargado" [formGroup]="form" (ngSubmit)="guardar()" class="space-y-5">

          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">Día de la Semana</label>
            <select
              formControlName="diaSemana"
              class="w-full rounded-lg border border-gray-300 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 p-2 text-gray-700"
              required
            >
              <option value="">Seleccione...</option>
              <option *ngFor="let d of dias" [value]="d">{{ d }}</option>
            </select>
          </div>

          <div class="grid grid-cols-2 gap-4">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Hora de Inicio</label>
              <input
                type="time"
                formControlName="horaInicio"
                class="w-full rounded-lg border border-gray-300 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 p-2"
                required
              />
            </div>

            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">Hora de Fin</label>
              <input
                type="time"
                formControlName="horaFin"
                class="w-full rounded-lg border border-gray-300 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 p-2"
                required
              />
            </div>
          </div>

          <div class="flex items-center gap-2">
            <input
              type="checkbox"
              formControlName="disponible"
              id="disponible"
              class="h-4 w-4 text-indigo-600 border-gray-300 rounded focus:ring-indigo-500"
            />
            <label for="disponible" class="text-sm text-gray-700">Disponible</label>
          </div>

          <ng-container *ngIf="esAdmin">
            <div>
              <label class="block text-sm font-medium text-gray-700 mb-1">ID Médico</label>
              <input
                type="number"
                formControlName="medicoId"
                class="w-full rounded-lg border border-gray-300 focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 p-2"
              />
            </div>
          </ng-container>

          <ng-container *ngIf="!esAdmin">
            <input type="hidden" formControlName="medicoId" />
          </ng-container>

          <div class="flex justify-between mt-6">
            <button
              type="submit"
              [disabled]="form.invalid"
              class="px-5 py-2 bg-indigo-600 text-white rounded-lg shadow hover:bg-indigo-700 transition disabled:opacity-50"
            >
              {{ editMode ? 'Actualizar' : 'Guardar' }}
            </button>

            <button
              type="button"
              (click)="cancelar()"
              class="px-5 py-2 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition"
            >
              Cancelar
            </button>
          </div>
        </form>

        <p *ngIf="!rolCargado" class="text-center text-gray-500 mt-6">Cargando formulario...</p>
      </div>
    </div>
  `
})
export class HorarioFormComponent implements OnInit {
  form!: FormGroup;
  editMode = false;
  id!: number;
  esAdmin = false;
  rolCargado = false;
  dias = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];

  constructor(
    private fb: FormBuilder,
    private horarioService: HorarioService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      medicoId: [''],
      diaSemana: ['', Validators.required],
      horaInicio: ['', Validators.required],
      horaFin: ['', Validators.required],
      disponible: [true]
    });

    this.detectarRolUsuario();

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.editMode = true;
      this.id = +idParam;
      this.horarioService.obtener(this.id).subscribe(h => this.form.patchValue(h));
    }

    const medicoIdParam = this.route.snapshot.queryParamMap.get('medicoId');
    if (medicoIdParam) this.form.patchValue({ medicoId: +medicoIdParam });
  }

  private detectarRolUsuario(): void {
    const token = localStorage.getItem('token');
    if (token) {
      try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        const roles: string[] = payload.roles || [];
        this.esAdmin = roles.includes('ROLE_ADMIN');
      } catch (e) {
        console.error('Error al leer roles del token:', e);
      }
    }

    this.rolCargado = true;
    this.cdr.detectChanges();
  }

  guardar(): void {
    if (this.form.invalid) return;

    const horario: Horario = this.form.value;
    const request = this.editMode
      ? this.horarioService.actualizar(this.id, horario)
      : this.horarioService.crear(horario);

    request.subscribe({
      next: () => {
        alert(this.editMode ? 'Horario actualizado correctamente' : 'Horario registrado exitosamente');
        this.router.navigate(['/horarios']);
      },
      error: (err) => {
        console.error('Error al guardar horario:', err);
        alert('Ocurrió un error al guardar el horario');
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/horarios']);
  }
}

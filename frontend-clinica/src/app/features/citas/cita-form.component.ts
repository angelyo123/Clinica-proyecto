import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CitaService } from '../../core/services/cita.service';
import { Cita } from '../../core/models/cita.model';
import { PacienteService } from '../../core/services/paciente.service';

@Component({
  selector: 'app-cita-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
   <div class="container mx-auto p-4 md:p-6 max-w-lg"> <h2 class="text-3xl font-semibold text-gray-800 mb-6 text-center">
    {{ editMode ? 'Editar Cita' : 'Nueva Cita' }}
  </h2>

  <div *ngIf="!rolCargado" class="text-center text-gray-500 italic">
    Cargando formulario...
  </div>

  <form *ngIf="rolCargado" [formGroup]="form" (ngSubmit)="guardar()" class="space-y-6 bg-white p-8 rounded-lg shadow-md border border-gray-200">

    <div>
      <label for="fechaHora" class="block text-sm font-medium text-gray-700 mb-1">Fecha y Hora:</label>
      <input
        type="datetime-local"
        id="fechaHora"
        formControlName="fechaHora"
        required
        class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
        [ngClass]="{'border-red-500': form.get('fechaHora')?.invalid && form.get('fechaHora')?.touched}"
      />
      <div *ngIf="form.get('fechaHora')?.invalid && form.get('fechaHora')?.touched" class="mt-1 text-xs text-red-600">
        La fecha y hora son requeridas.
      </div>
    </div>

    <div>
      <label for="estado" class="block text-sm font-medium text-gray-700 mb-1">Estado:</label>
      <select
        id="estado"
        formControlName="estado"
        required
        class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm bg-white"
        [ngClass]="{'border-red-500': form.get('estado')?.invalid && form.get('estado')?.touched}"
      >
        <option *ngFor="let e of estados" [value]="e">{{ e | titlecase }}</option>
      </select>
      <div *ngIf="form.get('estado')?.invalid && form.get('estado')?.touched" class="mt-1 text-xs text-red-600">
        El estado es requerido.
      </div>
    </div>

    <ng-container *ngIf="esAdmin">
      <div>
        <label for="medicoId" class="block text-sm font-medium text-gray-700 mb-1">ID Médico:</label>
        <input
          type="number"
          id="medicoId"
          formControlName="medicoId"
          placeholder="Ingrese ID del médico"
          class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
          [ngClass]="{'border-red-500': form.get('medicoId')?.invalid && form.get('medicoId')?.touched}"
        />
        <div *ngIf="form.get('medicoId')?.errors?.['required'] && form.get('medicoId')?.touched" class="mt-1 text-xs text-red-600">
           El ID del médico es requerido.
         </div>
      </div>

      <div>
        <label for="pacienteId" class="block text-sm font-medium text-gray-700 mb-1">ID Paciente:</label>
        <input
          type="number"
          id="pacienteId"
          formControlName="pacienteId"
          placeholder="Ingrese ID del paciente"
          class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
          [ngClass]="{'border-red-500': form.get('pacienteId')?.invalid && form.get('pacienteId')?.touched}"
        />
         <div *ngIf="form.get('pacienteId')?.errors?.['required'] && form.get('pacienteId')?.touched" class="mt-1 text-xs text-red-600">
           El ID del paciente es requerido.
         </div>
      </div>
    </ng-container>

    <input *ngIf="!esAdmin" type="hidden" formControlName="medicoId" />
    <input *ngIf="!esAdmin" type="hidden" formControlName="pacienteId" />


    <div class="flex items-center justify-end space-x-4 pt-4 border-t border-gray-200 mt-2">
      <button
        type="button"
        (click)="cancelar()"
        class="px-4 py-2 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition duration-150 ease-in-out"
      >
        Cancelar
      </button>
      <button
        type="submit"
        [disabled]="form.invalid || form.pristine"
        class="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed transition duration-150 ease-in-out"
      >
        {{ editMode ? 'Actualizar Cita' : 'Guardar Cita' }}
      </button>
    </div>

  </form>
</div>
  `,
  styles: [`
    form { display: flex; flex-direction: column; max-width: 400px; gap: 10px; }
    .acciones { display: flex; gap: 10px; margin-top: 10px; }
  `]
})
export class CitaFormComponent implements OnInit {
  form!: FormGroup;
  editMode = false;
  id!: number;
  esAdmin = false;
  rolCargado = false;
  estados: string[] = ['PENDIENTE', 'CONFIRMADA', 'CANCELADA', 'COMPLETADA'];

  constructor(
    private fb: FormBuilder,
    private citaService: CitaService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef,
    private pacienteService: PacienteService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      fechaHora: ['', Validators.required],
      estado: ['PENDIENTE', Validators.required],
      medicoId: [''],
      pacienteId: ['']
    });

    this.detectarRolUsuario();

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.editMode = true;
      this.id = +idParam;
      this.citaService.obtener(this.id).subscribe(c => {
        this.form.patchValue({
          fechaCita: c.fechaCita,
          estado: (c.estado || 'PENDIENTE').toUpperCase(),
          medicoId: c.medico?.id,
          pacienteId: c.paciente?.id
        });
      });
    }

    const medicoIdParam = this.route.snapshot.queryParamMap.get('medicoId');
    if (medicoIdParam) {
      this.form.patchValue({ medicoId: +medicoIdParam });
    }
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

    if (!this.esAdmin) {
  this.pacienteService.obtenerPerfil().subscribe({
    next: (paciente) => {
      if (paciente && paciente.id) {
        this.form.patchValue({ pacienteId: paciente.id });
        console.log('✅ Paciente logueado cargado:', paciente.id);
      }
    },
    error: (err) => console.error('Error al obtener perfil del paciente:', err)
  });
}

    this.cdr.detectChanges();
  }

guardar(): void {
  if (this.form.invalid) return;

  // Si es médico y está editando, usar endpoint específico de estado
  if (this.editMode && !this.esAdmin) {
    const nuevoEstado: string = this.form.value.estado;
    this.citaService.actualizarEstado(this.id, nuevoEstado).subscribe({
      next: () => {
        alert('Estado actualizado correctamente');
        this.redirigirDespuesDeAccion(); // 👈 Redirección dinámica
      },
      error: (err) => {
        console.error('Error al actualizar estado:', err);
        alert('Ocurrió un error al actualizar el estado');
      }
    });
    return;
  }

  // Flujos admin (o creación)
  const cita: Cita = {
    fechaCita: this.form.value.fechaCita,
    estado: this.form.value.estado,
    medico: this.form.value.medicoId ? { id: this.form.value.medicoId } : undefined,
    paciente: this.form.value.pacienteId ? { id: this.form.value.pacienteId } : undefined
  };

  const request = this.editMode
    ? this.citaService.actualizar(this.id, cita)
    : this.citaService.crearCita(cita);

  request.subscribe({
    next: () => {
      alert(this.editMode ? 'Cita actualizada correctamente' : 'Cita creada exitosamente');
      this.redirigirDespuesDeAccion(); // 👈 Redirección dinámica
    },
    error: (err) => {
      console.error('Error al guardar cita:', err);
      alert('Ocurrió un error al guardar la cita');
    }
  });
}

cancelar(): void {
  this.redirigirDespuesDeAccion(); // 👈 misma función usada aquí
}

/** 🔹 Nueva función: decide a dónde volver según el rol */
private redirigirDespuesDeAccion(): void {
  if (this.esAdmin) {
    this.router.navigate(['/citas']); // admin → módulo general de citas
  } else {
    this.router.navigate(['/mis-citas-paciente']); // paciente → sus propias citas
  }
}
}

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MedicoService } from '../../core/services/medico.service';
import { Medico } from '../../core/models/medico.model';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-medico-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
  <div class="min-h-screen flex items-center justify-center bg-gray-50 py-10 px-4">
    <div class="bg-white shadow-xl rounded-2xl w-full max-w-lg p-8">
      <h2 class="text-2xl font-semibold text-gray-800 text-center mb-6">
        {{ editMode ? 'Editar Médico' : 'Registrar Médico' }}
      </h2>

      <form [formGroup]="form" (ngSubmit)="guardar()" class="space-y-5">
        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Nombre</label>
          <input formControlName="nombre" type="text" class="w-full border rounded-lg p-2" required />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Especialidad</label>
          <input formControlName="especialidad" type="text" class="w-full border rounded-lg p-2" required />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">Teléfono</label>
          <input formControlName="telefono" type="text" class="w-full border rounded-lg p-2" required />
        </div>

        <div>
          <label class="block text-sm font-medium text-gray-700 mb-1">DNI</label>
          <input formControlName="dni" type="text" class="w-full border rounded-lg p-2" required />
        </div>

        <div class="flex justify-between mt-6">
          <button
            type="submit"
            [disabled]="form.invalid"
            class="px-5 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition disabled:opacity-50"
          >
            {{ editMode ? 'Actualizar' : 'Guardar' }}
          </button>

          <button
            type="button"
            (click)="cancelar()"
            class="px-5 py-2 bg-gray-300 text-gray-700 rounded-lg hover:bg-gray-400 transition"
          >
            Cancelar
          </button>
        </div>
      </form>
    </div>
  </div>
  `
})
export class MedicoFormComponent implements OnInit {
  form!: FormGroup;
  editMode = false;
  id!: number;

  constructor(
    private fb: FormBuilder,
    private medicoService: MedicoService,
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      nombre: ['', Validators.required],
      especialidad: ['', Validators.required],
      telefono: ['', Validators.required],
      dni: ['', Validators.required]
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.editMode = true;
      this.id = +idParam;
      this.medicoService.obtener(this.id).subscribe(m => this.form.patchValue(m));
    }
  }

  guardar(): void {
    if (this.form.invalid) return;
    const medico: Medico = this.form.value;

    const request = this.editMode
      ? this.medicoService.actualizar(this.id, medico)
      : this.medicoService.crear(medico);

    request.subscribe({
      next: () => {
        alert(this.editMode ? 'Médico actualizado correctamente' : 'Médico registrado exitosamente');
        this.router.navigate(['/medicos']);
      },
      error: (err) => {
        console.error('Error al guardar médico:', err);
        alert('Ocurrió un error al guardar el médico');
      }
    });
  }

  cancelar(): void {
    this.router.navigate(['/medicos']);
  }
}
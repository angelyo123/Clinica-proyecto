import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PacienteService } from '../../core/services/paciente.service';
import { Paciente } from '../../core/models/paciente.model';

@Component({
  selector: 'app-paciente-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="max-w-lg mx-auto p-6 md:p-10">
      
      <h2 class="text-3xl font-bold text-gray-800 mb-6 text-center">
        {{ editMode ? 'Editar Paciente' : 'Nuevo Paciente' }}
      </h2>

      <form [formGroup]="form" 
            (ngSubmit)="guardar()" 
            class="space-y-5 bg-white shadow-lg rounded-lg p-8 border border-gray-200">

        <div>
          <label for="nombre" class="block text-sm font-semibold text-gray-700 mb-1">
            Nombre:
          </label>
          <input type="text" 
                 id="nombre" 
                 formControlName="nombre" 
                 class="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm 
                        focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500
                        invalid:border-red-500 invalid:text-red-600 focus:invalid:ring-red-500" />
          <p *ngIf="form.get('nombre')?.invalid && form.get('nombre')?.touched" 
             class="text-xs text-red-600 mt-1">
            El nombre es obligatorio.
          </p>
        </div>

        <div>
          <label for="dni" class="block text-sm font-semibold text-gray-700 mb-1">
            DNI:
          </label>
          <input type="text" 
                 id="dni" 
                 formControlName="dni" 
                 class="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm 
                        focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500
                        invalid:border-red-500 invalid:text-red-600 focus:invalid:ring-red-500" />
          <p *ngIf="form.get('dni')?.invalid && form.get('dni')?.touched" 
             class="text-xs text-red-600 mt-1">
            El DNI es obligatorio.
          </p>
        </div>

        <div>
          <label for="telefono" class="block text-sm font-semibold text-gray-700 mb-1">
            Teléfono:
          </label>
          <input type="text" 
                 id="telefono" 
                 formControlName="telefono" 
                 class="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm 
                        focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500
                        invalid:border-red-500 invalid:text-red-600 focus:invalid:ring-red-500" />
          <p *ngIf="form.get('telefono')?.invalid && form.get('telefono')?.touched" 
             class="text-xs text-red-600 mt-1">
            El teléfono es obligatorio.
          </p>
        </div>

        <div class="flex items-center justify-end gap-4 pt-4">
          <button type="button" 
                  routerLink="/pacientes" 
                  class="bg-gray-200 hover:bg-gray-300 text-gray-800 font-medium py-2 px-4 rounded-lg transition-colors duration-200">
            Cancelar
          </button>
          <button type="submit" 
                  [disabled]="form.invalid"
                  class="bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-4 rounded-lg shadow 
                         transition-colors duration-200
                         disabled:bg-gray-400 disabled:cursor-not-allowed">
            {{ editMode ? 'Actualizar' : 'Guardar' }}
          </button>
        </div>

      </form>
    </div>
  `
})
export class PacienteFormComponent implements OnInit {
  form!: FormGroup;
  editMode = false;
  id!: number;

  constructor(
    private fb: FormBuilder,
    private pacienteService: PacienteService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      nombre: ['', Validators.required],
      dni: ['', Validators.required],
      telefono: ['', Validators.required]
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.editMode = true;
      this.id = +idParam;
      this.pacienteService.obtener(this.id).subscribe(p => this.form.patchValue(p));
    }
  }

  guardar(): void {
    if (this.form.invalid) return;

    const paciente: Paciente = this.form.value;

    const request = this.editMode
      ? this.pacienteService.actualizar(this.id, paciente)
      : this.pacienteService.crear(paciente);

    request.subscribe({
      next: () => {
        alert(this.editMode ? 'Paciente actualizado' : 'Paciente creado');
        this.router.navigate(['/pacientes']);
      },
      error: (err) => console.error(err)
    });
  }
}

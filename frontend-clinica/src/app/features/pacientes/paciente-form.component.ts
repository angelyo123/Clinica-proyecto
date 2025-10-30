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
                 (keypress)="onlyLetters($event)" 
                 class="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm 
                         focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500
                         invalid:border-red-500 invalid:text-red-600 focus:invalid:ring-red-500" />
          <div *ngIf="form.get('nombre')?.invalid && form.get('nombre')?.touched" 
               class="text-xs text-red-600 mt-1">
            <p *ngIf="form.get('nombre')?.errors?.['required']">El nombre es obligatorio.</p>
            <p *ngIf="form.get('nombre')?.errors?.['pattern']">El nombre solo puede contener letras y espacios.</p>
          </div>
        </div>

        <div>
          <label for="dni" class="block text-sm font-semibold text-gray-700 mb-1">
            DNI:
          </label>
          <input type="text" 
                 id="dni" 
                 formControlName="dni" 
                 maxlength="8" 
                 (keypress)="onlyNumbers($event)" 
                 class="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm 
                         focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500
                         invalid:border-red-500 invalid:text-red-600 focus:invalid:ring-red-500" />
          <div *ngIf="form.get('dni')?.invalid && form.get('dni')?.touched" 
               class="text-xs text-red-600 mt-1">
            <p *ngIf="form.get('dni')?.errors?.['required']">El DNI es obligatorio.</p>
            <p *ngIf="form.get('dni')?.errors?.['minlength'] || form.get('dni')?.errors?.['maxlength']">El DNI debe tener exactamente 8 dígitos.</p>
            <p *ngIf="form.get('dni')?.errors?.['pattern']">El DNI solo debe contener números.</p>
          </div>
        </div>

        <div>
          <label for="telefono" class="block text-sm font-semibold text-gray-700 mb-1">
            Teléfono:
          </label>
          <input type="text" 
                 id="telefono" 
                 formControlName="telefono" 
                 maxlength="9" 
                 (keypress)="onlyNumbers($event)" 
                 class="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm 
                         focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500
                         invalid:border-red-500 invalid:text-red-600 focus:invalid:ring-red-500" />
          <div *ngIf="form.get('telefono')?.invalid && form.get('telefono')?.touched" 
               class="text-xs text-red-600 mt-1">
            <p *ngIf="form.get('telefono')?.errors?.['required']">El teléfono es obligatorio.</p>
            <p *ngIf="form.get('telefono')?.errors?.['minlength'] || form.get('telefono')?.errors?.['maxlength']">El teléfono debe tener exactamente 9 dígitos.</p>
            <p *ngIf="form.get('telefono')?.errors?.['pattern']">El teléfono solo debe contener números.</p>
          </div>
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
  `,
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

  // 🚨 MÉTODOS DE FILTRADO EN TIEMPO REAL (KEYPRESS) 🚨

  /** Permite solo la entrada de letras, acentos y espacios. */
  public onlyLetters(event: KeyboardEvent): boolean {
    const isControlKey = event.key === 'Backspace' || event.key === 'Delete' || event.key.startsWith('Arrow') || event.key === 'Tab';
    if (isControlKey) return true;
    
    const char = event.key;
    const isLetter = /^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]$/.test(char); 
    return isLetter;
  }

  /** Permite solo la entrada de dígitos (0-9). */
  public onlyNumbers(event: KeyboardEvent): boolean {
    const isControlKey = event.key === 'Backspace' || event.key === 'Delete' || event.key.startsWith('Arrow') || event.key === 'Tab';
    if (isControlKey) return true;

    const char = event.key;
    const isDigit = /^\d$/.test(char); 
    return isDigit;
  }
  
  // --- ngOnInit CON VALIDACIONES ---

  ngOnInit(): void {
    const soloLetrasRegEx = /^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/;
    const soloNumerosRegEx = /^[0-9]+$/;

    this.form = this.fb.group({
      // Nombre: Solo letras
      nombre: ['', [
        Validators.required, 
        Validators.pattern(soloLetrasRegEx)
      ]],
      // DNI: 8 dígitos, solo números
      dni: ['', [
        Validators.required, 
        Validators.minLength(8), 
        Validators.maxLength(8), 
        Validators.pattern(soloNumerosRegEx)
      ]],
      // Teléfono: 9 dígitos, solo números
      telefono: ['', [
        Validators.required, 
        Validators.minLength(9), 
        Validators.maxLength(9), 
        Validators.pattern(soloNumerosRegEx)
      ]]
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.editMode = true;
      this.id = +idParam;
      this.pacienteService.obtener(this.id).subscribe(p => this.form.patchValue(p));
    }
  }


  guardar(): void {
    if (this.form.invalid) {
        this.form.markAllAsTouched();
        return;
    }

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
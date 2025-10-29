// En: features/registro-paciente/registro-paciente-form.component.ts

import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../core/services/auth.service'; // <-- Usa AuthService

@Component({
  selector: 'app-registro-paciente-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <div class="max-w-lg mx-auto p-6 md:p-10">
      
      <h2 class="text-3xl font-bold text-gray-800 mb-6 text-center">
        Crea tu Cuenta de Paciente
      </h2>

      <form [formGroup]="form" 
            (ngSubmit)="guardar()" 
            class="space-y-5 bg-white shadow-lg rounded-lg p-8 border border-gray-200">

        <h3 class="text-lg font-semibold text-gray-700 border-b pb-2">Datos Personales</h3>
        
        <div>
          <label for="nombre" class="block text-sm font-semibold text-gray-700 mb-1">
            Nombre:
          </label>
          <input type="text" id="nombre" formControlName="nombre" class="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
        </div>

        <div>
          <label for="dni" class="block text-sm font-semibold text-gray-700 mb-1">
            DNI:
          </label>
          <input type="text" id="dni" formControlName="dni" class="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
        </div>

        <div>
          <label for="telefono" class="block text-sm font-semibold text-gray-700 mb-1">
            Teléfono:
          </label>
          <input type="text" id="telefono" formControlName="telefono" class="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
        </div>

        <h3 class="text-lg font-semibold text-gray-700 border-b pb-2 pt-4">Datos de Acceso</h3>

        <div>
          <label for="username" class="block text-sm font-semibold text-gray-700 mb-1">
            Username (Usuario de Acceso):
          </label>
          <input type="text" 
                 id="username" 
                 formControlName="username" 
                 class="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm bg-gray-100" 
                 [readOnly]="true" />
          <p class="text-xs text-gray-500 mt-1">Se genera automáticamente con el DNI.</p>
        </div>
        
        <div>
          <label for="password" class="block text-sm font-semibold text-gray-700 mb-1">
            Crea tu Contraseña:
          </label>
          <input type="password" 
                 id="password" 
                 formControlName="password"
                 class="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
        </div>

        <div class="flex items-center justify-end gap-4 pt-4">
          <button type="button" 
                  routerLink="/login" 
                  class="bg-gray-200 hover:bg-gray-300 text-gray-800 font-medium py-2 px-4 rounded-lg transition-colors duration-200">
            Cancelar
          </button>
          <button type="submit" 
                  [disabled]="form.invalid"
                  class="bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-4 rounded-lg shadow 
                         transition-colors duration-200
                         disabled:bg-gray-400 disabled:cursor-not-allowed">
            Registrarme
          </button>
        </div>

      </form>
    </div>
  `
})
export class RegistroPacienteFormComponent implements OnInit {
  form!: FormGroup;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      nombre: ['', Validators.required],
      dni: ['', [Validators.required, Validators.minLength(8)]],
      telefono: ['', Validators.required],
      username: ['', Validators.required],
      password: ['', [Validators.required, Validators.minLength(6)]] // El paciente elige su contraseña
    });

    // Sincronizamos DNI -> Username
    this.form.get('dni')?.valueChanges.subscribe(dniValue => {
      this.form.get('username')?.setValue(dniValue);
    });
  }

  guardar(): void {
    if (this.form.invalid) return;

    // Llama al endpoint de registro público
    this.authService.registerPaciente(this.form.getRawValue()).subscribe({
      next: () => {
        alert('¡Cuenta creada exitosamente!');
        this.router.navigate(['/login']); // Redirige al login
      },
      error: (err) => {
        console.error(err);
        alert('Error al registrar: ' + (err.error.message || 'Error desconocido'));
      }
    });
  }
}
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
    <h2>{{ editMode ? 'Editar Paciente' : 'Nuevo Paciente' }}</h2>

    <form [formGroup]="form" (ngSubmit)="guardar()">
      <label>Nombre:</label>
      <input type="text" formControlName="nombre" />

      <label>DNI:</label>
      <input type="text" formControlName="dni" />

      <label>Teléfono:</label>
      <input type="text" formControlName="telefono" />

      <button type="submit" [disabled]="form.invalid">
        {{ editMode ? 'Actualizar' : 'Guardar' }}
      </button>
      <button routerLink="/pacientes">Cancelar</button>
    </form>
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

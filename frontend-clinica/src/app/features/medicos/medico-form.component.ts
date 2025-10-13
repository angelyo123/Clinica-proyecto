import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MedicoService } from '../../core/services/medico.service';
import { Medico } from '../../core/models/medico.model';

@Component({
  selector: 'app-medico-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <h2>{{ editMode ? 'Editar Médico' : 'Nuevo Médico' }}</h2>

    <form [formGroup]="form" (ngSubmit)="guardar()">
      <label>Nombre:</label>
      <input formControlName="nombre" />
      <label>Especialidad:</label>
      <input formControlName="especialidad" />
      <label>Teléfono:</label>
      <input formControlName="telefono" />

      <button type="submit" [disabled]="form.invalid">
        {{ editMode ? 'Actualizar' : 'Guardar' }}
      </button>
      <button routerLink="/medicos">Cancelar</button>
    </form>
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
    private router: Router
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      nombre: ['', Validators.required],
      especialidad: ['', Validators.required],
      telefono: ['', Validators.required]
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
        alert(this.editMode ? 'Médico actualizado' : 'Médico creado');
        this.router.navigate(['/medicos']);
      },
      error: (err) => console.error(err)
    });
  }
}

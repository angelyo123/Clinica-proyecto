import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { CitaService } from '../../core/services/cita.service';
import { Cita } from '../../core/models/cita.model';

@Component({
  selector: 'app-cita-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <h2>{{ editMode ? 'Editar Cita' : 'Nueva Cita' }}</h2>

    <form *ngIf="rolCargado" [formGroup]="form" (ngSubmit)="guardar()">
      <label>Fecha y Hora:</label>
      <input type="datetime-local" formControlName="fechaHora" required />

      <label>Estado:</label>
      <input type="text" formControlName="estado" placeholder="Ej: Pendiente" required />

      <!-- Solo ADMIN ve estos campos -->
      <ng-container *ngIf="esAdmin">
        <label>ID Médico:</label>
        <input type="number" formControlName="medicoId" />

        <label>ID Paciente:</label>
        <input type="number" formControlName="pacienteId" />
      </ng-container>

      <!-- Si no es admin (paciente), los IDs se manejan automáticamente -->
      <ng-container *ngIf="!esAdmin">
        <input type="hidden" formControlName="medicoId" />
      </ng-container>

      <div class="acciones">
        <button type="submit" [disabled]="form.invalid">
          {{ editMode ? 'Actualizar' : 'Guardar' }}
        </button>
        <button type="button" (click)="cancelar()">Cancelar</button>
      </div>
    </form>

    <p *ngIf="!rolCargado">Cargando formulario...</p>
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

  constructor(
    private fb: FormBuilder,
    private citaService: CitaService,
    private route: ActivatedRoute,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.form = this.fb.group({
      fechaHora: ['', Validators.required],
      estado: ['Pendiente', Validators.required],
      medicoId: [''],
      pacienteId: ['']
    });

    this.detectarRolUsuario();

    // Modo edición
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      this.editMode = true;
      this.id = +idParam;
      this.citaService.obtener(this.id).subscribe(c => {
        this.form.patchValue({
          fechaHora: c.fechaHora,
          estado: c.estado,
          medicoId: c.medico?.id,
          pacienteId: c.paciente?.id
        });
      });
    }

    // Si viene ?medicoId=1 (paciente elige médico)
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
    this.cdr.detectChanges(); // 🔁 fuerza la actualización del DOM
  }


  
  guardar(): void {
    if (this.form.invalid) return;

    const cita: Cita = {
      fechaHora: this.form.value.fechaHora,
      estado: this.form.value.estado,
      medico: { id: this.form.value.medicoId }
    };

    if (this.esAdmin && this.form.value.pacienteId) {
      cita.paciente = { id: this.form.value.pacienteId };
    }

    const request = this.editMode
      ? this.citaService.actualizar(this.id, cita)
      : this.citaService.crearCita(cita);

    request.subscribe({
      next: () => {
        alert(this.editMode ? 'Cita actualizada correctamente' : 'Cita creada exitosamente');
        this.router.navigate(['/citas']);
      },
      error: (err) => {
        console.error('Error al guardar cita:', err);
        alert('Ocurrió un error al guardar la cita');
      }
    });
  }



  

  cancelar(): void {
    this.router.navigate(['/citas']);
  }
}

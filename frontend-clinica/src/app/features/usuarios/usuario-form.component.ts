import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UsuarioService } from '../../core/services/usuario.service';
import { Usuario } from '../../core/models/usuario.model';

@Component({
  selector: 'app-usuario-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  template: `
    <h2>{{ editMode ? 'Editar Usuario' : 'Nuevo Usuario' }}</h2>

    <form [formGroup]="form" (ngSubmit)="guardar()">
      <label>Username:</label>
      <input formControlName="username" />
      <div *ngIf="form.get('username')?.invalid && form.get('username')?.touched">
        El username es obligatorio.
      </div>

      <label>password:</label>
      <input formControlName="password" />
      <div *ngIf="form.get('password')?.invalid && form.get('password')?.touched">
        ingrese correctamente el password
      </div>

      <label>Rol:</label>
      <input formControlName="rol" />

      <button type="submit" [disabled]="form.invalid">
        {{ editMode ? 'Actualizar' : 'Guardar' }}
      </button>
      <button routerLink="/usuarios">Cancelar</button>
    </form>
  `
})
export class UsuarioFormComponent implements OnInit {
  form!: FormGroup;
  editMode = false;
  id!: number;

  constructor(
    private fb: FormBuilder,
    private UsuarioService: UsuarioService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    // ✅ ahora sí, fb ya existe
    this.form = this.fb.group({
      nombre: ['', Validators.required],
      dni: ['', [Validators.required, Validators.minLength(8)]],
      telefono: ['', Validators.required]
    });

    const paramId = this.route.snapshot.paramMap.get('id');
    if (paramId) {
      this.editMode = true;
      this.id = +paramId;
      this.UsuarioService.obtener(this.id).subscribe(p => this.form.patchValue(p));
    }
  }

  guardar(): void {
    if (this.form.invalid) return;

    // ✅ aseguramos el tipo Usuario correctamente
    const usuario: Usuario = {
      username: this.form.value.username ?? '',
      password: this.form.value.password ?? '',
      rol: this.form.value.rol ?? ''
    };

    const request = this.editMode
      ? this.UsuarioService.actualizar(this.id, usuario)
      : this.UsuarioService.crear(usuario);

    request.subscribe({
      next: () => {
        alert(this.editMode ? 'Usuario actualizado' : 'Usuario creado');
        this.router.navigate(['/usuarios']);
      },
      error: (err) => console.error('Error:', err)
    });
  }
}

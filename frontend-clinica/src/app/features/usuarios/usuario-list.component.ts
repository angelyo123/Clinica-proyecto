import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { Usuario } from '../../core/models/usuario.model';
import { UsuarioService } from '../../core/services/usuario.service';

@Component({
  selector: 'app-usuario-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <h2>Usuarios</h2>
    <button routerLink="/usuarios/nuevo">➕ Nuevo usuario</button>

    <div *ngIf="loading">Cargando...</div>
    <div *ngIf="error">{{ error }}</div>

    <table *ngIf="usuario.length" border="1" cellpadding="5">
      <thead>
        <tr>
          <th>ID</th>
          <th>username</th>
          <th>password</th>
          <th>rol</th>
        </tr>
      </thead>
      <tbody>
        <tr *ngFor="let p of usuario">
          <td>{{ p.id }}</td>
          <td>{{ p.username }}</td>
          <td>{{ p.password }}</td>
          <td>{{ p.rol }}</td>
          <td>
            <button [routerLink]="['/usuarios/editar', p.id]">✏️</button>
            <button (click)="eliminar(p.id!)">🗑️</button>
          </td>
        </tr>
      </tbody>
    </table>
  `
})
export class UsuarioListComponent implements OnInit {
  usuario: Usuario[] = [];
  loading = false;
  error = '';

  constructor(private usuarioService: UsuarioService, private router: Router) {}

  ngOnInit(): void {
    this.obtenerUsuario();
  }

  obtenerUsuario() {
    this.loading = true;
    this.usuarioService.listar().subscribe({
      next: (data) => {
        this.usuario = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Error al cargar usuarios';
        console.error(err);
        this.loading = false;
      }
    });
  }

  eliminar(id: number): void {
    if (confirm('¿Deseas eliminar este usuario?')) {
      this.usuarioService.eliminar(id).subscribe(() => this.obtenerUsuario());
    }
  }
}

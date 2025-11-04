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
    <div class="p-6 bg-gray-50 min-h-screen">
      <div class="flex justify-between items-center mb-6">
        <h2 class="text-3xl font-extrabold text-gray-900">👥 Gestión de Usuarios</h2>
        <button
          routerLink="/usuarios/nuevo"
          class="px-4 py-2 bg-indigo-600 text-white font-semibold rounded-lg shadow-md hover:bg-indigo-700 transition duration-300 ease-in-out focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 flex items-center space-x-2"
        >
          <span class="text-xl leading-none">➕</span>
          <span>Nuevo usuario</span>
        </button>
      </div>

      <div *ngIf="loading" class="p-4 mb-4 text-sm text-blue-700 bg-blue-100 rounded-lg" role="alert">
        <span class="font-medium">Cargando...</span> Por favor, espere.
      </div>

      <div *ngIf="error" class="p-4 mb-4 text-sm text-red-700 bg-red-100 rounded-lg" role="alert">
        <span class="font-medium">Error:</span> {{ error }}
      </div>

      <div *ngIf="usuario.length" class="overflow-x-auto shadow-xl rounded-lg">
        <table class="min-w-full divide-y divide-gray-200 bg-white">
          <thead class="bg-gray-100">
            <tr>
              <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                ID
              </th>
              <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Username
              </th>
              <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Password
              </th>
              <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                Rol
              </th>
              <th scope="col" class="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">
                Acciones
              </th>
            </tr>
          </thead>
          <tbody class="divide-y divide-gray-200">
            <tr *ngFor="let p of usuario" class="hover:bg-gray-50">
              <td class="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
                {{ p.id }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                {{ p.username }}
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                ********
              </td>
              <td class="px-6 py-4 whitespace-nowrap">
                <span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full"
                      [ngClass]="{
                          'bg-green-100 text-green-800': p.rol === 'admin',
                          'bg-yellow-100 text-yellow-800': p.rol === 'user',
                          'bg-indigo-100 text-indigo-800': p.rol !== 'admin' && p.rol !== 'user'
                      }">
                  {{ p.rol }}
                </span>
              </td>
              <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                <button
                  [routerLink]="['/usuarios/editar', p.id]"
                  title="Editar"
                  class="text-indigo-600 hover:text-indigo-900 mr-3 p-2 rounded-full hover:bg-indigo-100 transition duration-150"
                >
                  <span class="text-lg leading-none">✏️</span>
                </button>
                <button
                  (click)="eliminar(p.id!)"
                  title="Eliminar"
                  class="text-red-600 hover:text-red-900 p-2 rounded-full hover:bg-red-100 transition duration-150"
                >
                  <span class="text-lg leading-none">🗑️</span>
                </button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div *ngIf="!loading && !error && !usuario.length" class="text-center p-10 mt-6 bg-white rounded-lg shadow-md">
        <p class="text-xl font-semibold text-gray-700">No hay usuarios registrados.</p>
        <p class="mt-2 text-gray-500">Comienza añadiendo un nuevo usuario.</p>
      </div>
    </div>
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
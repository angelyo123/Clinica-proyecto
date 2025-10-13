import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { PacienteService } from '../../core/services/paciente.service';
import { Paciente } from '../../core/models/paciente.model';

@Component({
  selector: 'app-paciente-list',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <h2>Lista de Pacientes</h2>
    <button routerLink="/pacientes/nuevo">Nuevo Paciente</button>

    <table border="1" style="margin-top: 10px; width: 100%; border-collapse: collapse;">
      <thead>
        <tr>
          <th>ID</th>
          <th>Nombre</th>
          <th>DNI</th>
          <th>Teléfono</th>
          <th>Usuario</th>
          <th>Acciones</th>
        </tr>
      </thead>
      <tbody>
        <tr *ngFor="let p of pacientes">
          <td>{{ p.id }}</td>
          <td>{{ p.nombre }}</td>
          <td>{{ p.dni }}</td>
          <td>{{ p.telefono }}</td>
          <td>{{ p.usuario?.username }}</td>
          <td>
            <button [routerLink]="['/pacientes/editar', p.id]">Editar</button>
            <button (click)="eliminar(p.id!)">Eliminar</button>
          </td>
        </tr>
      </tbody>
    </table>

    <p *ngIf="!pacientes.length">No hay pacientes registrados.</p>
  `
})
export class PacienteListComponent implements OnInit {
  pacientes: Paciente[] = [];

  constructor(private pacienteService: PacienteService) {}

  ngOnInit(): void {
    this.cargarPacientes();
  }

  cargarPacientes(): void {
    this.pacienteService.listar().subscribe({
      next: data => (this.pacientes = data),
      error: err => console.error('Error al listar pacientes:', err)
    });
  }

  eliminar(id: number): void {
    if (confirm('¿Seguro que deseas eliminar este paciente?')) {
      this.pacienteService.eliminar(id).subscribe({
        next: () => {
          alert('Paciente eliminado');
          this.pacientes = this.pacientes.filter(p => p.id !== id);
        },
        error: err => console.error('Error al eliminar paciente:', err)
      });
    }
  }
}

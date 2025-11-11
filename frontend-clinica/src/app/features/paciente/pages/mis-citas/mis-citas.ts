import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Cita } from '../../../../core/models/cita.model';
import { CitaService } from '../../../../core/services/cita.service';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-mis-citas',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './mis-citas.html',
  styleUrls: ['./mis-citas.css']
})
export class MisCitas implements OnInit {

  citas: Cita[] = [];
  citasFiltradas: Cita[] = [];
  estados = ['PENDIENTE', 'CONFIRMADA', 'EN_PROCESO', 'COMPLETADA', 'CANCELADA'];

  // ⚡ Eliminamos la propiedad duplicada y usamos una variable privada
  private _estadoSeleccionado = 'PENDIENTE';

  loading = true;
  error = '';

  constructor(
    private citaService: CitaService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    const pacienteId = this.authService.getPacienteId();

    if (!pacienteId) {
      this.error = 'No se encontró el ID del paciente';
      this.loading = false;
      return;
    }

    this.citaService.listarDetallesPorPaciente(pacienteId).subscribe({
      next: (data) => {
        this.citas = data;
        this.aplicarFiltro();
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al obtener citas:', err);
        this.error = 'Error al cargar las citas del paciente';
        this.loading = false;
      }
    });
  }

  aplicarFiltro(): void {
    this.citasFiltradas = this.citas.filter(c => c.estado === this.estadoSeleccionado);
  }

  // ✅ Getter y Setter unificados
  set estadoSeleccionado(value: string) {
    this._estadoSeleccionado = value;
    this.aplicarFiltro();
  }
  get estadoSeleccionado(): string {
    return this._estadoSeleccionado;
  }

  cancelarCita(id: number): void {
    if (!confirm('¿Seguro que deseas cancelar esta cita?')) return;

    this.citaService.actualizarEstado(id, 'CANCELADA').subscribe({
      next: () => {
        const cita = this.citas.find(c => c.id === id);
        if (cita) cita.estado = 'CANCELADA';
        this.aplicarFiltro();
        alert('✅ Cita cancelada correctamente.');
      },
      error: (err) => {
        console.error('Error al cancelar cita:', err);
        alert('❌ No se pudo cancelar la cita.');
      }
    });
  }
}
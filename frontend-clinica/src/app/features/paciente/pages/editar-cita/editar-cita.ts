import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Cita } from '../../../../core/models/cita.model';
import { ActivatedRoute, Router } from '@angular/router';
import { CitaService } from '../../../../core/services/cita.service';
import { Horario } from '../../../../core/models/horario.model';
import { HorarioService } from '../../../../core/services/horario.service';

@Component({
  selector: 'app-editar-cita',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './editar-cita.html',
  styleUrls: ['./editar-cita.css']
})
export class EditarCita implements OnInit {
  cita!: Cita;
  horarios: Horario[] = [];
  horarioSeleccionado?: Horario;
  loading = true;
  mensaje = '';
  mensajeTipo: 'success' | 'error' | '' = '';

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private citaService: CitaService,
    private horarioService: HorarioService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!id) return;

    this.citaService.obtener(id).subscribe({
      next: (data) => {
        this.cita = data;
        this.cargarHorariosDelMedico(data.medico!.id);
      },
      error: (err) => {
        console.error('Error al obtener cita:', err);
        this.mensaje = '❌ No se pudo cargar la cita.';
        this.mensajeTipo = 'error';
        this.loading = false;
      }
    });
  }

  cargarHorariosDelMedico(medicoId: number) {
    this.horarioService.listarPorMedico(medicoId).subscribe({
      next: (data) => {
        this.horarios = data.filter(h => h.disponible === true);
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al obtener horarios:', err);
        this.mensaje = '❌ Error al cargar horarios del médico.';
        this.mensajeTipo = 'error';
        this.loading = false;
      }
    });
  }

  seleccionarHorario(h: Horario) {
    this.horarioSeleccionado = h;
  }

  confirmarCambio() {
    if (!this.horarioSeleccionado) {
      this.mensaje = 'Selecciona un nuevo horario.';
      this.mensajeTipo = 'error';
      return;
    }

    const citaActualizada: Cita = {
      ...this.cita,
      fechaCita: `${this.horarioSeleccionado.fechaInicio}T${this.horarioSeleccionado.horaInicio}`,
      idHorario: this.horarioSeleccionado.id
    };

    // 1️⃣ Liberar horario anterior
    this.horarioService.actualizarDisponibilidad(this.cita.idHorario!, true).subscribe({
      next: () => {
        // 2️⃣ Actualizar cita con el nuevo horario
        console.log('🩺 ID horario actual:', this.cita.idHorario);
        this.citaService.actualizar(this.cita.id!, citaActualizada).subscribe({
          next: () => {
            // 3️⃣ Bloquear nuevo horario
            this.horarioService.actualizarDisponibilidad(this.horarioSeleccionado!.id, false).subscribe();
            this.mensaje = '✅ Cita reprogramada correctamente.';
            this.mensajeTipo = 'success';
            setTimeout(() => this.router.navigate(['/paciente/mis-citas']), 2000);
          },
          error: (err) => {
            console.error('Error al reprogramar cita:', err);
            this.mensaje = '❌ No se pudo reprogramar la cita.';
            this.mensajeTipo = 'error';
          }
        });
      },
      error: (err) => {
        console.error('Error liberando horario anterior:', err);
      }
    });
  }
}
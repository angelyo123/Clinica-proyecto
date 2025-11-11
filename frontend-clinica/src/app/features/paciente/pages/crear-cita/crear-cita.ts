import { Component, OnInit } from '@angular/core';
import { Horario } from '../../../../core/models/horario.model';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { HorarioService } from '../../../../core/services/horario.service';
import { CitaService } from '../../../../core/services/cita.service';
import { AuthService } from '../../../../core/services/auth.service';
import { Cita } from '../../../../core/models/cita.model';

@Component({
  selector: 'app-crear-cita',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './crear-cita.html',
  styleUrls: ['./crear-cita.css']
})
export class CrearCita implements OnInit {
  medicoId!: number;
  horarios: Horario[] = [];
  horarioSeleccionado?: Horario;
  loading = true;
  error = '';
  mensaje = ''; // ✅ nuevo campo para feedback visual
  mensajeTipo: 'success' | 'error' | '' = ''; // ✅ tipo visual

  constructor(
    private route: ActivatedRoute,
    private horarioService: HorarioService,
    private citaService: CitaService,
    public authService: AuthService, // 👈 la hicimos pública
    private router: Router
  ) {}

  ngOnInit(): void {
    this.medicoId = Number(this.route.snapshot.queryParamMap.get('medicoId'));
    if (!this.medicoId) {
      this.error = 'No se ha especificado un médico válido.';
      this.loading = false;
      return;
    }

    this.horarioService.listarPorMedico(this.medicoId).subscribe({
      next: (data) => {
        this.horarios = data.filter(h => h.disponible === true);
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al obtener horarios:', err);
        this.error = 'Error al cargar los horarios del médico.';
        this.loading = false;
      }
    });
  }

  seleccionarHorario(h: Horario) {
    this.horarioSeleccionado = h;
    this.mensaje = '';
  }

  confirmarCita() {
    if (!this.horarioSeleccionado) {
      this.mensaje = 'Selecciona un horario primero.';
      this.mensajeTipo = 'error';
      return;
    }

    const pacienteId = this.authService.getPacienteId();
    if (!pacienteId) {
      this.mensaje = '❌ No se pudo identificar al paciente. Inicia sesión nuevamente.';
      this.mensajeTipo = 'error';
      return;
    }

    const cita: Cita = {
      fechaCita: `${this.horarioSeleccionado.fechaInicio}T${this.horarioSeleccionado.horaInicio}`,
      estado: 'PENDIENTE',
      medico: { id: this.medicoId },
      paciente: { id: pacienteId },
      idHorario: this.horarioSeleccionado.id
    };

    this.citaService.crearCita(cita).subscribe({
      next: () => {
        this.mensaje = '✅ Cita creada correctamente.';
        this.mensajeTipo = 'success';
        setTimeout(() => this.router.navigate(['/paciente/mis-citas']), 2000);
      },
      error: (err) => {
        console.error('Error al crear cita:', err);
        this.mensaje = '❌ No se pudo crear la cita. Inténtalo más tarde.';
        this.mensajeTipo = 'error';
      }
    });
  }
}

import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Paciente } from '../../../../core/models/paciente.model';
import { PacienteService } from '../../../../core/services/paciente.service';

@Component({
  selector: 'app-perfil-paciente',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './perfil-paciente.html',
  styleUrls: ['./perfil-paciente.css']
})
export class PerfilPaciente implements OnInit {

  paciente?: Paciente;
  cargando = true;
  error = '';

  constructor(private pacienteService: PacienteService) {}

  ngOnInit(): void {
    this.pacienteService.obtenerPerfil().subscribe({
      next: (data) => {
        this.paciente = data;
        this.cargando = false;
      },
      error: (err) => {
        console.error('Error al obtener perfil:', err);
        this.error = 'No se pudo cargar el perfil del paciente.';
        this.cargando = false;
      }
    });
  }
}
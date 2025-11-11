import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Medico } from '../../../../core/models/medico.model';
import { MedicoService } from '../../../../core/services/medico.service';

@Component({
  selector: 'app-medicos-disponibles',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './medicos-disponibles.html',
  styleUrls: ['./medicos-disponibles.css']
})
export class MedicosDisponibles implements OnInit {

  medicos: Medico[] = [];
  loading = true;
  error = '';

  constructor(private medicoService: MedicoService) {}

  ngOnInit(): void {
    this.medicoService.listarPublico().subscribe({
      next: (data) => {
        this.medicos = data;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error al cargar médicos:', err);
        this.error = 'No se pudieron cargar los médicos disponibles.';
        this.loading = false;
      }
    });
  }
}
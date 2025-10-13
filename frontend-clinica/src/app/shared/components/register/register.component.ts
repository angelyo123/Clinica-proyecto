import { Component } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {
  form!: FormGroup;
  tipo = 'paciente';

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.form = this.fb.group({
      nombre: ['', Validators.required],
      username: ['', Validators.required],
      password: ['', Validators.required],
      dni: [''],
      telefono: [''],
      especialidad: ['']
    });
  }

  onSubmit() {

    localStorage.removeItem('token');

    const data = {
      ...this.form.value,
      usuario: {
        username: this.form.value.username,
        password: this.form.value.password
      }
    };

    if (this.tipo === 'paciente') {
      this.authService.registerPaciente(data).subscribe({
        next: () => {
          alert('Paciente registrado correctamente');
          this.router.navigate(['/login']);
        },
        error: () => alert('Error al registrar paciente')
      });
    } else {
      this.authService.registerMedico(data).subscribe({
        next: () => {
          alert('Médico registrado correctamente');
          this.router.navigate(['/login']);
        },
        error: () => alert('Error al registrar médico')
      });
    }
  }
}
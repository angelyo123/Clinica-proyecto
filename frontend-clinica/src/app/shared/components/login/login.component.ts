import { Component } from '@angular/core';
//C:\Users\macaa\Clinica-proyecto\frontend-clinica\src\app\core\services\auth.service.ts

import { AuthService } from '../../../core/services/auth.service'

import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [FormsModule, CommonModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']

})
export class LoginComponent  {

  username: string = '';
  password: string = '';

  constructor(private authService: AuthService,
    private router: Router
  ) {}

  onLogin() {

    localStorage.removeItem('token');

    this.authService.login(this.username, this.password).subscribe({
      next: (res) => {
        this.authService.saveToken(res.token);
        const roles = this.authService.getUserRoles();

        if (roles.includes('ROLE_ADMIN')) this.router.navigate(['/dashboard']);
        else if (roles.includes('ROLE_MEDICO')) this.router.navigate(['/citas']);
        else if (roles.includes('ROLE_PACIENTE')) this.router.navigate(['/medicos']); // 👈 ver médicos para pedir cita
        else this.router.navigate(['/login']);

      },
      error: () => alert('Credenciales incorrectas')
    });
  }

}

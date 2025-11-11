import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-paciente-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink],
  template: `
    <!-- Navbar -->
    <nav class="bg-gradient-to-r from-purple-500 to-indigo-500 text-white px-6 py-3 flex justify-between items-center shadow-md">
      <div class="font-bold text-lg tracking-tight">🏥 Clínica Virtual</div>

      <ul class="flex gap-6 text-sm font-medium">
        <li>
          <a routerLink="perfil" routerLinkActive="link-activo" class="link">👤 Perfil</a>
        </li>
        <li>
          <a routerLink="mis-citas" routerLinkActive="link-activo" class="link">📅 Mis Citas</a>
        </li>
        <li>
          <a routerLink="medicos-disponibles" routerLinkActive="link-activo" class="link">👨‍⚕️ Médicos</a>
        </li>
      </ul>

      <button 
        class="btn-logout"
        (click)="logout()">
        Cerrar sesión
      </button>
    </nav>

    <!-- Contenido dinámico -->
    <div class="p-6 bg-[#F5F6FA] min-h-screen">
      <router-outlet></router-outlet>
    </div>
  `,
  styles: [`
    /* 🎨 Enlaces de navegación */
    .link {
      position: relative;
      color: #F9FAFB;
      text-decoration: none;
      padding-bottom: 2px;
      transition: color 0.2s ease, opacity 0.2s ease;
    }
    .link:hover {
      opacity: 0.85;
    }

    /* ✨ Subrayado activo */
    .link-activo {
      font-weight: 600;
    }
    .link-activo::after {
      content: '';
      position: absolute;
      bottom: -3px;
      left: 0;
      width: 100%;
      height: 2px;
      background-color: white;
      border-radius: 2px;
    }

    /* 💜 Botón Cerrar sesión */
    .btn-logout {
      background-color: #F9FAFB;
      color: #7C3AED;
      font-weight: 600;
      padding: 0.4rem 1rem;
      border-radius: 8px;
      transition: all 0.25s ease;
      box-shadow: 0 2px 4px rgba(124,58,237,0.25);
      border: 1px solid transparent;
    }
    .btn-logout:hover {
      background-color: #EDE9FE;
      box-shadow: 0 3px 8px rgba(124,58,237,0.3);
      transform: translateY(-1px);
    }
  `]
})
export class PacienteLayout {
  logout() {
    localStorage.removeItem('token');
    window.location.href = '/login';
  }
}

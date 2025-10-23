import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';


@Component({
  selector: 'app-navbar',
  standalone: true,
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit {
  roles: string[] = [];
  isAdmin = false;
  isMedico = false;
  isPaciente = false;

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.roles = this.authService.getUserRoles();
    this.isAdmin = this.roles.includes('ROLE_ADMIN');
    this.isMedico = this.roles.includes('ROLE_MEDICO');
    this.isPaciente = this.roles.includes('ROLE_PACIENTE');
  }

  logout() {
    this.authService.logout();
  }
}
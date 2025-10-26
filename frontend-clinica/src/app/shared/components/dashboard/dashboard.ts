import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router'; // ✅ Importar Router

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html',
  styleUrls: ['./dashboard.css']
})
export class Dashboard {
  data: any;

  constructor(private http: HttpClient,
              private router: Router) {} // ✅ Inyectar Router

  loadData() {
    this.http.get('http://localhost:8080/admin/dashboard').subscribe({
      next: res => this.data = res,
      error: err => console.error('Error:', err)
    });
  }

  logout() {
    localStorage.removeItem('token');
    this.router.navigate(['/login']); // Mejor que usar window.location.href
  }

  goTo(path: string) {
    this.router.navigate([`/${path}`]); // Ahora funcionará
  }
}

import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.html',   // ✅ usar archivo externo
  styleUrls: ['./dashboard.css']     // opcional, si existe
})
export class Dashboard {
  data: any;

  constructor(private http: HttpClient) {}

  loadData() {
    this.http.get('http://localhost:8080/admin/dashboard').subscribe({
      next: res => this.data = res,
      error: err => console.error('Error:', err)
    });
  }

  logout() {
  localStorage.removeItem('token');
  window.location.href = '/login';
}

}

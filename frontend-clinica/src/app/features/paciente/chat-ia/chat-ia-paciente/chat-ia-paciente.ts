import { CommonModule } from '@angular/common';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-chat-ia-paciente',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './chat-ia-paciente.html',
  styleUrls: ['./chat-ia-paciente.css']
})
export class ChatIaPaciente {
  mensajes: { remitente: 'paciente' | 'ia'; texto: string }[] = [];
  mensajeUsuario = '';
  cargando = false;

  constructor(private http: HttpClient, private authService: AuthService) {}

  enviarMensaje(): void {
    const mensaje = this.mensajeUsuario.trim();
    if (!mensaje) return;

    const pacienteId = this.authService.getPacienteId();
    if (!pacienteId) {
      alert('Error: no se pudo identificar al paciente.');
      return;
    }

    // Mostrar el mensaje del paciente
    this.mensajes.push({ remitente: 'paciente', texto: mensaje });
    this.mensajeUsuario = '';
    this.cargando = true;

    const body = { pacienteId, mensaje };
    const headers = new HttpHeaders({ 'Content-Type': 'application/json' });

    this.http.post<any>('http://localhost:8086/ia/conversar', body, { headers }).subscribe({
      next: (res) => {
  let textoIA = res?.mensaje || '🤖 La IA no respondió.';
  
      // 💡 Limpia el JSON del texto y deja solo el contenido legible
      try {
        const start = textoIA.indexOf('"respuesta":');
        if (start !== -1) {
          const match = textoIA.match(/"respuesta":\s*"(.*?)"/s);
          if (match && match[1]) {
            textoIA = match[1].replace(/\\n/g, '\n').replace(/\\"/g, '"');
          }
        }
      } catch (_) {}

      this.mensajes.push({ remitente: 'ia', texto: textoIA });
      this.cargando = false;
},
      error: (err) => {
        console.error('Error conversando con la IA:', err);
        this.mensajes.push({ remitente: 'ia', texto: '⚠️ Error al comunicarse con el asistente. Inténtalo más tarde.' });
        this.cargando = false;
      }
    });
  }
}
import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';;

@Component({
  selector: 'app-paciente-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
  <div class="flex flex-col h-[85vh] max-w-3xl mx-auto bg-white rounded-xl shadow-md border border-gray-200">
    
    <div class="bg-blue-600 text-white text-lg font-semibold px-4 py-3 rounded-t-xl">
      💬 Asistente Médico IA
    </div>

    <div class="flex-1 overflow-y-auto p-4 space-y-3 bg-gray-50" #chatContainer>
      <div *ngFor="let msg of mensajes">
        <div *ngIf="msg.origen === 'paciente'" class="flex justify-end">
          <div class="bg-blue-500 text-white px-4 py-2 rounded-2xl max-w-[75%]">
            {{ msg.texto }}
          </div>
        </div>
        <div *ngIf="msg.origen === 'ia'" class="flex justify-start">
          <div class="bg-gray-200 text-gray-800 px-4 py-2 rounded-2xl max-w-[75%]">
            {{ msg.texto }}
          </div>
        </div>
      </div>
    </div>

    <form (ngSubmit)="enviarMensaje()" class="flex border-t border-gray-300">
      <input [(ngModel)]="mensaje" name="mensaje" 
             placeholder="Escribe tu mensaje..." 
             class="flex-1 px-4 py-3 focus:outline-none text-gray-700" />
      <button type="submit"
              class="bg-blue-600 hover:bg-blue-700 text-white font-medium px-6 transition rounded-r-xl">
        Enviar
      </button>
    </form>
  </div>
  `
})
export class PacienteChatComponent {
  mensajes: { origen: 'paciente' | 'ia'; texto: string }[] = [];
  mensaje: string = '';
  apiUrl = 'http://localhost:8086/ia/conversar'; // tu backend

  constructor(private http: HttpClient) {}

  enviarMensaje() {
    const texto = this.mensaje.trim();
    if (!texto) return;

    // Mostrar el mensaje del paciente
    this.mensajes.push({ origen: 'paciente', texto });
    this.mensaje = '';

    const payload = {
      pacienteId: 2, // puedes hacerlo dinámico según el usuario
      mensaje: texto
    };

    this.http.post<{ mensaje?: string }>(this.apiUrl, payload).subscribe({
      next: (res) => {
        const respuesta = res?.mensaje || '🤖 (sin respuesta del servidor)';
        this.mensajes.push({ origen: 'ia', texto: respuesta });
      },
      error: (err) => {
        console.error('Error en conversación:', err);
        this.mensajes.push({ origen: 'ia', texto: '⚠️ Error al conectar con el servidor' });
      }
    });
  }
}
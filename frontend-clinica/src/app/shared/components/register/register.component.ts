import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';
import { Router, RouterLink } from '@angular/router'; 
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink], 
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent implements OnInit {
  form!: FormGroup;
  tipo = 'paciente'; 

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {}

   
   
    public onlyNumbers(event: KeyboardEvent): boolean {
        
        const isControlKey = event.key === 'Backspace' || event.key === 'Delete' || event.key.startsWith('Arrow') || event.key === 'Tab';
        if (isControlKey) {
            return true;
        }

        const char = event.key;
        
        const isDigit = /^\d$/.test(char); 
        
        return isDigit;
    }

    
    
    public onlyLetters(event: KeyboardEvent): boolean {
        
        const isControlKey = event.key === 'Backspace' || event.key === 'Delete' || event.key.startsWith('Arrow') || event.key === 'Tab';
        if (isControlKey) {
            return true;
        }

        const char = event.key;
       
        const isLetter = /^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]$/.test(char); 
        
        return isLetter;
    }

    

    ngOnInit(): void {
        
        const soloLetrasRegEx = /^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/;
        const soloNumerosRegEx = /^[0-9]+$/;

       
        this.form = this.fb.group({
            
            nombre: ['', [
                Validators.required, 
                Validators.pattern(soloLetrasRegEx)
            ]],
            
            username: ['', Validators.required], 
            password: ['', Validators.required], 
            
            
            dni: ['', [
                Validators.required, 
                Validators.minLength(8), 
                Validators.maxLength(8), 
                Validators.pattern(soloNumerosRegEx)
            ]],
            
            
            telefono: ['', [
                Validators.required, 
                Validators.minLength(9), 
                Validators.maxLength(9), 
                Validators.pattern(soloNumerosRegEx)
            ]],
            
            
            especialidad: [''] 
        });

        this.form.get('dni')?.valueChanges.subscribe(dniValue => {
            if (this.tipo === 'paciente') {
                this.form.get('username')?.setValue(dniValue);
            }
        });

        
        this.onTipoChange(); 
    }

  onTipoChange() {
    const usernameControl = this.form.get('username');
    const passwordControl = this.form.get('password');
    const dniControl = this.form.get('dni');
    const telefonoControl = this.form.get('telefono');
    const especialidadControl = this.form.get('especialidad');

    if (this.tipo === 'paciente') {
      
      dniControl?.setValidators(Validators.required);
      telefonoControl?.setValidators(Validators.required);
      
      usernameControl?.setValue(this.form.get('dni')?.value || ''); 
   
      passwordControl?.setValue('1234'); 
   
      
      
      especialidadControl?.clearValidators();
      especialidadControl?.setValue('');
    } else { 
      
      usernameControl?.enable();
      usernameControl?.setValue('');
      usernameControl?.setValidators(Validators.required);
      passwordControl?.enable();
      passwordControl?.setValue('');
      passwordControl?.setValidators([Validators.required, Validators.minLength(6)]);

      
      dniControl?.clearValidators();
      dniControl?.setValue('');
      telefonoControl?.clearValidators();
      telefonoControl?.setValue('');
      
      
      especialidadControl?.setValidators(Validators.required);
    }

    usernameControl?.updateValueAndValidity();
    passwordControl?.updateValueAndValidity();
    dniControl?.updateValueAndValidity();
    telefonoControl?.updateValueAndValidity();
    especialidadControl?.updateValueAndValidity();
  }

 
  onSubmit() {
    if (this.form.invalid) {
        alert('Por favor, completa todos los campos requeridos.');
        return;
    }

    const formData = this.form.getRawValue();

    
    let data: any = {
        
        nombre: formData.nombre,
        
        
        username: formData.username, 
        password: formData.password
    };

    if (this.tipo === 'paciente') {
       
        data = {
            ...data,
            dni: formData.dni,
            telefono: formData.telefono
        };

        this.authService.registerPaciente(data).subscribe({
            next: () => this.handleSuccess('Paciente'),
            error: (err) => this.handleError('paciente', err)
        });

    } else { 
        
        data = {
            ...data,
            especialidad: formData.especialidad
        };

        this.authService.registerMedico(data).subscribe({
            next: () => this.handleSuccess('Médico'),
            error: (err) => this.handleError('médico', err)
        });
    }
}

  private handleSuccess(tipo: string) {
    alert(`${tipo} registrado correctamente`);
    this.router.navigate(['/login']);
  }

  private handleError(tipo: string, error: any) {
    console.error(`Error al registrar ${tipo}:`, error);
    alert(`Error al registrar ${tipo}. Inténtalo de nuevo.`);
  }
}
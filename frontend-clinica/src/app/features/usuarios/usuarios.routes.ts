import { Routes } from '@angular/router';
import { UsuarioListComponent } from './usuario-list.component';
import { UsuarioFormComponent } from './usuario-form.component';
import { AuthGuard } from '../../core/guards/auth.guard';

export const USUARIOS_ROUTES: Routes = [
  { path: '', component: UsuarioListComponent, canActivate: [AuthGuard] },
  { path: 'nuevo', component: UsuarioFormComponent, canActivate: [AuthGuard] },
  { path: 'editar/:id', component: UsuarioFormComponent, canActivate: [AuthGuard] },
];

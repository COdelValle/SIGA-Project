import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { ServicioAutenticacion } from '../servicios/servicio-autenticacion';
import { USUARIOS_SIMULADOS, UsuarioSimulado } from '../datos-prueba/usuarios-simulados';

@Component({
  selector: 'app-inicio-sesion',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './inicio-sesion.html',
  styleUrl: './inicio-sesion.css'
})
export class InicioSesionComponent {
  usuariosSimulados = USUARIOS_SIMULADOS;
  cargando = false;
  error = '';

  constructor(
    private servicioAuth: ServicioAutenticacion,
    private router: Router
  ) {}

  iniciarSesionConAzure() {
    this.cargando = true;
    this.error = '';
    this.servicioAuth.iniciarSesion().subscribe({
      next: (resultado) => {
        this.cargando = false;
        console.log('Sesión iniciada correctamente:', resultado);
        // Redirigir a la vista privada o panel principal
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.cargando = false;
        this.error = 'No se pudo completar el inicio de sesión con Azure AD.';
        console.error(err);
      }
    });
  }

  seleccionarUsuarioSimulado(usuario: UsuarioSimulado) {
    alert(`Modo prueba activado para: ${usuario.nombre} (${usuario.rol})`);
    this.router.navigate(['/dashboard']);
  }
}
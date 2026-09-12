export interface UsuarioSimulado {
  nombre: string;
  correo: string;
  rol: 'Administrador' | 'Docente' | 'Estudiante' | 'Apoderado';
}

export const USUARIOS_SIMULADOS: UsuarioSimulado[] = [
  {
    nombre: 'Administrador Prueba',
    correo: 'admin@genesisfloress.onmicrosoft.com',
    rol: 'Administrador'
  },
  {
    nombre: 'Docente Prueba',
    correo: 'docente@genesisfloress.onmicrosoft.com',
    rol: 'Docente'
  },
  {
    nombre: 'Estudiante Prueba',
    correo: 'estudiante@genesisfloress.onmicrosoft.com',
    rol: 'Estudiante'
  },
  {
    nombre: 'Apoderado Prueba',
    correo: 'apoderado@genesisfloress.onmicrosoft.com',
    rol: 'Apoderado'
  }
];
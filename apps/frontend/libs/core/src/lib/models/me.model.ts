import { Rol } from './role.enum';

export interface EstudianteVinculado {
  id: number;
  nombre: string;
  relacion: string;
}

export interface CursoResumen {
  id: number;
  nombre: string;
}

export interface Me {
  id: string;
  email: string;
  displayName: string;
  roles: Rol[];
  estudiantesVinculados?: EstudianteVinculado[];
  cursos?: CursoResumen[];
}

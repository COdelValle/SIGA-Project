/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.2.1263 on 2026-10-06 19:12:50.

export interface ClaseOpcionDTO {
    id: number;
    nivel: string;
    letra: string;
    anioAcademico: number;
}

export interface EstudianteOpcionDTO {
    id: number;
    rut: string;
    firstName: string;
    firstSurname: string;
}

export interface UsuarioDetalleDTO {
    id: string;
    fullName: string;
    email: string;
    rol: string;
    estado: string;
    rut: string;
    fechaNacimiento: string;
    detalle: string;
    etiquetas: string[];
}

export interface PerfilEstudianteResponseDTO {
    id: number;
    idUsuario: string;
    rut: string;
    firstName: string;
    middleName: string;
    firstSurname: string;
    secondSurname: string;
    birthDate: Date;
    allergies: string[];
    state: string;
    idClase: number;
    clase: ClaseDetalleDTO;
    asignaturas: AsignaturaDetalleDTO[];
}

export interface MeResponseDTO {
    id: string;
    email: string;
    displayName: string;
    roles: Rol[];
}

export interface ClaseDetalleDTO {
    id: number;
    nivel: string;
    letra: string;
    anioAcademico: number;
}

export interface AsignaturaDetalleDTO {
    id: number;
    idAsignatura: number;
    name: string;
    description: string;
    area: AreaAcademica;
    caracter: CaracterAsignatura;
    calificable: boolean;
    idDocente: number;
    docente: string;
    horarios: HorarioDetalleDTO[];
    evaluaciones: EvaluacionDetalleDTO[];
}

export interface HorarioDetalleDTO {
    id: number;
    dia: string;
    horarioEntrada: string;
    horarioSalida: string;
    ubicacion: string;
}

export interface EvaluacionDetalleDTO {
    id: number;
    nombre: string;
    tipo: string;
    ponderacion: number;
    nota: number;
}

export type Rol = "ADMIN" | "DOCENTE" | "APODERADO" | "ESTUDIANTE";

export type AreaAcademica = "Matemáticas" | "Ciencias Naturales y Exactas" | "Ciencias para la Ciudadanía" | "Lenguaje y Comunicación" | "Historia y Ciencias Sociales" | "Formación Ciudadana" | "Filosofía" | "Lenguas e Idiomas" | "Artes y Música" | "Educación Física" | "Tecnología e Informática" | "Orientación" | "Religión" | "Economía y Finanzas" | "Otra Área";

export type CaracterAsignatura = "OBLIGATORIA" | "OPTATIVA" | "ELECTIVA";

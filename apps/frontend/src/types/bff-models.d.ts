/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.2.1263 on 2026-10-03 20:26:53.

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
    name: string;
    description: string;
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

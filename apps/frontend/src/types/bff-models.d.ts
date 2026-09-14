/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.2.1263 on 2026-09-12 16:46:19.

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
    asignaturas: AsignaturaDetalleDTO[];
}

export interface MeResponseDTO {
    id: string;
    email: string;
    displayName: string;
    roles: Rol[];
}

export interface AsignaturaDetalleDTO {
    id: number;
    name: string;
    description: string;
    notas: NotaDetalleDTO[];
}

export interface NotaDetalleDTO {
    id: number;
    score: number;
}

export type Rol = "ADMIN" | "DOCENTE" | "APODERADO" | "ESTUDIANTE";

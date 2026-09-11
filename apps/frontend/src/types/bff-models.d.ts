/* tslint:disable */
/* eslint-disable */
// Generated using typescript-generator version 3.2.1263 on 2026-09-11 20:32:04.

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

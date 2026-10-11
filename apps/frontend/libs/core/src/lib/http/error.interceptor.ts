import { HttpInterceptorFn } from '@angular/common/http';

/**
 * Punto unico de manejo de errores HTTP del frontend. Los errores se propagan
 * tal cual para que los flujos de sesion (authInterceptor + App) decidan que
 * mostrar; antes redirigia a `/` ante un 401 y competia con el manejo real.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => next(req);

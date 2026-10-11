/**
 * Contrato de paginacion del backend (Spring Data `Page<T>`).
 */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  /** Pagina actual (0-indexada, como Spring Data). */
  number: number;
  size: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface PageQuery {
  /** Pagina solicitada (0-indexada). */
  page?: number;
  size?: number;
  /** Orden, por ejemplo "id,asc". */
  sort?: string;
}

export const PAGE_SIZE_DEFAULT = 20;
export const PAGE_SIZE_MAX = 100;

/** Parametros de query para un endpoint paginado del backend. */
export function pageQueryParams(query: PageQuery = {}): Record<string, string> {
  const params: Record<string, string> = {
    page: String(query.page ?? 0),
    size: String(Math.min(query.size ?? PAGE_SIZE_DEFAULT, PAGE_SIZE_MAX)),
  };
  if (query.sort) {
    params['sort'] = query.sort;
  }
  return params;
}

/** Construye un `Page<T>` en cliente (util mientras las vistas usan mocks). */
export function toPage<T>(items: T[], page = 0, size = PAGE_SIZE_DEFAULT): Page<T> {
  const safeSize = Math.max(1, Math.min(size, PAGE_SIZE_MAX));
  const total = items.length;
  const totalPages = Math.max(1, Math.ceil(total / safeSize));
  const safePage = Math.min(Math.max(0, page), totalPages - 1);
  const content = items.slice(safePage * safeSize, safePage * safeSize + safeSize);
  return {
    content,
    totalElements: total,
    totalPages,
    number: safePage,
    size: safeSize,
    numberOfElements: content.length,
    first: safePage === 0,
    last: safePage >= totalPages - 1,
    empty: content.length === 0,
  };
}

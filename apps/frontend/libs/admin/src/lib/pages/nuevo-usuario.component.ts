import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, computed, inject, output, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import {
  AREAS_ACADEMICAS,
  ClaseOpcion,
  CredencialTemporal,
  EstudianteOpcion,
  PARENTESCOS,
  RegistroUsuarioEstado,
  RegistroUsuarioPayload,
  RolRegistrable,
  correoInstitucionalSugerido,
  esEstadoFinal,
  etiquetaClase,
  etiquetaEstudiante,
  formatearRutEntrada,
  nombreCompletoSugerido,
  normalizarNombrePropio,
} from '@siga/mocks';
import { copiarAlPortapapeles } from '../utils/clipboard';

const CAMPOS_NOMBRE = ['firstName', 'middleName', 'firstSurname', 'secondSurname'];
import {
  Subscription,
  debounceTime,
  distinctUntilChanged,
  interval,
  of,
  startWith,
  switchMap,
  takeWhile,
} from 'rxjs';
import { AdminService } from '../state/admin.service';
import { CredencialTemporalComponent } from '../components/credencial-temporal.component';

interface CertificadoForm {
  nombre: string;
  institucionRealizacion: string;
  fechaTitulacion: string;
}

interface VinculoForm {
  idEstudiante: string;
  parentesco: string;
  etiqueta: string;
}

interface FormularioRegistro {
  contactEmail: string;
  requestedRole: RolRegistrable;
  estudiante: {
    firstName: string;
    middleName: string;
    firstSurname: string;
    secondSurname: string;
    rut: string;
    birthDate: string;
    allergies: string;
    idClase: string;
  };
  docente: {
    firstName: string;
    middleName: string;
    firstSurname: string;
    secondSurname: string;
    rut: string;
    fechaContratacion: string;
    area: string;
    certificados: CertificadoForm[];
  };
  apoderado: {
    firstName: string;
    middleName: string;
    firstSurname: string;
    secondSurname: string;
    rut: string;
    telefonos: string;
    estudiantes: VinculoForm[];
  };
}

const FORM_INICIAL: FormularioRegistro = {
  contactEmail: '',
  requestedRole: 'ESTUDIANTE',
  estudiante: {
    firstName: '',
    middleName: '',
    firstSurname: '',
    secondSurname: '',
    rut: '',
    birthDate: '',
    allergies: '',
    idClase: '',
  },
  docente: {
    firstName: '',
    middleName: '',
    firstSurname: '',
    secondSurname: '',
    rut: '',
    fechaContratacion: '',
    area: AREAS_ACADEMICAS[0],
    certificados: [{ nombre: '', institucionRealizacion: '', fechaTitulacion: '' }],
  },
  apoderado: {
    firstName: '',
    middleName: '',
    firstSurname: '',
    secondSurname: '',
    rut: '',
    telefonos: '',
    estudiantes: [],
  },
};

/**
 * Formulario de registro compuesto (usuario + perfil de rol). Envía la
 * solicitud al BFF, hace polling del proceso y permite obtener la credencial
 * temporal una única vez cuando la cuenta queda aprovisionada.
 */
@Component({
  selector: 'siga-admin-nuevo-usuario',
  imports: [CredencialTemporalComponent],
  template: `
    <section class="rounded-2xl bg-panel p-5 shadow-lg">
      <div class="flex items-start justify-between gap-4">
        <div>
          <h2 class="text-lg font-semibold text-heading">Registrar usuario</h2>
          <p class="text-sm text-muted">
            Crea la cuenta en Microsoft Entra ID y el perfil del rol de forma asíncrona.
          </p>
        </div>
        <button
          type="button"
          (click)="cerrar.emit()"
          class="rounded-lg border border-line px-3 py-1 text-sm text-muted transition hover:text-ink"
        >
          Cerrar
        </button>
      </div>

      @if (mensaje()) {
        <p class="mt-4 rounded-xl border border-gold/60 bg-surface px-4 py-3 text-sm text-ink">
          {{ mensaje() }}
        </p>
      }

      @if (!estado()) {
        <form class="mt-4 flex flex-col gap-4" (submit)="enviar($event)">
          <div class="grid gap-3 sm:grid-cols-2">
            <label class="flex flex-col gap-1 text-sm">
              <span class="text-muted">Correo de contacto (opcional)</span>
              <input
                type="email"
                [value]="formulario().contactEmail"
                (input)="actualizar('contactEmail', $event)"
                [class]="inputClass"
                placeholder="correo personal para futuras notificaciones"
              />
            </label>
            <label class="flex flex-col gap-1 text-sm">
              <span class="text-muted">Rol *</span>
              <select [value]="formulario().requestedRole" (change)="cambiarRol($event)" [class]="inputClass">
                <option value="ESTUDIANTE">Estudiante</option>
                <option value="DOCENTE">Docente</option>
                <option value="APODERADO">Apoderado</option>
              </select>
            </label>
          </div>

          <div class="flex flex-col gap-3 rounded-xl border border-line bg-surface p-3">
            <span class="text-sm font-semibold text-heading">Datos de la cuenta (automáticos)</span>
            <label class="flex flex-col gap-1 text-sm">
              <span class="text-muted">Nombre completo</span>
              <span class="flex gap-2">
                <input
                  readonly
                  [value]="nombreCompletoPreview()"
                  placeholder="Se genera con los nombres y apellidos"
                  [class]="inputClass + ' flex-1'"
                />
                <button
                  type="button"
                  (click)="copiar(nombreCompletoPreview(), 'nombre')"
                  [disabled]="!nombreCompletoPreview()"
                  class="rounded-lg border border-brand px-3 py-2 text-xs font-semibold text-brand transition hover:bg-brand/20 disabled:opacity-40"
                >
                  {{ copiado() === 'nombre' ? 'Copiado' : 'Copiar' }}
                </button>
              </span>
            </label>
            <label class="flex flex-col gap-1 text-sm">
              <span class="text-muted">Correo que se generará</span>
              <span class="flex gap-2">
                <input
                  readonly
                  [value]="correoPreview()"
                  placeholder="nombre.apellido@platformsiga.onmicrosoft.com"
                  [class]="inputClass + ' flex-1'"
                />
                <button
                  type="button"
                  (click)="copiar(correoPreview(), 'correo')"
                  [disabled]="!correoPreview()"
                  class="rounded-lg border border-brand px-3 py-2 text-xs font-semibold text-brand transition hover:bg-brand/20 disabled:opacity-40"
                >
                  {{ copiado() === 'correo' ? 'Copiado' : 'Copiar' }}
                </button>
              </span>
            </label>
            <p class="text-xs text-muted">
              El correo se genera con la primera palabra del nombre y del primer apellido; si ya está en
              uso se agrega el segundo apellido o un número.
            </p>
          </div>

          @if (formulario().requestedRole === 'ESTUDIANTE') {
            <fieldset class="grid gap-3 sm:grid-cols-2">
              <legend class="col-span-full text-sm font-semibold text-heading">Datos del estudiante</legend>
              <label class="flex flex-col gap-1 text-sm">
                <span class="text-muted">Nombres *</span>
                <input type="text" required [value]="formulario().estudiante.firstName"
                  (input)="actualizarEstudiante('firstName', $event)"
                  (keydown)="onNombreKeydown('estudiante', 'firstName', $event)" [class]="inputClass" />
              </label>
              <label class="flex flex-col gap-1 text-sm">
                <span class="text-muted">Segundo nombre</span>
                <input type="text" [value]="formulario().estudiante.middleName"
                  (input)="actualizarEstudiante('middleName', $event)"
                  (keydown)="onNombreKeydown('estudiante', 'middleName', $event)" [class]="inputClass" />
              </label>
              <label class="flex flex-col gap-1 text-sm">
                <span class="text-muted">Primer apellido *</span>
                <input type="text" required [value]="formulario().estudiante.firstSurname"
                  (input)="actualizarEstudiante('firstSurname', $event)"
                  (keydown)="onNombreKeydown('estudiante', 'firstSurname', $event)" [class]="inputClass" />
              </label>
              <label class="flex flex-col gap-1 text-sm">
                <span class="text-muted">Segundo apellido</span>
                <input type="text" [value]="formulario().estudiante.secondSurname"
                  (input)="actualizarEstudiante('secondSurname', $event)"
                  (keydown)="onNombreKeydown('estudiante', 'secondSurname', $event)" [class]="inputClass" />
              </label>
              <label class="flex flex-col gap-1 text-sm">
                <span class="text-muted">RUT *</span>
                <input type="text" required maxlength="12" [value]="formulario().estudiante.rut"
                  (input)="actualizarEstudiante('rut', $event)" [class]="inputClass" placeholder="12.345.678-9" />
              </label>
              <label class="flex flex-col gap-1 text-sm">
                <span class="text-muted">Fecha de nacimiento *</span>
                <input type="date" required [value]="formulario().estudiante.birthDate"
                  (input)="actualizarEstudiante('birthDate', $event)" [class]="inputClass" />
              </label>
              <label class="flex flex-col gap-1 text-sm">
                <span class="text-muted">Alergias (separadas por coma)</span>
                <input type="text" [value]="formulario().estudiante.allergies"
                  (input)="actualizarEstudiante('allergies', $event)" [class]="inputClass" />
              </label>
              <label class="flex flex-col gap-1 text-sm">
                <span class="text-muted">Clase * ({{ anioActual }})</span>
                <select required [value]="formulario().estudiante.idClase"
                  (change)="actualizarEstudiante('idClase', $event)" [class]="inputClass">
                  <option value="">Selecciona una clase</option>
                  @for (clase of clases(); track clase.id) {
                    <option [value]="clase.id">{{ etiquetaDeClase(clase) }}</option>
                  }
                </select>
                @if (cargandoClases()) {
                  <span class="text-xs text-muted">Cargando clases…</span>
                }
                @if (errorClases()) {
                  <span class="text-xs text-red-600">{{ errorClases() }}</span>
                }
                @if (!cargandoClases() && !errorClases() && clases().length === 0) {
                  <span class="text-xs text-muted">No hay clases registradas para {{ anioActual }}.</span>
                }
              </label>
            </fieldset>
          }

          @if (formulario().requestedRole === 'DOCENTE') {
            <fieldset class="grid gap-3 sm:grid-cols-2">
              <legend class="col-span-full text-sm font-semibold text-heading">Datos del docente</legend>
              <label class="flex flex-col gap-1 text-sm">
                <span class="text-muted">Nombres *</span>
                <input type="text" required [value]="formulario().docente.firstName"
                  (input)="actualizarDocente('firstName', $event)"
                  (keydown)="onNombreKeydown('docente', 'firstName', $event)" [class]="inputClass" />
              </label>
              <label class="flex flex-col gap-1 text-sm">
                <span class="text-muted">Segundo nombre</span>
                <input type="text" [value]="formulario().docente.middleName"
                  (input)="actualizarDocente('middleName', $event)"
                  (keydown)="onNombreKeydown('docente', 'middleName', $event)" [class]="inputClass" />
              </label>
              <label class="flex flex-col gap-1 text-sm">
                <span class="text-muted">Primer apellido *</span>
                <input type="text" required [value]="formulario().docente.firstSurname"
                  (input)="actualizarDocente('firstSurname', $event)"
                  (keydown)="onNombreKeydown('docente', 'firstSurname', $event)" [class]="inputClass" />
              </label>
              <label class="flex flex-col gap-1 text-sm">
                <span class="text-muted">Segundo apellido</span>
                <input type="text" [value]="formulario().docente.secondSurname"
                  (input)="actualizarDocente('secondSurname', $event)"
                  (keydown)="onNombreKeydown('docente', 'secondSurname', $event)" [class]="inputClass" />
              </label>
              <label class="flex flex-col gap-1 text-sm">
                <span class="text-muted">RUT *</span>
                <input type="text" required maxlength="12" [value]="formulario().docente.rut"
                  (input)="actualizarDocente('rut', $event)" [class]="inputClass" placeholder="12.345.678-9" />
              </label>
              <label class="flex flex-col gap-1 text-sm">
                <span class="text-muted">Fecha de contratación *</span>
                <input type="date" required [value]="formulario().docente.fechaContratacion"
                  (input)="actualizarDocente('fechaContratacion', $event)" [class]="inputClass" />
              </label>
              <label class="flex flex-col gap-1 text-sm sm:col-span-2">
                <span class="text-muted">Área académica *</span>
                <select [value]="formulario().docente.area"
                  (change)="actualizarDocente('area', $event)" [class]="inputClass">
                  @for (area of areas; track area) {
                    <option [value]="area">{{ area }}</option>
                  }
                </select>
              </label>

              <div class="sm:col-span-2">
                <div class="flex items-center justify-between">
                  <p class="text-sm font-semibold text-heading">Certificados *</p>
                  <button type="button" (click)="agregarCertificado()"
                    class="rounded-lg border border-brand px-3 py-1 text-xs font-semibold text-brand transition hover:bg-brand/20">
                    Agregar certificado
                  </button>
                </div>
                @for (certificado of formulario().docente.certificados; track $index) {
                  <div class="mt-2 grid gap-2 rounded-xl border border-line p-3 sm:grid-cols-3">
                    <input type="text" [value]="certificado.nombre" placeholder="Nombre *"
                      (input)="actualizarCertificado($index, 'nombre', $event)" [class]="inputClass" />
                    <input type="text" [value]="certificado.institucionRealizacion" placeholder="Institución *"
                      (input)="actualizarCertificado($index, 'institucionRealizacion', $event)" [class]="inputClass" />
                    <div class="flex gap-2">
                      <input type="date" [value]="certificado.fechaTitulacion"
                        (input)="actualizarCertificado($index, 'fechaTitulacion', $event)" [class]="inputClass" />
                      @if (formulario().docente.certificados.length > 1) {
                        <button type="button" (click)="quitarCertificado($index)"
                          class="rounded-lg border border-line px-2 text-sm text-muted transition hover:text-ink">×</button>
                      }
                    </div>
                  </div>
                }
              </div>
            </fieldset>
          }

          @if (formulario().requestedRole === 'APODERADO') {
            <fieldset class="grid gap-3 sm:grid-cols-2">
              <legend class="col-span-full text-sm font-semibold text-heading">Datos del apoderado</legend>
              <label class="flex flex-col gap-1 text-sm">
                <span class="text-muted">Nombres *</span>
                <input type="text" required [value]="formulario().apoderado.firstName"
                  (input)="actualizarApoderado('firstName', $event)"
                  (keydown)="onNombreKeydown('apoderado', 'firstName', $event)" [class]="inputClass" />
              </label>
              <label class="flex flex-col gap-1 text-sm">
                <span class="text-muted">Segundo nombre</span>
                <input type="text" [value]="formulario().apoderado.middleName"
                  (input)="actualizarApoderado('middleName', $event)"
                  (keydown)="onNombreKeydown('apoderado', 'middleName', $event)" [class]="inputClass" />
              </label>
              <label class="flex flex-col gap-1 text-sm">
                <span class="text-muted">Primer apellido *</span>
                <input type="text" required [value]="formulario().apoderado.firstSurname"
                  (input)="actualizarApoderado('firstSurname', $event)"
                  (keydown)="onNombreKeydown('apoderado', 'firstSurname', $event)" [class]="inputClass" />
              </label>
              <label class="flex flex-col gap-1 text-sm">
                <span class="text-muted">Segundo apellido</span>
                <input type="text" [value]="formulario().apoderado.secondSurname"
                  (input)="actualizarApoderado('secondSurname', $event)"
                  (keydown)="onNombreKeydown('apoderado', 'secondSurname', $event)" [class]="inputClass" />
              </label>
              <label class="flex flex-col gap-1 text-sm">
                <span class="text-muted">RUT *</span>
                <input type="text" required maxlength="12" [value]="formulario().apoderado.rut"
                  (input)="actualizarApoderado('rut', $event)" [class]="inputClass" placeholder="12.345.678-9" />
              </label>
              <label class="flex flex-col gap-1 text-sm">
                <span class="text-muted">Teléfonos * (separados por coma)</span>
                <input type="text" required [value]="formulario().apoderado.telefonos"
                  (input)="actualizarApoderado('telefonos', $event)" [class]="inputClass"
                  placeholder="+56912345678, +56987654321" />
              </label>

              <div class="sm:col-span-2">
                <p class="text-sm font-semibold text-heading">Estudiantes vinculados *</p>
                <div class="relative mt-1">
                  <input type="search" [value]="textoBusqueda()" (input)="buscarAlumno($event)"
                    placeholder="Buscar alumno por RUT o nombre (mín. 2 caracteres)" [class]="inputClass" />
                  @if (buscando()) {
                    <p class="mt-1 text-xs text-muted">Buscando…</p>
                  }
                  @if (errorBusqueda()) {
                    <p class="mt-1 text-xs text-red-600">{{ errorBusqueda() }}</p>
                  }
                  @if (resultados().length > 0) {
                    <ul class="absolute z-20 mt-1 max-h-56 w-full overflow-auto rounded-xl border border-line bg-panel shadow-xl">
                      @for (opcion of resultados(); track opcion.id) {
                        <li>
                          <button type="button"
                            class="w-full px-3 py-2 text-left text-sm text-ink transition hover:bg-brand/10"
                            (click)="seleccionarAlumno(opcion)">
                            {{ etiquetaDeEstudiante(opcion) }}
                          </button>
                        </li>
                      }
                    </ul>
                  }
                </div>
                @for (vinculo of formulario().apoderado.estudiantes; track $index) {
                  <div class="mt-2 flex flex-wrap items-center gap-2 rounded-xl border border-line p-3">
                    <span class="flex-1 text-sm text-ink">{{ vinculo.etiqueta }}</span>
                    <select [value]="vinculo.parentesco" (change)="actualizarVinculo($index, $event)"
                      [class]="inputClass">
                      @for (parentesco of parentescos; track parentesco) {
                        <option [value]="parentesco">{{ parentesco }}</option>
                      }
                    </select>
                    <button type="button" (click)="quitarVinculo($index)"
                      class="rounded-lg border border-line px-3 py-2 text-sm text-muted transition hover:text-ink">×</button>
                  </div>
                }
                @if (formulario().apoderado.estudiantes.length === 0) {
                  <p class="mt-2 text-xs text-muted">Agrega al menos un estudiante.</p>
                }
              </div>
            </fieldset>
          }

          <div class="flex justify-end">
            <button
              type="submit"
              [disabled]="enviando()"
              class="rounded-lg bg-brand px-5 py-2 text-sm font-semibold text-page transition hover:brightness-110 disabled:opacity-60"
            >
              {{ enviando() ? 'Enviando…' : 'Registrar usuario' }}
            </button>
          </div>
        </form>
      } @else {
        <div class="mt-4 flex flex-col gap-4">
          <div class="grid gap-3 sm:grid-cols-3">
            <div class="rounded-xl border border-line bg-surface p-3">
              <p class="text-xs text-muted">Proceso</p>
              <p class="font-semibold text-ink">{{ estado()!.processId }}</p>
            </div>
            <div class="rounded-xl border border-line bg-surface p-3">
              <p class="text-xs text-muted">Estado</p>
              <p class="font-semibold" [class]="colorEstado(estado()!.state)">{{ estado()!.state }}</p>
            </div>
            <div class="rounded-xl border border-line bg-surface p-3">
              <p class="text-xs text-muted">Entra ID / Dominio</p>
              <p class="font-semibold text-ink">
                {{ estado()!.azureState }} / {{ estado()!.domainState }}
              </p>
            </div>
          </div>

          @if (estado()!.errorMessage) {
            <p class="rounded-xl border border-gold/60 bg-surface px-4 py-3 text-sm text-ink">
              {{ estado()!.errorMessage }}
            </p>
          }

          @if (estado()!.state === 'EN_PROCESO' || estado()!.state === 'PENDIENTE') {
            <p class="text-sm text-muted">Procesando… la vista se actualiza automáticamente.</p>
          }

          @if (estado()!.state === 'COMPLETADO') {
            <div class="flex flex-wrap items-center gap-3">
              <button
                type="button"
                (click)="obtenerCredencial()"
                [disabled]="obteniendoCredencial()"
                class="rounded-lg bg-brand px-4 py-2 text-sm font-semibold text-page transition hover:brightness-110 disabled:opacity-60"
              >
                {{ obteniendoCredencial() ? 'Obteniendo…' : 'Obtener credencial temporal' }}
              </button>
              <p class="text-sm text-muted">La clave se muestra una sola vez.</p>
            </div>
          }

          <div class="flex justify-end gap-2">
            @if (estado()!.state === 'FALLIDO') {
              <button
                type="button"
                (click)="nuevoIntento()"
                class="rounded-lg border border-brand px-4 py-2 text-sm font-semibold text-brand transition hover:bg-brand/20"
              >
                Nuevo intento
              </button>
            }
            <button
              type="button"
              (click)="cerrar.emit()"
              class="rounded-lg border border-line px-4 py-2 text-sm text-muted transition hover:text-ink"
            >
              Cerrar
            </button>
          </div>
        </div>
      }
    </section>

    @if (credencial(); as credencialTemporal) {
      <siga-credencial-temporal [credencial]="credencialTemporal" (cerrar)="credencial.set(null)" />
    }
  `,
})
export class NuevoUsuarioComponent implements OnDestroy {
  private readonly adminService = inject(AdminService);

  readonly cerrar = output<void>();
  readonly finalizado = output<void>();

  protected readonly inputClass =
    'rounded-xl border border-line bg-surface px-3 py-2 text-sm text-ink placeholder:text-muted focus:outline-none focus-visible:ring-2 focus-visible:ring-brand';
  protected readonly areas = AREAS_ACADEMICAS;
  protected readonly parentescos = PARENTESCOS;
  protected readonly etiquetaDeClase = etiquetaClase;
  protected readonly etiquetaDeEstudiante = etiquetaEstudiante;

  protected readonly formulario = signal<FormularioRegistro>(structuredClone(FORM_INICIAL));
  protected readonly enviando = signal(false);
  protected readonly obteniendoCredencial = signal(false);
  protected readonly mensaje = signal<string | null>(null);
  protected readonly estado = signal<RegistroUsuarioEstado | null>(null);
  protected readonly credencial = signal<CredencialTemporal | null>(null);

  protected readonly anioActual = new Date().getFullYear();
  protected readonly clases = signal<ClaseOpcion[]>([]);
  protected readonly cargandoClases = signal(false);
  protected readonly errorClases = signal<string | null>(null);
  protected readonly textoBusqueda = signal('');
  protected readonly resultados = signal<EstudianteOpcion[]>([]);
  protected readonly buscando = signal(false);
  protected readonly errorBusqueda = signal<string | null>(null);

  protected readonly copiado = signal<'nombre' | 'correo' | null>(null);
  private readonly excepcionesNombre = signal<Set<string>>(new Set());
  protected readonly nombreCompletoPreview = computed(() => {
    const campos = this.camposNombre();
    return nombreCompletoSugerido(campos.n, campos.m, campos.a1, campos.a2);
  });
  protected readonly correoPreview = computed(() => {
    const campos = this.camposNombre();
    return correoInstitucionalSugerido(campos.n, campos.a1);
  });

  private polling?: Subscription;

  constructor() {
    this.cargarClases();
    toObservable(this.textoBusqueda)
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((texto) => {
          const q = texto.trim();
          if (q.length < 2) {
            return of<EstudianteOpcion[] | null>([]);
          }
          this.buscando.set(true);
          this.errorBusqueda.set(null);
          return this.adminService.buscarEstudiantes(q);
        }),
        takeUntilDestroyed(),
      )
      .subscribe({
        next: (resultados) => {
          this.buscando.set(false);
          this.resultados.set(resultados ?? []);
        },
        error: () => {
          this.buscando.set(false);
          this.resultados.set([]);
          this.errorBusqueda.set('No se pudo buscar alumnos.');
        },
      });
  }

  ngOnDestroy(): void {
    this.detenerPolling();
  }

  private cargarClases(): void {
    this.cargandoClases.set(true);
    this.errorClases.set(null);
    this.adminService.getClases(this.anioActual).subscribe({
      next: (clases) => {
        this.cargandoClases.set(false);
        this.clases.set(clases ?? []);
      },
      error: () => {
        this.cargandoClases.set(false);
        this.clases.set([]);
        this.errorClases.set('No se pudieron cargar las clases.');
      },
    });
  }

  protected actualizar(campo: 'contactEmail', event: Event): void {
    const valor = (event.target as HTMLInputElement).value;
    this.formulario.update((actual) => ({ ...actual, [campo]: valor }));
  }

  protected cambiarRol(event: Event): void {
    const valor = (event.target as HTMLSelectElement).value as RolRegistrable;
    this.formulario.update((actual) => ({ ...actual, requestedRole: valor }));
  }

  protected copiar(valor: string, campo: 'nombre' | 'correo'): void {
    void copiarAlPortapapeles(valor).then((ok) => {
      this.copiado.set(ok ? campo : null);
      if (ok) {
        setTimeout(() => {
          if (this.copiado() === campo) {
            this.copiado.set(null);
          }
        }, 2000);
      }
    });
  }

  private camposNombre(): { n: string; m: string; a1: string; a2: string } {
    const formulario = this.formulario();
    if (formulario.requestedRole === 'ESTUDIANTE') {
      return {
        n: formulario.estudiante.firstName,
        m: formulario.estudiante.middleName,
        a1: formulario.estudiante.firstSurname,
        a2: formulario.estudiante.secondSurname,
      };
    }
    if (formulario.requestedRole === 'DOCENTE') {
      return {
        n: formulario.docente.firstName,
        m: formulario.docente.middleName,
        a1: formulario.docente.firstSurname,
        a2: formulario.docente.secondSurname,
      };
    }
    return {
      n: formulario.apoderado.firstName,
      m: formulario.apoderado.middleName,
      a1: formulario.apoderado.firstSurname,
      a2: formulario.apoderado.secondSurname,
    };
  }

  protected onNombreKeydown(rol: 'estudiante' | 'docente' | 'apoderado', campo: string, event: KeyboardEvent): void {
    if (event.shiftKey && event.key.length === 1 && /\p{L}/u.test(event.key)) {
      const clave = `${rol}.${campo}`;
      this.excepcionesNombre.update((actual) => new Set(actual).add(clave));
    }
  }

  private valorCampo(rol: 'estudiante' | 'docente' | 'apoderado', campo: string, event: Event): string {
    const input = event.target as HTMLInputElement | HTMLSelectElement;
    if (CAMPOS_NOMBRE.includes(campo)) {
      const clave = `${rol}.${campo}`;
      if (!input.value.trim()) {
        this.excepcionesNombre.update((actual) => {
          const copia = new Set(actual);
          copia.delete(clave);
          return copia;
        });
        return input.value;
      }
      return this.excepcionesNombre().has(clave)
        ? input.value
        : normalizarNombrePropio(input.value);
    }
    if (campo === 'rut') {
      return formatearRutEntrada(input.value);
    }
    return input.value;
  }

  protected actualizarEstudiante(campo: keyof FormularioRegistro['estudiante'], event: Event): void {
    const valor = this.valorCampo('estudiante', campo as string, event);
    this.formulario.update((actual) => ({
      ...actual,
      estudiante: { ...actual.estudiante, [campo]: valor },
    }));
  }

  protected actualizarDocente(campo: 'firstName' | 'middleName' | 'firstSurname' | 'secondSurname' | 'rut' | 'fechaContratacion' | 'area', event: Event): void {
    const valor = this.valorCampo('docente', campo, event);
    this.formulario.update((actual) => ({
      ...actual,
      docente: { ...actual.docente, [campo]: valor },
    }));
  }

  protected actualizarApoderado(campo: 'firstName' | 'middleName' | 'firstSurname' | 'secondSurname' | 'rut' | 'telefonos', event: Event): void {
    const valor = this.valorCampo('apoderado', campo, event);
    this.formulario.update((actual) => ({
      ...actual,
      apoderado: { ...actual.apoderado, [campo]: valor },
    }));
  }

  protected agregarCertificado(): void {
    this.formulario.update((actual) => ({
      ...actual,
      docente: {
        ...actual.docente,
        certificados: [...actual.docente.certificados, { nombre: '', institucionRealizacion: '', fechaTitulacion: '' }],
      },
    }));
  }

  protected quitarCertificado(indice: number): void {
    this.formulario.update((actual) => ({
      ...actual,
      docente: {
        ...actual.docente,
        certificados: actual.docente.certificados.filter((_, i) => i !== indice),
      },
    }));
  }

  protected actualizarCertificado(indice: number, campo: keyof CertificadoForm, event: Event): void {
    const valor = (event.target as HTMLInputElement).value;
    this.formulario.update((actual) => ({
      ...actual,
      docente: {
        ...actual.docente,
        certificados: actual.docente.certificados.map((certificado, i) =>
          i === indice ? { ...certificado, [campo]: valor } : certificado,
        ),
      },
    }));
  }

  protected buscarAlumno(event: Event): void {
    this.textoBusqueda.set((event.target as HTMLInputElement).value);
  }

  protected seleccionarAlumno(opcion: EstudianteOpcion): void {
    if (this.formulario().apoderado.estudiantes.some((v) => v.idEstudiante === String(opcion.id))) {
      this.errorBusqueda.set('Ese estudiante ya está vinculado.');
      return;
    }
    this.formulario.update((actual) => ({
      ...actual,
      apoderado: {
        ...actual.apoderado,
        estudiantes: [
          ...actual.apoderado.estudiantes,
          {
            idEstudiante: String(opcion.id),
            parentesco: PARENTESCOS[0],
            etiqueta: etiquetaEstudiante(opcion),
          },
        ],
      },
    }));
    this.textoBusqueda.set('');
    this.resultados.set([]);
    this.errorBusqueda.set(null);
  }

  protected quitarVinculo(indice: number): void {
    this.formulario.update((actual) => ({
      ...actual,
      apoderado: {
        ...actual.apoderado,
        estudiantes: actual.apoderado.estudiantes.filter((_, i) => i !== indice),
      },
    }));
  }

  protected actualizarVinculo(indice: number, event: Event): void {
    const parentesco = (event.target as HTMLSelectElement).value;
    this.formulario.update((actual) => ({
      ...actual,
      apoderado: {
        ...actual.apoderado,
        estudiantes: actual.apoderado.estudiantes.map((vinculo, i) =>
          i === indice ? { ...vinculo, parentesco } : vinculo,
        ),
      },
    }));
  }

  protected enviar(event: Event): void {
    event.preventDefault();
    if (this.enviando()) {
      return;
    }
    const payload = this.construirPayload();
    if (!payload) {
      return;
    }
    this.enviando.set(true);
    this.mensaje.set(null);
    this.adminService.iniciarRegistro(payload).subscribe({
      next: (estado) => {
        this.enviando.set(false);
        if (!estado) {
          this.mensaje.set('Modo demo: el registro asíncrono no está disponible.');
          return;
        }
        this.estado.set(estado);
        this.finalizado.emit();
        this.iniciarPolling(estado.processId);
      },
      error: (error: HttpErrorResponse) => {
        this.enviando.set(false);
        this.mensaje.set(this.textoError(error));
      },
    });
  }

  protected obtenerCredencial(): void {
    const estado = this.estado();
    if (!estado || this.obteniendoCredencial()) {
      return;
    }
    this.obteniendoCredencial.set(true);
    this.mensaje.set(null);
    this.adminService.getCredencialTemporal(estado.processId).subscribe({
      next: (credencial) => {
        this.obteniendoCredencial.set(false);
        this.credencial.set(credencial);
      },
      error: (error: HttpErrorResponse) => {
        this.obteniendoCredencial.set(false);
        this.mensaje.set(this.textoError(error));
      },
    });
  }

  protected nuevoIntento(): void {
    this.detenerPolling();
    this.estado.set(null);
    this.mensaje.set(null);
  }

  protected colorEstado(estado: string): string {
    if (estado === 'COMPLETADO') {
      return 'text-emerald-600';
    }
    if (estado === 'FALLIDO') {
      return 'text-red-600';
    }
    return 'text-brand';
  }

  private iniciarPolling(processId: string): void {
    this.detenerPolling();
    this.polling = interval(2500)
      .pipe(
        startWith(0),
        switchMap(() => this.adminService.getRegistro(processId)),
        takeWhile((estado) => !esEstadoFinal(estado.state), true),
      )
      .subscribe({
        next: (estado) => this.estado.set(estado),
        error: (error: HttpErrorResponse) => {
          this.mensaje.set(this.textoError(error));
          this.detenerPolling();
        },
      });
  }

  private detenerPolling(): void {
    this.polling?.unsubscribe();
    this.polling = undefined;
  }

  private construirPayload(): RegistroUsuarioPayload | null {
    const formulario = this.formulario();

    const base: RegistroUsuarioPayload = {
      requestedRole: formulario.requestedRole,
      contactEmail: formulario.contactEmail.trim() || null,
      roleData: {},
    };

    if (formulario.requestedRole === 'ESTUDIANTE') {
      const datos = formulario.estudiante;
      if (!datos.firstName.trim() || !datos.firstSurname.trim() || !datos.rut.trim()
        || !datos.birthDate || !datos.idClase) {
        this.mensaje.set('Completa los campos obligatorios del estudiante (nombres, apellidos, RUT, nacimiento y clase).');
        return null;
      }
      base.roleData.estudiante = {
        firstName: datos.firstName.trim(),
        middleName: datos.middleName.trim() || null,
        firstSurname: datos.firstSurname.trim(),
        secondSurname: datos.secondSurname.trim() || null,
        rut: datos.rut.trim(),
        birthDate: datos.birthDate,
        allergies: datos.allergies.trim()
          ? datos.allergies.split(',').map((item) => item.trim()).filter(Boolean)
          : null,
        idClase: datos.idClase ? Number(datos.idClase) : null,
      };
      return base;
    }

    if (formulario.requestedRole === 'DOCENTE') {
      const datos = formulario.docente;
      const certificados = datos.certificados.filter(
        (certificado) => certificado.nombre.trim() && certificado.institucionRealizacion.trim() && certificado.fechaTitulacion,
      );
      if (!datos.firstName.trim() || !datos.firstSurname.trim() || !datos.rut.trim()
        || !datos.fechaContratacion || certificados.length === 0) {
        this.mensaje.set('Completa los campos obligatorios del docente y al menos un certificado completo.');
        return null;
      }
      base.roleData.docente = {
        firstName: datos.firstName.trim(),
        middleName: datos.middleName.trim() || null,
        firstSurname: datos.firstSurname.trim(),
        secondSurname: datos.secondSurname.trim() || null,
        rut: datos.rut.trim(),
        fechaContratacion: datos.fechaContratacion,
        area: datos.area,
        certificados: certificados.map((certificado) => ({
          nombre: certificado.nombre.trim(),
          institucionRealizacion: certificado.institucionRealizacion.trim(),
          fechaTitulacion: certificado.fechaTitulacion,
        })),
      };
      return base;
    }

    const datos = formulario.apoderado;
    const telefonos = datos.telefonos.split(',').map((item) => item.trim()).filter(Boolean);
    const vinculos = datos.estudiantes.map((vinculo) => ({
      idEstudiante: Number(vinculo.idEstudiante),
      parentesco: vinculo.parentesco,
    }));
    if (!datos.firstName.trim() || !datos.firstSurname.trim() || !datos.rut.trim()
      || telefonos.length === 0 || vinculos.length === 0) {
      this.mensaje.set('Completa los campos obligatorios del apoderado, al menos un teléfono y un estudiante.');
      return null;
    }
    base.roleData.apoderado = {
      firstName: datos.firstName.trim(),
      middleName: datos.middleName.trim() || null,
      firstSurname: datos.firstSurname.trim(),
      secondSurname: datos.secondSurname.trim() || null,
      rut: datos.rut.trim(),
      telefonos,
      estudiantes: vinculos,
    };
    return base;
  }

  private textoError(error: HttpErrorResponse): string {
    const cuerpo = error.error as { message?: string; errors?: Record<string, string> } | null;
    const detalleCampos = cuerpo?.errors ? Object.values(cuerpo.errors)[0] : undefined;
    return detalleCampos
      ?? cuerpo?.message
      ?? 'No se pudo completar la operación. Revisa la conexión con el BFF.';
  }
}

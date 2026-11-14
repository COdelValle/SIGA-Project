import { readdir, readFile, stat, writeFile } from 'node:fs/promises';
import { basename, join, resolve } from 'node:path';

const roots = process.argv.slice(2);
if (roots.length === 0) {
  roots.push('allure-results');
}

const LAYERS = new Set([
  'unit',
  'component',
  'integration',
  'contract',
  'e2e',
  'security',
  'performance',
  'infra',
  'coverage',
]);

const MODULE_FEATURES = {
  msnotas: 'Notas',
  msevaluaciones: 'Evaluaciones',
  msasistencias: 'Asistencias',
  msestudiantes: 'Estudiantes',
  msapoderados: 'Apoderados',
  msdocentes: 'Docentes',
  msasignaturas: 'Asignaturas',
  msclases: 'Clases',
  msnotificaciones: 'Notificaciones',
  msrabbitmqadmin: 'RabbitMQ Admin',
  msusuariosauth: 'Usuarios y autenticacion',
};

const FRONTEND_FEATURES = {
  'notas-tabla': 'Notas',
  'asistencia-historial': 'Asistencias',
  'asistencia-lista': 'Asistencias',
  'asistencia-tabla': 'Asistencias',
  'periodo-resumen': 'Periodos',
  'horario-resumen': 'Horarios',
  'registrar-notas': 'Notas',
  'registrar-asistencias': 'Asistencias',
  usuarios: 'Usuarios',
  inicio: 'Inicio',
  asignaturas: 'Asignaturas',
  malla: 'Malla curricular',
  roles: 'Roles',
  'nuevo-usuario': 'Nuevo usuario',
  'detalle-usuario': 'Detalle usuario',
  'credencial-temporal': 'Credencial temporal',
  'progreso': 'Progreso',
  'notas': 'Notas',
  'horarios': 'Horarios',
  'asistencias': 'Asistencias',
  'perfil-estudiante': 'Perfil estudiante',
  'academico.model': 'Modelos academicos',
  'access-token-claims': 'Autenticacion',
  'msal.factory': 'Autenticacion',
  'role.guard': 'Roles y permisos',
  'notification.service': 'Notificaciones',
  'notification-bell': 'Notificaciones',
  'app': 'Bootstrap',
};

function labelsOf(result) {
  return new Map((result.labels ?? []).map((label) => [label.name, label.value]));
}

function addLabel(result, name, value) {
  if (!value) {
    return;
  }
  result.labels ??= [];
  const existing = result.labels.find((label) => label.name === name);
  if (!existing) {
    result.labels.push({ name, value });
  } else if (!existing.value) {
    existing.value = value;
  }
}

function capitalize(value) {
  return value ? value.charAt(0).toUpperCase() + value.slice(1) : value;
}

function humanize(value) {
  if (!value) {
    return '';
  }
  const spaced = String(value)
    .replace(/\(\)/g, '')
    .replace(/_/g, ' ')
    .replace(/([a-z0-9])([A-Z])/g, '$1 $2')
    .replace(/([A-Z]+)([A-Z][a-z])/g, '$1 $2')
    .replace(/\s+/g, ' ')
    .trim();
  return spaced
    .split(' ')
    .map((word, index) => {
      if (index === 0) {
        return capitalize(word);
      }
      return word.length > 1 && word === word.toUpperCase() ? word : word.toLowerCase();
    })
    .join(' ');
}

function inferredLayer(className, framework) {
  if (framework === 'playwright') {
    return 'e2e';
  }
  if (framework === 'vitest') {
    return /component\.spec|\.pages\.|shared-ui/.test(className) ? 'component' : 'unit';
  }
  if (className.includes('ContractTest') || className.includes('OpenApiSpecificationTest')) {
    return 'contract';
  }
  if (
    className.includes('ControllerTest') ||
    className.includes('Migraciones') ||
    className.includes('Rabbit') ||
    className.includes('Outbox') ||
    className.includes('ConfirmadorMensajesRabbit')
  ) {
    return 'integration';
  }
  if (
    className.includes('PreAuthorize') ||
    className.includes('Security') ||
    className.includes('Jwt') ||
    className.includes('CredentialCipher')
  ) {
    return 'security';
  }
  return 'unit';
}

function frontendTaxonomy(pkg) {
  const file = pkg
    .replace(/^frontend\./, '')
    .replace(/\.spec\.ts$/, '')
    .split('.');
  const libIndex = file.indexOf('libs');
  const lib = libIndex >= 0 ? file[libIndex + 1] : 'src';
  const featureKey = Object.keys(FRONTEND_FEATURES)
    .sort((left, right) => right.length - left.length)
    .find((key) => pkg.includes(key));
  const layer = inferredLayer(pkg, 'vitest');
  let epic = 'Compartido';
  let feature = featureKey ? FRONTEND_FEATURES[featureKey] : humanize(file[file.length - 1]);

  if (lib === 'admin') {
    epic = 'Portal Administracion';
  } else if (lib === 'docente') {
    epic = 'Portal Docente';
  } else if (lib === 'estudiante') {
    epic = 'Portal Estudiante';
  } else if (lib === 'apoderado') {
    epic = 'Portal Apoderado';
  } else if (lib === 'academico') {
    epic = 'Gestion Academica';
  } else if (lib === 'core') {
    epic = pkg.includes('.auth.') ? 'Identidad y Acceso' : 'Compartido';
  } else if (lib === 'src') {
    epic = 'Aplicacion';
  }
  if (pkg.includes('notification')) {
    feature = 'Notificaciones';
  }
  if (pkg.includes('.auth.')) {
    feature = pkg.includes('role.guard') ? 'Roles y permisos' : 'Autenticacion';
  }
  return { epic, feature, layer, owner: 'frontend', story: feature };
}

function backendTaxonomy(pkg, className) {
  const layer = inferredLayer(className, 'junit-platform');
  const module = pkg.match(/cl\.siga\.([^.]+)/)?.[1] ?? '';
  let epic = 'Gestion Academica';
  let feature = MODULE_FEATURES[module] ?? humanize(className);
  let owner = 'backend';

  if (pkg.includes('.contract.')) {
    epic = 'Contratos BFF';
    feature = humanize(className.replace(/ClientContractTest$/, ''));
    owner = 'qa';
  } else if (pkg.includes('.openapi.')) {
    epic = pkg.includes('.bffweb.') ? 'Contratos BFF' : epic;
    feature = pkg.includes('.bffweb.') ? 'OpenAPI BFF' : `OpenAPI ${feature}`;
  } else if (module === 'bffweb') {
    epic = 'BFF';
    if (pkg.includes('.domain.')) {
      const rest = pkg.split('.domain.')[1] ?? '';
      feature = humanize(rest.split('.')[0] ?? 'BFF');
    } else if (pkg.includes('.integration.')) {
      const rest = pkg.split('.integration.')[1] ?? '';
      feature = humanize(rest.split('.')[0] ?? 'Integracion');
    }
  } else if (module === 'coreshare') {
    epic = 'Plataforma';
    if (pkg.includes('.security.')) feature = 'Seguridad';
    else if (pkg.includes('.mensajeria.') || pkg.includes('.messaging.')) feature = 'Mensajeria';
    else if (pkg.includes('.validation.')) feature = 'Validacion';
    else if (pkg.includes('.format.')) feature = 'Formato';
    else if (pkg.includes('.exception.')) feature = 'Errores';
    else if (pkg.includes('.config.')) feature = 'Configuracion';
    else if (pkg.includes('.dto.')) feature = 'Contratos';
  } else if (module === 'msusuariosauth') {
    epic = 'Identidad y Acceso';
    if (className.includes('AsyncUserRegistration') || className.includes('MigracionesRegistro')) feature = 'Registro asincrono';
    else if (className.includes('Invitacion')) feature = 'Invitaciones';
    else if (className.includes('Credential')) feature = 'Credenciales';
    else if (className.includes('EmailInstitucional')) feature = 'Correo institucional';
    else if (className.includes('Migraciones')) feature = 'Migraciones';
    else feature = 'Usuarios';
  } else if (module === 'msnotificaciones') {
    epic = 'Notificaciones';
  } else if (module === 'msrabbitmqadmin') {
    epic = 'Plataforma';
  }

  const story = humanize(
    className
      .replace('ContractTest', '')
      .replace('OpenApiSpecificationTest', '')
      .replace('SpecificationTest', '')
      .replace('ApplicationTests', '')
      .replace('Test', ''),
  );
  return { epic, feature, layer, owner, story };
}

function playwightTaxonomy(pkg) {
  const layer = 'e2e';
  let epic = 'Portal';
  let feature = 'E2E';
  if (pkg.includes('admin')) {
    epic = 'Portal Administracion';
    feature = 'Admin';
  } else if (pkg.includes('docente')) {
    epic = 'Portal Docente';
    feature = 'Docente';
  } else if (pkg.includes('estudiante')) {
    epic = 'Portal Estudiante';
    feature = 'Estudiante';
  } else if (pkg.includes('apoderado')) {
    epic = 'Portal Apoderado';
    feature = 'Apoderado';
  }
  return { epic, feature, layer, owner: 'qa', story: feature };
}

async function processFile(path) {
  const original = await readFile(path, 'utf8');
  let result;
  try {
    result = JSON.parse(original);
  } catch {
    return;
  }
  const labels = labelsOf(result);
  const framework = labels.get('framework') ?? '';
  const pkg = labels.get('package') ?? labels.get('testClass') ?? labels.get('suite') ?? '';
  let taxonomy;

  if (framework === 'vitest') {
    taxonomy = frontendTaxonomy(pkg);
  } else if (framework === 'playwright') {
    taxonomy = playwightTaxonomy(pkg);
  } else if (framework === 'junit-platform') {
    taxonomy = backendTaxonomy(pkg, labels.get('testClass') ?? pkg);
  } else {
    taxonomy = null;
  }

  if (!taxonomy) {
    return;
  }

  let name = result.name ?? '';
  if (!name || name.endsWith('()') || name === labels.get('testMethod')) {
    name = humanize(labels.get('testMethod') ?? name);
    result.name = name;
    result.fullName = labels.get('testClass') ? `${labels.get('testClass')}.${name}` : result.fullName;
  }

  addLabel(result, 'layer', taxonomy.layer);
  addLabel(result, 'epic', taxonomy.epic);
  addLabel(result, 'feature', taxonomy.feature);
  addLabel(result, 'story', taxonomy.story);
  addLabel(result, 'owner', taxonomy.owner);
  addLabel(result, 'severity', 'normal');
  addLabel(result, 'tag', taxonomy.layer);

  if (!LAYERS.has(labels.get('layer') ?? taxonomy.layer)) {
    throw new Error(`Capa Allure invalida en ${path}: ${labels.get('layer')}`);
  }

  const next = JSON.stringify(result, null, 2);
  if (next !== original) {
    await writeFile(path, next, 'utf8');
  }
}

async function walk(path) {
  const info = await stat(path);
  if (info.isDirectory()) {
    for (const entry of await readdir(path)) {
      if (entry === 'node_modules') {
        continue;
      }
      await walk(join(path, entry));
    }
    return;
  }
  if (basename(path).endsWith('-result.json')) {
    await processFile(path);
  }
}

for (const root of roots) {
  await walk(resolve(root));
}

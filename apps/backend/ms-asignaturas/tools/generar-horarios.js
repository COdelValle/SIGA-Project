/*
 * Genera la migracion V4__recalcular_horarios_mock.sql de ms-asignaturas.
 *
 * Reglas:
 *  - 5 dias x 4 franjas de 90 min por curso (20 franjas semanales).
 *  - Un docente no puede estar en dos cursos en la misma franja.
 *  - Una asignatura no se repite mas de una vez por dia en un curso.
 *  - En 7-8, Artes Visuales y Musica son electivas: comparten franja y los
 *    alumnos se inscriben en una (se modela como un item con 2 docentes).
 *  - Los cursos de 5-6 cambian a los docentes del grupo de niveles y suman
 *    Lengua de Senas y Religion; 4B suma los docentes nuevos 24/25.
 *
 * Uso: node tools/generar-horarios.js
 */
'use strict';

const fs = require('fs');
const path = require('path');

const DIAS = ['LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES'];
const BLOQUES = [
  ['08:00:00', '08:45:00'],
  ['08:45:00', '09:30:00'],
  ['09:50:00', '10:35:00'],
  ['10:35:00', '11:20:00'],
  ['12:15:00', '13:00:00'],
  ['13:00:00', '13:45:00'],
  ['13:55:00', '14:40:00'],
  ['14:40:00', '15:25:00'],
];
const FRANJAS_POR_DIA = 4;
const TOTAL_SLOTS = DIAS.length * FRANJAS_POR_DIA;

const SALA_EF = 'Cancha Techada 1';
const SALA_EF_4B = 'Patio Cubierto 2';

const clases = [
  { id: 1, grupo: '78', sala: 'Sala 7° Básico A', ef: SALA_EF },
  { id: 2, grupo: '78', sala: 'Sala 7° Básico B', ef: SALA_EF },
  { id: 3, grupo: '78', sala: 'Sala 7° Básico C', ef: SALA_EF },
  { id: 4, grupo: '78', sala: 'Sala 8° Básico A', ef: SALA_EF },
  { id: 5, grupo: '78', sala: 'Sala 8° Básico B', ef: SALA_EF },
  { id: 6, grupo: '78', sala: 'Sala 8° Básico C', ef: SALA_EF },
  { id: 7, grupo: '4', sala: 'Sala 4° Básico B', ef: SALA_EF_4B },
  { id: 8, grupo: '56', sala: 'Sala 5° Básico A', ef: SALA_EF },
  { id: 9, grupo: '56', sala: 'Sala 5° Básico B', ef: SALA_EF },
  { id: 10, grupo: '56', sala: 'Sala 5° Básico C', ef: SALA_EF },
  { id: 11, grupo: '56', sala: 'Sala 6° Básico A', ef: SALA_EF },
  { id: 12, grupo: '56', sala: 'Sala 6° Básico B', ef: SALA_EF },
  { id: 13, grupo: '56', sala: 'Sala 6° Básico C', ef: SALA_EF },
];

// Planes semanales (en franjas de 90 min). El item `electiva` ocupa una franja
// con dos dictaciones simultaneas (Artes y Musica).
const planes = {
  '78': [
    { subj: 2, docente: 4, n: 3 },   // Lengua y Literatura
    { subj: 3, docente: 6, n: 3 },   // Matematica
    { subj: 4, docente: 1, n: 3 },   // Ciencias Naturales
    { subj: 9, docente: 5, n: 3 },   // Historia
    { subj: 13, docente: 3, n: 2 },  // Educacion Fisica
    { subj: 10, docente: 7, n: 2 },  // Ingles
    { electiva: [{ subj: 11, docente: 8 }, { subj: 12, docente: 10 }] },
    { subj: 15, docente: 11, n: 1 }, // Orientacion
    { subj: 14, docente: 9, n: 1 },  // Tecnologia
    { subj: 17, docente: 12, n: 1 }, // Educacion Financiera
  ],
  '56': [
    { subj: 1, docente: 14, n: 3 },  // Lenguaje y Comunicacion
    { subj: 3, docente: 15, n: 3 },  // Matematica
    { subj: 4, docente: 2, n: 2 },   // Ciencias Naturales
    { subj: 9, docente: 13, n: 2 },  // Historia
    { subj: 13, docente: 18, n: 2 }, // Educacion Fisica
    { subj: 10, docente: 17, n: 2 }, // Ingles
    { subj: 11, docente: 19, n: 1 }, // Artes Visuales
    { subj: 12, docente: 21, n: 1 }, // Musica
    { subj: 15, docente: 22, n: 1 }, // Orientacion
    { subj: 14, docente: 20, n: 1 }, // Tecnologia
    { subj: 18, docente: 26, n: 1 }, // Lengua de Senas
    { subj: 16, docente: 23, n: 1 }, // Religion
  ],
  '4': [
    { subj: 1, docente: 24, n: 3 },  // Lenguaje y Comunicacion
    { subj: 3, docente: 25, n: 3 },  // Matematica
    { subj: 4, docente: 16, n: 3 },  // Ciencias Naturales
    { subj: 9, docente: 13, n: 3 },  // Historia
    { subj: 13, docente: 18, n: 2 }, // Educacion Fisica
    { subj: 10, docente: 17, n: 1 }, // Ingles
    { subj: 11, docente: 19, n: 1 }, // Artes Visuales
    { subj: 12, docente: 21, n: 1 }, // Musica
    { subj: 15, docente: 22, n: 1 }, // Orientacion
    { subj: 14, docente: 20, n: 1 }, // Tecnologia
    { subj: 16, docente: 23, n: 1 }, // Religion
  ],
};

// (clase, asignatura) -> id de la dictacion. Incluye las altas 144-155.
const cursoAsignatura = {
  1: { 2: 90, 3: 89, 4: 91, 9: 92, 10: 98, 11: 93, 12: 95, 13: 99, 14: 94, 15: 96, 17: 97 },
  2: { 2: 101, 3: 100, 4: 102, 9: 103, 10: 109, 11: 104, 12: 106, 13: 110, 14: 105, 15: 107, 17: 108 },
  3: { 2: 112, 3: 111, 4: 113, 9: 114, 10: 120, 11: 115, 12: 117, 13: 121, 14: 116, 15: 118, 17: 119 },
  4: { 2: 2, 3: 1, 4: 3, 9: 4, 10: 5, 11: 7, 12: 9, 13: 6, 14: 8, 15: 10, 17: 11 },
  5: { 2: 123, 3: 122, 4: 124, 9: 125, 10: 131, 11: 126, 12: 128, 13: 132, 14: 127, 15: 129, 17: 130 },
  6: { 2: 134, 3: 133, 4: 135, 9: 136, 10: 142, 11: 137, 12: 139, 13: 143, 14: 138, 15: 140, 17: 141 },
  7: { 1: 13, 3: 12, 4: 14, 9: 15, 10: 21, 11: 16, 12: 18, 13: 22, 14: 17, 15: 19, 16: 20 },
  8: { 1: 24, 3: 23, 4: 25, 9: 26, 10: 32, 11: 27, 12: 29, 13: 33, 14: 28, 15: 30, 18: 144, 16: 150 },
  9: { 1: 35, 3: 34, 4: 36, 9: 37, 10: 43, 11: 38, 12: 40, 13: 44, 14: 39, 15: 41, 18: 145, 16: 151 },
  10: { 1: 46, 3: 45, 4: 47, 9: 48, 10: 54, 11: 49, 12: 51, 13: 55, 14: 50, 15: 52, 18: 146, 16: 152 },
  11: { 1: 57, 3: 56, 4: 58, 9: 59, 10: 65, 11: 60, 12: 62, 13: 66, 14: 61, 15: 63, 18: 147, 16: 153 },
  12: { 1: 68, 3: 67, 4: 69, 9: 70, 10: 76, 11: 71, 12: 73, 13: 77, 14: 72, 15: 74, 18: 148, 16: 154 },
  13: { 1: 79, 3: 78, 4: 80, 9: 81, 10: 87, 11: 82, 12: 84, 13: 88, 14: 83, 15: 85, 18: 149, 16: 155 },
};

// Inscripciones de la electiva Artes(11)/Musica(12) en 7-8 (alternadas).
const inscripcionesElectiva = [
  [23, 93], [24, 95], [25, 93],
  [26, 104], [27, 106], [28, 104],
  [29, 115], [30, 117], [31, 115],
  [1, 7], [32, 9], [33, 7],
  [34, 126], [35, 128], [36, 126],
  [37, 137], [38, 139], [39, 137],
];

function construirItems() {
  const items = [];
  for (const clase of clases) {
    for (const entrada of planes[clase.grupo]) {
      if (entrada.electiva) {
        items.push({
          clase: clase.id,
          subjects: entrada.electiva,
          docentes: entrada.electiva.map((e) => e.docente),
          room: clase.sala,
        });
        continue;
      }
      for (let i = 0; i < entrada.n; i++) {
        items.push({
          clase: clase.id,
          subjects: [{ subj: entrada.subj, docente: entrada.docente }],
          docentes: [entrada.docente],
          room: entrada.subj === 13 ? clase.ef : clase.sala,
        });
      }
    }
  }
  return items;
}

function shuffle(arr, rnd) {
  for (let i = arr.length - 1; i > 0; i--) {
    const j = Math.floor(rnd() * (i + 1));
    [arr[i], arr[j]] = [arr[j], arr[i]];
  }
  return arr;
}

function mulberry32(seed) {
  let a = seed >>> 0;
  return function () {
    a |= 0;
    a = (a + 0x6d2b79f5) | 0;
    let t = Math.imul(a ^ (a >>> 15), 1 | a);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

function resolver(items, seed) {
  const rnd = mulberry32(seed);
  const classSlot = new Map();
  const docenteSlot = new Map();
  const subjectDay = new Map();
  for (const clase of clases) {
    classSlot.set(clase.id, new Array(TOTAL_SLOTS).fill(null));
  }
  const docentes = new Set(items.flatMap((i) => i.docentes));
  for (const d of docentes) {
    docenteSlot.set(d, new Set());
  }
  for (const item of items) {
    for (const s of item.subjects) {
      subjectDay.set(`${item.clase}|${s.subj}`, new Set());
    }
  }

  const puede = (item, slot) => {
    if (classSlot.get(item.clase)[slot]) return false;
    const dia = Math.floor(slot / FRANJAS_POR_DIA);
    for (const s of item.subjects) {
      if (subjectDay.get(`${item.clase}|${s.subj}`).has(dia)) return false;
    }
    for (const d of item.docentes) {
      if (docenteSlot.get(d).has(slot)) return false;
    }
    return true;
  };

  const poner = (item, slot) => {
    classSlot.get(item.clase)[slot] = item;
    const dia = Math.floor(slot / FRANJAS_POR_DIA);
    for (const s of item.subjects) subjectDay.get(`${item.clase}|${s.subj}`).add(dia);
    for (const d of item.docentes) docenteSlot.get(d).add(slot);
  };

  const quitar = (item, slot) => {
    classSlot.get(item.clase)[slot] = null;
    const dia = Math.floor(slot / FRANJAS_POR_DIA);
    for (const s of item.subjects) subjectDay.get(`${item.clase}|${s.subj}`).delete(dia);
    for (const d of item.docentes) docenteSlot.get(d).delete(slot);
  };

  const pendientes = shuffle(items.slice(), rnd);
  let nodos = 0;
  const limite = 4000000;

  const paso = (count) => {
    if (count === 0) return true;
    if (++nodos > limite) return false;

    let mejor = -1;
    let mejoresSlots = null;
    for (let i = 0; i < count; i++) {
      const item = pendientes[i];
      const slots = [];
      for (let s = 0; s < TOTAL_SLOTS; s++) {
        if (puede(item, s)) slots.push(s);
      }
      if (slots.length === 0) return false;
      if (mejoresSlots === null || slots.length < mejoresSlots.length) {
        mejor = i;
        mejoresSlots = slots;
        if (slots.length === 1) break;
      }
    }

    const item = pendientes[mejor];
    pendientes[mejor] = pendientes[count - 1];
    pendientes[count - 1] = item;
    shuffle(mejoresSlots, rnd);

    for (const slot of mejoresSlots) {
      poner(item, slot);
      if (paso(count - 1)) return true;
      quitar(item, slot);
    }

    pendientes[count - 1] = pendientes[mejor];
    pendientes[mejor] = item;
    return false;
  };

  if (!paso(pendientes.length)) {
    return null;
  }
  return { classSlot, docenteSlot };
}

function validar(solucion, items) {
  for (const clase of clases) {
    const slots = solucion.classSlot.get(clase.id);
    if (slots.filter(Boolean).length !== TOTAL_SLOTS) {
      throw new Error(`Curso ${clase.id}: franjas incompletas`);
    }
  }
  const cargas = new Map();
  for (const clase of clases) {
    for (const item of solucion.classSlot.get(clase.id)) {
      for (const s of item.subjects) {
        const key = `${clase.id}|${s.subj}`;
        cargas.set(key, (cargas.get(key) || 0) + 1);
      }
    }
  }
  for (const [claseId, plan] of Object.entries(cursoAsignatura)) {
    for (const [subj] of Object.entries(plan)) {
      // la validacion de conteos se hace contra el plan declarado
    }
  }
  // Conteos esperados por curso/asignatura
  const esperado = new Map();
  for (const clase of clases) {
    for (const entrada of planes[clase.grupo]) {
      if (entrada.electiva) {
        for (const e of entrada.electiva) esperado.set(`${clase.id}|${e.subj}`, 1);
      } else {
        esperado.set(`${clase.id}|${entrada.subj}`, entrada.n);
      }
    }
  }
  for (const [key, valor] of esperado) {
    if ((cargas.get(key) || 0) !== valor) {
      throw new Error(`Conteo invalido para ${key}: ${cargas.get(key)} != ${valor}`);
    }
  }
}

function filasHorarios(solucion) {
  const filas = [];
  for (const clase of clases) {
    const slots = solucion.classSlot.get(clase.id);
    for (let slot = 0; slot < TOTAL_SLOTS; slot++) {
      const item = slots[slot];
      const dia = DIAS[Math.floor(slot / FRANJAS_POR_DIA)];
      const franja = slot % FRANJAS_POR_DIA;
      for (const s of item.subjects) {
        const id = cursoAsignatura[clase.id][s.subj];
        if (!id) throw new Error(`Sin dictacion para clase ${clase.id} asignatura ${s.subj}`);
        for (const [entrada, salida] of [BLOQUES[franja * 2], BLOQUES[franja * 2 + 1]]) {
          filas.push(
            `    (${id}, '${dia}', '${entrada}', '${salida}', '${item.room.replace(/'/g, "''")}', TRUE)`,
          );
        }
      }
    }
  }
  return filas;
}

function mallaSql() {
  const lineas = [];
  const planes = {
    PRIMERO_BASICO: [[1, 6], [3, 6], [4, 6], [9, 6], [13, 4], [10, 2], [11, 2], [12, 2], [15, 2], [14, 2], [16, 2]],
    SEGUNDO_BASICO: [[1, 6], [3, 6], [4, 6], [9, 6], [13, 4], [10, 2], [11, 2], [12, 2], [15, 2], [14, 2], [16, 2]],
    TERCERO_BASICO: [[1, 6], [3, 6], [4, 6], [9, 6], [13, 4], [10, 2], [11, 2], [12, 2], [15, 2], [14, 2], [16, 2]],
    CUARTO_BASICO: [[1, 6], [3, 6], [4, 6], [9, 6], [13, 4], [10, 2], [11, 2], [12, 2], [15, 2], [14, 2], [16, 2]],
    QUINTO_BASICO: [[1, 6], [3, 6], [4, 4], [9, 4], [13, 4], [10, 4], [11, 2], [12, 2], [15, 2], [14, 2], [18, 2], [16, 2]],
    SEXTO_BASICO: [[1, 6], [3, 6], [4, 4], [9, 4], [13, 4], [10, 4], [11, 2], [12, 2], [15, 2], [14, 2], [18, 2], [16, 2]],
    SEPTIMO_BASICO: [[2, 6], [3, 6], [4, 6], [9, 6], [13, 4], [10, 4], [11, 2], [12, 2], [15, 2], [14, 2], [17, 2]],
    OCTAVO_BASICO: [[2, 6], [3, 6], [4, 6], [9, 6], [13, 4], [10, 4], [11, 2], [12, 2], [15, 2], [14, 2], [17, 2]],
    PRIMERO_MEDIO: [[2, 6], [3, 6], [5, 2], [7, 2], [6, 2], [9, 6], [13, 4], [10, 4], [11, 2], [12, 2], [15, 2], [14, 2], [20, 2]],
    SEGUNDO_MEDIO: [[2, 6], [3, 6], [5, 2], [7, 2], [6, 2], [9, 6], [13, 4], [10, 4], [11, 2], [12, 2], [15, 2], [14, 2], [20, 2]],
  };
  for (const [nivel, filas] of Object.entries(planes)) {
    for (const [subj, horas] of filas) {
      lineas.push(
        `UPDATE malla_curricular SET horas_semanales = ${horas} WHERE nivel = '${nivel}' AND id_asignatura = ${subj} AND plan = 'COMUN';`,
      );
    }
  }
  // Artes/Musica electivas en 7-8 y medios.
  for (const nivel of ['SEPTIMO_BASICO', 'OCTAVO_BASICO', 'PRIMERO_MEDIO', 'SEGUNDO_MEDIO']) {
    lineas.push(
      `UPDATE malla_curricular SET caracter = 'ELECTIVA' WHERE nivel = '${nivel}' AND id_asignatura IN (11, 12) AND plan = 'COMUN';`,
    );
  }
  // Filas fuera de malla.
  for (const nivel of ['QUINTO_BASICO', 'SEXTO_BASICO']) {
    lineas.push(
      `UPDATE malla_curricular SET horas_semanales = NULL, active = FALSE WHERE nivel = '${nivel}' AND id_asignatura = 17 AND plan = 'COMUN';`,
    );
  }
  for (const nivel of ['SEPTIMO_BASICO', 'OCTAVO_BASICO']) {
    lineas.push(
      `UPDATE malla_curricular SET horas_semanales = NULL, active = FALSE WHERE nivel = '${nivel}' AND id_asignatura IN (16, 18, 19) AND plan = 'COMUN';`,
    );
  }
  for (const nivel of ['PRIMERO_MEDIO', 'SEGUNDO_MEDIO']) {
    lineas.push(
      `UPDATE malla_curricular SET horas_semanales = NULL, active = FALSE WHERE nivel = '${nivel}' AND id_asignatura IN (16, 21, 22) AND plan = 'COMUN';`,
    );
  }
  // Alta de Lengua de Senas en 5-6 (no existe la fila).
  lineas.push(
    "INSERT INTO malla_curricular (nivel, id_asignatura, caracter, horas_semanales, plan, active) VALUES ('QUINTO_BASICO', 18, 'OPTATIVA', 2, 'COMUN', TRUE);",
    "INSERT INTO malla_curricular (nivel, id_asignatura, caracter, horas_semanales, plan, active) VALUES ('SEXTO_BASICO', 18, 'OPTATIVA', 2, 'COMUN', TRUE);",
  );
  return lineas.join('\n');
}

function migracion(solucion) {
  const lineas = [];
  lineas.push('-- Reparto docente por grupos de niveles (7-8 / 5-6 / 4B) y horario');
  lineas.push('-- semanal sin choques. Generado por tools/generar-horarios.js.');
  lineas.push('');
  lineas.push('-- 1. Docentes del grupo 5-6 (clases 8-13) pasan a los docentes del nivel.');
  lineas.push("UPDATE cursos_asignaturas SET id_docente = 14 WHERE id_clase BETWEEN 8 AND 13 AND id_asignatura = 1;");
  lineas.push("UPDATE cursos_asignaturas SET id_docente = 15 WHERE id_clase BETWEEN 8 AND 13 AND id_asignatura = 3;");
  lineas.push("UPDATE cursos_asignaturas SET id_docente = 13 WHERE id_clase BETWEEN 8 AND 13 AND id_asignatura = 9;");
  lineas.push("UPDATE cursos_asignaturas SET id_docente = 18 WHERE id_clase BETWEEN 8 AND 13 AND id_asignatura = 13;");
  lineas.push("UPDATE cursos_asignaturas SET id_docente = 17 WHERE id_clase BETWEEN 8 AND 13 AND id_asignatura = 10;");
  lineas.push("UPDATE cursos_asignaturas SET id_docente = 19 WHERE id_clase BETWEEN 8 AND 13 AND id_asignatura = 11;");
  lineas.push("UPDATE cursos_asignaturas SET id_docente = 21 WHERE id_clase BETWEEN 8 AND 13 AND id_asignatura = 12;");
  lineas.push("UPDATE cursos_asignaturas SET id_docente = 20 WHERE id_clase BETWEEN 8 AND 13 AND id_asignatura = 14;");
  lineas.push("UPDATE cursos_asignaturas SET id_docente = 22 WHERE id_clase BETWEEN 8 AND 13 AND id_asignatura = 15;");
  lineas.push('');
  lineas.push('-- 4B: Lenguaje y Matematica a los docentes nuevos 24/25.');
  lineas.push("UPDATE cursos_asignaturas SET id_docente = 24 WHERE id_clase = 7 AND id_asignatura = 1;");
  lineas.push("UPDATE cursos_asignaturas SET id_docente = 25 WHERE id_clase = 7 AND id_asignatura = 3;");
  lineas.push("UPDATE cursos_asignaturas SET caracter = 'OPTATIVA' WHERE id_clase = 7 AND id_asignatura = 10;");
  lineas.push('');
  lineas.push('-- 2. Artes/Musica como electiva en 7-8 (misma franja, alumno elige).');
  lineas.push("UPDATE cursos_asignaturas SET caracter = 'ELECTIVA' WHERE id_clase BETWEEN 1 AND 6 AND id_asignatura IN (11, 12);");
  lineas.push('');
  lineas.push('-- 3. Educacion Financiera sale de 5-6.');
  lineas.push("UPDATE cursos_asignaturas SET active = FALSE WHERE id_clase BETWEEN 8 AND 13 AND id_asignatura = 17;");
  lineas.push("UPDATE inscripciones SET estado = 'CANCELADO' WHERE curso_asignatura_id IN (SELECT id FROM cursos_asignaturas WHERE id_clase BETWEEN 8 AND 13 AND id_asignatura = 17);");
  lineas.push('');
  lineas.push('-- 4. Altas: Lengua de Senas y Religion en 5-6 (ids 144-155).');
  lineas.push('INSERT INTO cursos_asignaturas (id, id_asignatura, id_clase, id_docente, semestre, caracter, cupo_maximo, active) VALUES');
  const altas = [];
  for (let clase = 8; clase <= 13; clase++) {
    altas.push(`    (${143 + (clase - 7)}, 18, ${clase}, 26, 'SEMESTRE_1', 'OPTATIVA', NULL, TRUE)`);
  }
  for (let clase = 8; clase <= 13; clase++) {
    altas.push(`    (${149 + (clase - 7)}, 16, ${clase}, 23, 'SEMESTRE_1', 'OPTATIVA', NULL, TRUE)`);
  }
  lineas.push(altas.join(',\n') + ';');
  lineas.push('');
  lineas.push('-- 5. Inscripciones de la electiva Artes/Musica (7-8).');
  lineas.push('INSERT INTO inscripciones (id_alumno, curso_asignatura_id, estado, fecha_inscripcion) VALUES');
  lineas.push(
    inscripcionesElectiva
      .map(([alumno, curso]) => `    (${alumno}, ${curso}, 'ACTIVO', NOW(6))`)
      .join(',\n') + ';',
  );
  lineas.push('');
  lineas.push('-- 6. Horario semanal regenerado.');
  lineas.push('DELETE FROM horarios;');
  lineas.push('INSERT INTO horarios (curso_asignatura_id, dia, horario_entrada, horario_salida, ubicacion, active) VALUES');
  lineas.push(filasHorarios(solucion).join(',\n') + ';');
  lineas.push('');
  lineas.push('-- 7. Malla curricular: horas semanales por nivel (horas de 45 min).');
  lineas.push(mallaSql());
  lineas.push('');
  return lineas.join('\n');
}

function main() {
  const items = construirItems();
  let solucion = null;
  for (let intento = 1; intento <= 40 && !solucion; intento++) {
    solucion = resolver(items, 1000 + intento);
    if (solucion) {
      validar(solucion, items);
      console.log(`Solucion encontrada en el intento ${intento}`);
    }
  }
  if (!solucion) {
    throw new Error('No se encontro solucion factible');
  }
  const destino = path.join(__dirname, '..', 'src', 'main', 'resources', 'db', 'migration', 'V4__recalcular_horarios_mock.sql');
  fs.writeFileSync(destino, migracion(solucion), 'utf8');
  console.log(`Migracion escrita en ${destino}`);
}

main();

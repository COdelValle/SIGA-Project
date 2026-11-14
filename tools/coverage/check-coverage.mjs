import { readFile } from 'node:fs/promises';

const path = 'tools/coverage/coverage-summary.json';
const enforce = process.argv.includes('--enforce');
const backendTarget = Number(process.env.COVERAGE_BACKEND_TARGET ?? 55);
const frontendTarget = Number(process.env.COVERAGE_FRONTEND_TARGET ?? 45);

let summary;
try {
  summary = JSON.parse(await readFile(path, 'utf8'));
} catch {
  console.error(`No se encontro ${path}`);
  process.exit(1);
}

const backend = summary.backend?.business?.lines ?? 0;
const frontend = summary.frontend?.business?.lines ?? 0;
const rows = [
  { name: 'backend', actual: backend, target: backendTarget },
  { name: 'frontend', actual: frontend, target: frontendTarget },
];

for (const row of rows) {
  const status = row.actual >= row.target ? 'OK' : 'BAJO OBJETIVO';
  console.log(
    `Cobertura ${row.name}: ${row.actual}% (objetivo ${row.target}%) -> ${status}`,
  );
}

const below = rows.filter((row) => row.actual < row.target);
if (below.length > 0) {
  const message = `Cobertura bajo objetivo: ${below
    .map((row) => `${row.name} ${row.actual}% < ${row.target}%`)
    .join(', ')}`;
  if (enforce) {
    console.error(message);
    process.exit(1);
  }
  console.warn(`${message}. Modo informativo (sin --enforce).`);
}

import { readFile, writeFile } from 'node:fs/promises';

const path = 'tools/coverage/coverage-summary.json';
const baselinePath = 'tools/coverage/baseline.json';
const enforce = process.argv.includes('--enforce');
const updateBaseline = process.argv.includes('--update-baseline');
const backendTarget = Number(process.env.COVERAGE_BACKEND_TARGET ?? 70);
const frontendTarget = Number(process.env.COVERAGE_FRONTEND_TARGET ?? 60);

let summary;
try {
  summary = JSON.parse(await readFile(path, 'utf8'));
} catch {
  console.error(`No se encontro ${path}`);
  process.exit(1);
}

let baseline = {};
try {
  baseline = JSON.parse(await readFile(baselinePath, 'utf8'));
} catch {
  baseline = {};
}

const backend = summary.backend?.business?.lines ?? 0;
const frontend = summary.frontend?.business?.lines ?? 0;
const rows = [
  {
    name: 'backend',
    actual: backend,
    target: backendTarget,
    baseline: Number(baseline.backend ?? 0),
  },
  {
    name: 'frontend',
    actual: frontend,
    target: frontendTarget,
    baseline: Number(baseline.frontend ?? 0),
  },
];

if (updateBaseline) {
  await writeFile(
    baselinePath,
    `${JSON.stringify(
      {
        backend,
        frontend,
        updatedAt: new Date().toISOString(),
      },
      null,
      2,
    )}\n`,
  );
  console.log(`Baseline de cobertura actualizado: backend=${backend}% frontend=${frontend}%`);
  process.exit(0);
}

for (const row of rows) {
  const targetStatus = row.actual >= row.target ? 'OK' : 'BAJO OBJETIVO';
  const baselineStatus = row.actual >= row.baseline ? 'OK' : 'REGRESION';
  console.log(
    `Cobertura ${row.name}: ${row.actual}% (objetivo ${row.target}%, baseline ${row.baseline}%) -> ${targetStatus} / ${baselineStatus}`,
  );
}

const below = rows.filter((row) => row.actual < row.target);
const regressions = rows.filter((row) => row.actual < row.baseline);
const messages = [];
if (below.length > 0) {
  messages.push(
    `Cobertura bajo objetivo: ${below
      .map((row) => `${row.name} ${row.actual}% < ${row.target}%`)
      .join(', ')}`,
  );
}
if (regressions.length > 0) {
  messages.push(
    `Regresion de cobertura: ${regressions
      .map((row) => `${row.name} ${row.actual}% < baseline ${row.baseline}%`)
      .join(', ')}`,
  );
}

if (messages.length > 0) {
  const message = messages.join('. ');
  if (enforce) {
    console.error(message);
    process.exit(1);
  }
  console.warn(`${message}. Modo informativo (sin --enforce).`);
}

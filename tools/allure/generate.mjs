import { spawnSync } from 'node:child_process';
import { readdir, stat } from 'node:fs/promises';
import { join, resolve } from 'node:path';

const roots = ['apps/backend', 'apps/frontend', 'tools'];
const resultDirs = [];

async function walk(path) {
  const info = await stat(path);
  if (!info.isDirectory()) {
    return;
  }
  if (path.endsWith('allure-results') || path.endsWith('allure-results-e2e')) {
    resultDirs.push(resolve(path));
    return;
  }
  for (const entry of await readdir(path)) {
    if (entry === 'node_modules' || entry === '.git') {
      continue;
    }
    await walk(join(path, entry));
  }
}

for (const root of roots) {
  try {
    await walk(root);
  } catch {
    // Directorio opcional.
  }
}

const coverageResults = resolve('tools/coverage/allure-results');
try {
  await stat(coverageResults);
  resultDirs.push(coverageResults);
} catch {
  // Cobertura opcional en ejecuciones locales.
}

if (resultDirs.length === 0) {
  console.error('No se encontraron directorios de resultados Allure.');
  process.exit(1);
}

const uniqueResultDirs = [...new Set(resultDirs)];
resultDirs.length = 0;
resultDirs.push(...uniqueResultDirs);

const enrich = spawnSync(
  process.execPath,
  ['tools/allure/enrich-results.mjs', ...resultDirs],
  { stdio: 'inherit' },
);
if (enrich.status !== 0) {
  process.exit(enrich.status ?? 1);
}

const verify = spawnSync(
  process.execPath,
  ['tools/allure/verify-taxonomy.mjs', ...resultDirs],
  { stdio: 'inherit' },
);
if (verify.status !== 0) {
  process.exit(verify.status ?? 1);
}

const result = spawnSync(
  'npx',
  ['allure', 'generate', ...resultDirs, '--config', 'allurerc.mjs', '--clean'],
  { stdio: 'inherit', shell: process.platform === 'win32' },
);

process.exit(result.status ?? 1);

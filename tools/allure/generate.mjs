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

if (resultDirs.length === 0) {
  console.error('No se encontraron directorios de resultados Allure.');
  process.exit(1);
}

const result = spawnSync(
  'npx',
  ['allure', 'generate', ...resultDirs, '--config', 'allurerc.mjs', '--clean'],
  { stdio: 'inherit', shell: process.platform === 'win32' },
);

process.exit(result.status ?? 1);

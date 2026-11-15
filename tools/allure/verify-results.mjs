import { readdir, readFile, stat } from 'node:fs/promises';
import { join, resolve } from 'node:path';

const roots = process.argv.slice(2);
if (roots.length === 0) {
  roots.push('allure-results-download');
}
const counts = {
  backend: 0,
  frontend: 0,
  e2e: 0,
  infra: 0,
  total: 0,
};

async function walk(path) {
  const info = await stat(path);
  if (!info.isDirectory()) {
    return;
  }
  for (const entry of await readdir(path)) {
    const child = join(path, entry);
    const childInfo = await stat(child);
    if (childInfo.isDirectory()) {
      await walk(child);
      continue;
    }
    if (!entry.endsWith('-result.json')) {
      continue;
    }

    const result = JSON.parse(await readFile(child, 'utf8'));
    const labels = new Map(
      (result.labels ?? []).map((label) => [label.name, label.value]),
    );
    const framework = labels.get('framework');
    const layer = labels.get('layer');

    counts.total += 1;
    if (framework === 'junit-platform') {
      counts.backend += 1;
    }
    if (framework === 'vitest') {
      counts.frontend += 1;
    }
    if (framework === 'playwright') {
      counts.e2e += 1;
    }
    if (layer === 'infra') {
      counts.infra += 1;
    }
  }
}

for (const root of roots) {
  await walk(resolve(root));
}

console.log(
  `Allure results: backend=${counts.backend} frontend=${counts.frontend} e2e=${counts.e2e} infra=${counts.infra} total=${counts.total}`,
);

const missing = Object.entries(counts)
  .filter(([name, value]) => name !== 'total' && value === 0)
  .map(([name]) => name);

if (missing.length > 0) {
  console.error(`Faltan resultados de Allure para: ${missing.join(', ')}`);
  process.exit(1);
}

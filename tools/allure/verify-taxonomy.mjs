import { readdir, readFile, stat } from 'node:fs/promises';
import { basename, join, resolve } from 'node:path';

const roots = process.argv.slice(2);
if (roots.length === 0) {
  roots.push('allure-results');
}

const allowedLayers = new Set([
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

const requiredLabels = ['layer', 'epic', 'feature', 'story', 'owner'];
const counts = new Map();
const problems = [];

async function walk(path) {
  const info = await stat(path);
  if (info.isDirectory()) {
    for (const entry of await readdir(path)) {
      if (entry !== 'node_modules') {
        await walk(join(path, entry));
      }
    }
    return;
  }
  if (!basename(path).endsWith('-result.json')) {
    return;
  }

  const result = JSON.parse(await readFile(path, 'utf8'));
  const labels = new Map((result.labels ?? []).map((label) => [label.name, label.value]));
  const layer = labels.get('layer') ?? '(sin layer)';
  counts.set(layer, (counts.get(layer) ?? 0) + 1);

  const missing = requiredLabels.filter((name) => !labels.get(name));
  if (missing.length > 0) {
    problems.push(`${path}: faltan labels ${missing.join(', ')}`);
  }
  if (layer !== '(sin layer)' && !allowedLayers.has(layer)) {
    problems.push(`${path}: layer invalido '${layer}'`);
  }
  if (result.name && (result.name.endsWith('()') || !result.name.includes(' '))) {
    problems.push(`${path}: nombre no legible '${result.name}'`);
  }
}

for (const root of roots) {
  await walk(resolve(root));
}

console.log(
  `Taxonomia Allure: ${[...counts.entries()]
    .sort(([left], [right]) => left.localeCompare(right))
    .map(([layer, count]) => `${layer}=${count}`)
    .join(' ')}`,
);

if (problems.length > 0) {
  console.error(problems.slice(0, 50).join('\n'));
  if (problems.length > 50) {
    console.error(`... y ${problems.length - 50} problemas mas`);
  }
  process.exit(1);
}

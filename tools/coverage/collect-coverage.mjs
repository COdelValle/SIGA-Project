import { randomUUID } from 'node:crypto';
import { mkdir, readdir, readFile, stat, writeFile } from 'node:fs/promises';
import { join, resolve } from 'node:path';

const args = process.argv.slice(2);
const root = resolve(argument('--root') ?? 'coverage-download');
const output = resolve(argument('--output') ?? 'tools/coverage');
const resultsDir = join(output, 'allure-results');

function argument(name) {
  const index = args.indexOf(name);
  return index >= 0 ? args[index + 1] : undefined;
}

function percent(covered, total) {
  if (!total) {
    return 0;
  }
  return Math.round((10000 * covered) / total) / 100;
}

function moduleOfClass(className) {
  const match = className.match(/^cl\/siga\/([^/]+)\//);
  return match?.[1] ?? 'otros';
}

function isGeneratedOrData(className) {
  const simple = className.split('/').pop() ?? '';
  return (
    simple.endsWith('MapperImpl') ||
    simple.endsWith('Application') ||
    className.includes('/dto/') ||
    className.includes('/model/entity/') ||
    className.includes('/config/')
  );
}

async function readBackendCoverage(paths, summary) {
  for (const path of paths) {
    const xml = await readFile(path, 'utf8');
    const classPattern = /<class name="([^"]+)"[^>]*>([\s\S]*?)<\/class>/g;
    let match;
    while ((match = classPattern.exec(xml)) !== null) {
      const className = match[1];
      const counters = countersOf(match[2]);
      const module = moduleOfClass(className);
      summary.backend.raw = add(summary.backend.raw, counters);
      summary.backend.modules[module] ??= { raw: empty(), business: empty() };
      summary.backend.modules[module].raw = add(summary.backend.modules[module].raw, counters);
      if (!isGeneratedOrData(className)) {
        summary.backend.business = add(summary.backend.business, counters);
        summary.backend.modules[module].business = add(
          summary.backend.modules[module].business,
          counters,
        );
      }
    }
  }
}

function countersOf(block) {
  const result = empty();
  const counterPattern = /<counter type="(LINE|BRANCH|INSTRUCTION|METHOD)" missed="(\d+)" covered="(\d+)"\/>/g;
  let match;
  while ((match = counterPattern.exec(block)) !== null) {
    const key = match[1].toLowerCase();
    result[key].missed += Number(match[2]);
    result[key].covered += Number(match[3]);
  }
  return result;
}

function empty() {
  return {
    line: { missed: 0, covered: 0 },
    branch: { missed: 0, covered: 0 },
    instruction: { missed: 0, covered: 0 },
    method: { missed: 0, covered: 0 },
  };
}

function add(left, right) {
  for (const key of Object.keys(left)) {
    left[key].missed += right[key].missed;
    left[key].covered += right[key].covered;
  }
  return left;
}

function normalize(counters) {
  return {
    lines: percent(counters.line.covered, counters.line.covered + counters.line.missed),
    branches: percent(
      counters.branch.covered,
      counters.branch.covered + counters.branch.missed,
    ),
    instructions: percent(
      counters.instruction.covered,
      counters.instruction.covered + counters.instruction.missed,
    ),
    functions: percent(
      counters.method.covered,
      counters.method.covered + counters.method.missed,
    ),
    raw: counters,
  };
}

async function readFrontendCoverage(paths, summary) {
  for (const path of paths) {
    const lines = (await readFile(path, 'utf8')).split(/\r?\n/);
    let file;
    let lf = 0;
    let lh = 0;
    let brf = 0;
    let brh = 0;
    let fnf = 0;
    let fnh = 0;
    for (const line of lines) {
      if (line.startsWith('SF:')) {
        file = line.slice(3);
        lf = 0;
        lh = 0;
        brf = 0;
        brh = 0;
        fnf = 0;
        fnh = 0;
      } else if (line.startsWith('LF:')) {
        lf = Number(line.slice(3));
      } else if (line.startsWith('LH:')) {
        lh = Number(line.slice(3));
      } else if (line.startsWith('BRF:')) {
        brf = Number(line.slice(4));
      } else if (line.startsWith('BRH:')) {
        brh = Number(line.slice(4));
      } else if (line.startsWith('FNF:')) {
        fnf = Number(line.slice(4));
      } else if (line.startsWith('FNH:')) {
        fnh = Number(line.slice(4));
      } else if (line === 'end_of_record' && file) {
        const normalized = file.replace(/\\/g, '/');
        const lib = normalized.match(/libs\/([^/]+)\//)?.[1] ?? 'app';
        const raw = { lines: lf, hitLines: lh, branches: brf, hitBranches: brh, functions: fnf, hitFunctions: fnh };
        summary.frontend.raw = addFrontend(summary.frontend.raw, raw);
        summary.frontend.libs[lib] ??= { raw: emptyFrontend(), business: emptyFrontend() };
        summary.frontend.libs[lib].raw = addFrontend(summary.frontend.libs[lib].raw, raw);
        if (!normalized.includes('/libs/mocks/') && !/\.model\.ts$/.test(normalized) && !normalized.endsWith('/src/main.ts')) {
          summary.frontend.business = addFrontend(summary.frontend.business, raw);
          summary.frontend.libs[lib].business = addFrontend(summary.frontend.libs[lib].business, raw);
        }
        file = undefined;
      }
    }
  }
}

function emptyFrontend() {
  return {
    lines: 0,
    hitLines: 0,
    branches: 0,
    hitBranches: 0,
    functions: 0,
    hitFunctions: 0,
  };
}

function addFrontend(left, right) {
  left.lines += right.lines;
  left.hitLines += right.hitLines;
  left.branches += right.branches;
  left.hitBranches += right.hitBranches;
  left.functions += right.functions;
  left.hitFunctions += right.hitFunctions;
  return left;
}

function normalizeFrontend(counters) {
  return {
    lines: percent(counters.hitLines, counters.lines),
    branches: percent(counters.hitBranches, counters.branches),
    functions: percent(counters.hitFunctions, counters.functions),
    raw: counters,
  };
}

async function findFiles(path, fileName, found) {
  const info = await stat(path);
  if (info.isDirectory()) {
    for (const entry of await readdir(path)) {
      if (entry !== 'node_modules') {
        await findFiles(join(path, entry), fileName, found);
      }
    }
    return;
  }
  if (path.endsWith(fileName)) {
    found.push(path);
  }
}

function result({ name, feature, story, metrics, link }) {
  const uuid = randomUUID();
  const detail = Object.entries(metrics)
    .filter(([key]) => key !== 'raw')
    .map(([key, value]) => `${key}: ${value}%`)
    .join(', ');
  return {
    uuid,
    historyId: `coverage-${name}`,
    name,
    fullName: name,
    status: 'passed',
    statusDetails: { message: detail },
    stage: 'finished',
    start: Date.now(),
    stop: Date.now(),
    labels: [
      { name: 'layer', value: 'coverage' },
      { name: 'epic', value: 'Calidad' },
      { name: 'feature', value: feature },
      { name: 'story', value: story },
      { name: 'owner', value: 'qa' },
      { name: 'severity', value: 'normal' },
      { name: 'tag', value: 'coverage' },
    ],
    parameters: Object.entries(metrics)
      .filter(([key]) => key !== 'raw')
      .map(([key, value]) => ({ name: key, value: `${value}%` })),
    steps: [],
    attachments: [],
    links: link ? [{ name: 'HTML', url: link, type: 'custom' }] : [],
    description: `Cobertura ${name}: ${detail}`,
  };
}

const summary = {
  generatedAt: new Date().toISOString(),
  backend: { raw: empty(), business: empty(), modules: {} },
  frontend: { raw: emptyFrontend(), business: emptyFrontend(), libs: {} },
};

const jacocoFiles = [];
const lcovFiles = [];
await findFiles(root, 'jacoco.xml', jacocoFiles);
await findFiles(root, 'lcov.info', lcovFiles);

if (jacocoFiles.length === 0 && lcovFiles.length === 0) {
  console.error(`No se encontro cobertura en ${root}`);
  process.exit(1);
}

await readBackendCoverage(jacocoFiles, summary);
await readFrontendCoverage(lcovFiles, summary);

const normalized = {
  backend: {
    raw: normalize(summary.backend.raw),
    business: normalize(summary.backend.business),
    modules: Object.fromEntries(
      Object.entries(summary.backend.modules).map(([name, value]) => [
        name,
        { raw: normalize(value.raw), business: normalize(value.business) },
      ]),
    ),
  },
  frontend: {
    raw: normalizeFrontend(summary.frontend.raw),
    business: normalizeFrontend(summary.frontend.business),
    libs: Object.fromEntries(
      Object.entries(summary.frontend.libs).map(([name, value]) => [
        name,
        { raw: normalizeFrontend(value.raw), business: normalizeFrontend(value.business) },
      ]),
    ),
  },
};

await mkdir(resultsDir, { recursive: true });
const summaryPath = join(output, 'coverage-summary.json');
await writeFile(summaryPath, JSON.stringify(normalized, null, 2), 'utf8');
await writeFile(
  join(resultsDir, `${randomUUID()}-attachment.json`),
  JSON.stringify(normalized, null, 2),
  'utf8',
);

const backendLink = 'https://codelvalle.github.io/SIGA-Project/dev/coverage/backend/';
const frontendLink = 'https://codelvalle.github.io/SIGA-Project/dev/coverage/frontend/';

const results = [
  result({
    name: 'Cobertura backend',
    feature: 'Cobertura backend',
    story: 'Cobertura de codigo de negocio',
    metrics: normalized.backend.business,
    link: backendLink,
  }),
  result({
    name: 'Cobertura frontend',
    feature: 'Cobertura frontend',
    story: 'Cobertura de codigo de negocio',
    metrics: normalized.frontend.business,
    link: frontendLink,
  }),
];

for (const [module, value] of Object.entries(normalized.backend.modules)) {
  results.push(
    result({
      name: `Cobertura backend ${module}`,
      feature: 'Cobertura backend',
      story: module,
      metrics: value.business,
      link: backendLink,
    }),
  );
}
for (const [lib, value] of Object.entries(normalized.frontend.libs)) {
  results.push(
    result({
      name: `Cobertura frontend ${lib}`,
      feature: 'Cobertura frontend',
      story: lib,
      metrics: value.business,
      link: frontendLink,
    }),
  );
}

for (const item of results) {
  await writeFile(join(resultsDir, `${item.uuid}-result.json`), JSON.stringify(item, null, 2), 'utf8');
}

console.log(
  `Cobertura: backend negocio ${normalized.backend.business.lines}% / frontend negocio ${normalized.frontend.business.lines}%`,
);

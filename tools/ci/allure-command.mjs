import { spawnSync } from 'node:child_process';
import { mkdir, writeFile } from 'node:fs/promises';
import { randomUUID } from 'node:crypto';
import { join } from 'node:path';

const args = process.argv.slice(2);
const separator = args.indexOf('--');
if (separator < 0 || separator === args.length - 1) {
  console.error('Uso: node tools/ci/allure-command.mjs --name "<nombre>" [--dir <dir>] -- <comando> [args...]');
  process.exit(2);
}

const options = args.slice(0, separator);
const commandArgs = args.slice(separator + 1);

function optionValue(flag, fallback) {
  const index = options.indexOf(flag);
  return index >= 0 ? options[index + 1] : fallback;
}

const name = optionValue('--name', commandArgs.join(' '));
const outputDir = optionValue('--dir', 'tools/ci/allure-results');
const layer = optionValue('--layer', 'infra');
const epic = optionValue('--epic', 'Calidad');
const feature = optionValue('--feature', 'Infraestructura');
const story = optionValue('--story', 'Validacion de comandos');
const owner = optionValue('--owner', 'qa');
const tag = optionValue('--tag', 'infra');
const [command, ...rest] = commandArgs;

const started = Date.now();
const result = spawnSync(command, rest, { encoding: 'utf8' });
const stopped = Date.now();
const passed = result.status === 0;
const uuid = randomUUID();
const log = [
  `$ ${commandArgs.join(' ')}`,
  '',
  `exitCode: ${result.status ?? 'unknown'}`,
  '',
  'stdout:',
  result.stdout ?? '',
  '',
  'stderr:',
  result.stderr ?? '',
].join('\n');

await mkdir(outputDir, { recursive: true });
const attachmentName = `${uuid}-log.txt`;
await writeFile(join(outputDir, attachmentName), log, 'utf8');

const allureResult = {
  uuid,
  historyId: `ci-${name}`,
  name,
  fullName: name,
  status: passed ? 'passed' : 'failed',
  statusDetails: passed ? {} : { message: `Comando finalizo con codigo ${result.status}` },
  stage: 'finished',
  start: started,
  stop: stopped,
  labels: [
    { name: 'layer', value: layer },
    { name: 'epic', value: epic },
    { name: 'feature', value: feature },
    { name: 'story', value: story },
    { name: 'owner', value: owner },
    { name: 'tag', value: tag },
  ],
  parameters: [],
  steps: [],
  attachments: [
    { name: 'log', source: attachmentName, type: 'text/plain' },
  ],
  links: [],
};

await writeFile(join(outputDir, `${uuid}-result.json`), JSON.stringify(allureResult, null, 2), 'utf8');
console.log(`${name}: ${passed ? 'passed' : 'failed'}`);
process.exit(result.status ?? 1);

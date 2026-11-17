import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const configPath = resolve(
  root,
  'apps/backend/ms-reportes/src/main/java/cl/siga/msreportes/config/KafkaConsumerConfig.java',
);

function fail(message) {
  console.error(`kafka-error-policy: ${message}`);
  process.exit(1);
}

let source;
try {
  source = readFileSync(configPath, 'utf8');
} catch (error) {
  fail(`no se pudo leer ${configPath}: ${error.message}`);
}

const missing = [];
if (!source.includes('DefaultErrorHandler')) {
  missing.push('DefaultErrorHandler');
}
if (!source.includes('FixedBackOff(1000L, 2L)')) {
  missing.push('FixedBackOff(1000L, 2L)');
}
if (missing.length > 0) {
  fail(`falta la politica esperada en KafkaConsumerConfig: ${missing.join(', ')}`);
}
if (source.includes('DeadLetterPublishingRecoverer')) {
  fail('se encontro DeadLetterPublishingRecoverer; la politica esperada es log y descarte sin DLQ');
}

console.log('kafka-error-policy: OK (2 reintentos de 1000 ms, log y descarte sin DLQ)');

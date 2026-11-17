import { readFileSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..');

function fail(message) {
  console.error(`kafka-topic-policy: ${message}`);
  process.exit(1);
}

function read(relativePath) {
  const path = resolve(root, relativePath);
  try {
    return readFileSync(path, 'utf8');
  } catch (error) {
    fail(`no se pudo leer ${relativePath}: ${error.message}`);
  }
}

function requireTokens(label, source, tokens) {
  const missing = tokens.filter((token) => !source.includes(token));
  if (missing.length > 0) {
    fail(`${label} no contiene: ${missing.join(', ')}`);
  }
}

const provisioning = read(
  'apps/backend/ms-kafka-admin/src/main/java/cl/siga/ms_kafka/config/TopicProvisioningConfig.java',
);
requireTokens('TopicProvisioningConfig.java', provisioning, [
  'PARTICIONES = 3',
  'FACTOR_REPLICACION = 3',
  '604800000',
  '134217728',
  'CLEANUP_POLICY_DELETE',
  'siga.academico.asistencias',
  'siga.academico.rendimiento',
]);

const composeTokens = [
  'KAFKA_MIN_INSYNC_REPLICAS: 2',
  'KAFKA_AUTO_CREATE_TOPICS_ENABLE: "false"',
];
for (const composePath of [
  'docker-compose.yml',
  'infra/terraform/templates/docker-compose.yml',
]) {
  requireTokens(composePath, read(composePath), composeTokens);
}

console.log(
  'kafka-topic-policy: OK (3 particiones, RF 3, min ISR 2, retencion 7d/128MiB, auto-create deshabilitado)',
);

import { spawnSync } from 'node:child_process';

const BOOTSTRAP = 'kafka-1:29092';
const KAFKA_CONTAINER = 'kafka-1';
const MARIA_CONTAINER = 'mariadb-reportes';
const MARIA_DB = 'siga_reportes_db';
const TOPIC_RENDIMIENTO = 'siga.academico.rendimiento';
const GROUP_RENDIMIENTO = 'siga-reportes-rendimiento';
const BROKER_RECORDS = 20000;
const BROKER_MIN_RATE = 1000;
const CONSUMER_EVENTS = 300;
const CONSUMER_TIMEOUT_MS = 120000;

function usage() {
  console.error('Uso: node tools/performance/kafka-load.mjs <broker|consumer>');
  process.exit(2);
}

function docker(args, options = {}) {
  return spawnSync('docker', args, { encoding: 'utf8', ...options });
}

function must(result, label) {
  if (result.error) {
    throw new Error(`${label}: ${result.error.message}`);
  }
  if (result.status !== 0) {
    const detail = `${result.stderr ?? ''}`.trim() || `${result.stdout ?? ''}`.trim();
    throw new Error(`${label} fallo con codigo ${result.status}: ${detail}`);
  }
  return result;
}

function timestamp() {
  const now = new Date();
  const pad = (value) => String(value).padStart(2, '0');
  return (
    `${now.getFullYear()}${pad(now.getMonth() + 1)}${pad(now.getDate())}` +
    `${pad(now.getHours())}${pad(now.getMinutes())}${pad(now.getSeconds())}`
  );
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function broker() {
  const topic = `siga.e3.local-load-${timestamp()}`;
  let created = false;
  let failure;

  try {
    must(
      docker([
        'exec',
        KAFKA_CONTAINER,
        'kafka-topics',
        '--bootstrap-server',
        BOOTSTRAP,
        '--create',
        '--topic',
        topic,
        '--partitions',
        '3',
        '--replication-factor',
        '3',
        '--config',
        'min.insync.replicas=2',
        '--config',
        'retention.ms=3600000',
        '--config',
        'retention.bytes=1048576',
      ]),
      'crear topic temporal',
    );
    created = true;

    const perf = must(
      docker([
        'exec',
        KAFKA_CONTAINER,
        'kafka-producer-perf-test',
        '--topic',
        topic,
        '--num-records',
        String(BROKER_RECORDS),
        '--record-size',
        '512',
        '--throughput',
        '-1',
        '--producer-props',
        `bootstrap.servers=${BOOTSTRAP}`,
        'acks=all',
        'request.timeout.ms=10000',
        'delivery.timeout.ms=30000',
      ]),
      'kafka-producer-perf-test',
    );

    const output = `${perf.stdout ?? ''}\n${perf.stderr ?? ''}`;
    const sentMatch = output.match(/(\d+) records sent/);
    const rateMatch = output.match(/([\d.]+) records\/sec/);
    if (!sentMatch) {
      throw new Error(`no se pudo parsear "records sent" en la salida: ${output.trim()}`);
    }
    const sent = Number(sentMatch[1]);
    const rate = rateMatch ? Number(rateMatch[1]) : 0;
    const hasErrors = /exception|records failed [1-9]/i.test(output);

    const offsetsOutput = must(
      docker([
        'exec',
        KAFKA_CONTAINER,
        'kafka-get-offsets',
        '--bootstrap-server',
        BOOTSTRAP,
        '--topic',
        topic,
      ]),
      'leer offsets',
    ).stdout;

    const offsetsTotal = offsetsOutput
      .split(/\r?\n/)
      .filter(Boolean)
      .map((line) => Number(line.split(':').pop()))
      .reduce((sum, value) => sum + value, 0);

    const problems = [];
    if (sent !== BROKER_RECORDS) {
      problems.push(`records sent ${sent} != ${BROKER_RECORDS}`);
    }
    if (hasErrors) {
      problems.push('la salida del productor contiene errores');
    }
    if (rate < BROKER_MIN_RATE) {
      problems.push(`throughput ${rate} rec/s < ${BROKER_MIN_RATE} rec/s`);
    }
    if (offsetsTotal < BROKER_RECORDS) {
      problems.push(`offsets totales ${offsetsTotal} < ${BROKER_RECORDS}`);
    }

    console.log(`topic=${topic}`);
    console.log(`particiones=3 replication_factor=3 min_insync_replicas=2`);
    console.log(`records_sent=${sent}`);
    console.log(`records_per_sec=${rate.toFixed(3)}`);
    console.log(`offsets_totales=${offsetsTotal}`);
    console.log(`errores=${hasErrors ? 'si' : '0'}`);

    if (problems.length > 0) {
      throw new Error(`criterios no cumplidos: ${problems.join('; ')}`);
    }
    console.log('RESULTADO broker: OK');
  } catch (error) {
    failure = error;
  } finally {
    if (created) {
      const deletion = docker([
        'exec',
        KAFKA_CONTAINER,
        'kafka-topics',
        '--bootstrap-server',
        BOOTSTRAP,
        '--delete',
        '--topic',
        topic,
      ]);
      if (deletion.status === 0) {
        console.log(`topic_temporal_eliminado=${topic}`);
      } else {
        console.error(`no se pudo eliminar el topic temporal ${topic}`);
      }
    }
  }

  if (failure) {
    throw failure;
  }
}

function readPassword() {
  return must(
    docker(['exec', MARIA_CONTAINER, 'printenv', 'MARIADB_ROOT_PASSWORD']),
    'leer credencial MariaDB',
  ).stdout.trim();
}

function maria(sql, password) {
  return must(
    docker(
      [
        'exec',
        '-e',
        'MYSQL_PWD',
        MARIA_CONTAINER,
        'mariadb',
        '-uroot',
        MARIA_DB,
        '-N',
        '-e',
        sql,
      ],
      { env: { ...process.env, MYSQL_PWD: password } },
    ),
    'consulta MariaDB',
  ).stdout;
}

function mariaCount(sql, password) {
  const output = maria(sql, password).trim();
  return Number(output.split(/\r?\n/).pop() ?? '0');
}

function buildEvents(suffix) {
  const lines = [];
  for (let index = 0; index < CONSUMER_EVENTS; index += 1) {
    const idEstudiante = 930000000000000000n + BigInt(index);
    const idNota = 940000000000000000n + BigInt(index);
    const idEvaluacion = 950000000000000000n + BigInt(index);
    const idEvento = `local-load-r-${suffix}-${index}`;
    const correlationId = `local-load-corr-${suffix}-${index}`;
    const json =
      `{"idNota":${idNota},"idEstudiante":${idEstudiante},` +
      `"idEvaluacion":${idEvaluacion},"idCursoAsignatura":null,` +
      `"puntaje":6.5,"promedio":6.5,"evaluacionesInsuficientes":0,` +
      `"timestamp":"2026-11-17T02:02:00",` +
      `"idEvento":"${idEvento}","correlationId":"${correlationId}"}`;
    lines.push(`${idEstudiante}:${json}`);
  }
  return lines.join('\n');
}

async function waitForLagZero() {
  const started = Date.now();
  let maxLag = 0;
  let lastLag = null;

  while (Date.now() - started < CONSUMER_TIMEOUT_MS) {
    const result = docker([
      'exec',
      KAFKA_CONTAINER,
      'kafka-consumer-groups',
      '--bootstrap-server',
      BOOTSTRAP,
      '--describe',
      '--group',
      GROUP_RENDIMIENTO,
      '--timeout',
      '10000',
    ]);
    const output = result.stdout ?? '';
    const rows = output
      .split(/\r?\n/)
      .filter((line) => line.includes(TOPIC_RENDIMIENTO));
    const lags = rows
      .map((line) => Number(line.trim().split(/\s+/)[5]))
      .filter((value) => Number.isFinite(value));
    if (lags.length > 0) {
      const totalLag = lags.reduce((sum, value) => sum + value, 0);
      maxLag = Math.max(maxLag, totalLag);
      lastLag = totalLag;
      if (totalLag === 0) {
        return { seconds: (Date.now() - started) / 1000, maxLag };
      }
    }
    await sleep(2000);
  }

  throw new Error(
    `timeout de ${CONSUMER_TIMEOUT_MS / 1000}s esperando lag 0 (ultimo lag=${lastLag}, lag maximo=${maxLag})`,
  );
}

async function consumer() {
  const suffix = timestamp();
  const eventPrefix = `local-load-r-${suffix}-`;
  const correlationPrefix = `local-load-corr-${suffix}-`;
  const content = buildEvents(suffix);
  const password = readPassword();
  let failure;

  try {
    const publishStarted = Date.now();
    must(
      docker(
        [
          'exec',
          '-i',
          KAFKA_CONTAINER,
          'kafka-console-producer',
          '--bootstrap-server',
          BOOTSTRAP,
          '--topic',
          TOPIC_RENDIMIENTO,
          '--property',
          'parse.key=true',
          '--property',
          'key.separator=:',
          '--producer-property',
          'acks=all',
        ],
        { input: content },
      ),
      'publicar eventos de rendimiento',
    );
    const publishSeconds = (Date.now() - publishStarted) / 1000;

    const lag = await waitForLagZero();

    const proyecciones = mariaCount(
      `SELECT COUNT(*) FROM proyecciones_rendimiento WHERE id_evento LIKE '${eventPrefix}%';`,
      password,
    );
    const eventos = mariaCount(
      `SELECT COUNT(*) FROM eventos_reportes_procesados WHERE id_evento LIKE '${eventPrefix}%';`,
      password,
    );
    const alertas = mariaCount(
      `SELECT COUNT(*) FROM alertas_riesgo WHERE correlation_id LIKE '${correlationPrefix}%';`,
      password,
    );

    console.log(`eventos_publicados=${CONSUMER_EVENTS}`);
    console.log(`segundos_publicacion=${publishSeconds.toFixed(3)}`);
    console.log(`segundos_hasta_lag_0=${lag.seconds.toFixed(3)}`);
    console.log(`lag_maximo_observado=${lag.maxLag}`);
    console.log(`filas_proyecciones_rendimiento=${proyecciones}`);
    console.log(`filas_eventos_procesados=${eventos}`);
    console.log(`alertas_generadas=${alertas}`);

    const problems = [];
    if (proyecciones !== CONSUMER_EVENTS) {
      problems.push(`proyecciones ${proyecciones} != ${CONSUMER_EVENTS}`);
    }
    if (eventos !== CONSUMER_EVENTS) {
      problems.push(`eventos procesados ${eventos} != ${CONSUMER_EVENTS}`);
    }
    if (alertas !== 0) {
      problems.push(`alertas ${alertas} != 0`);
    }
    if (problems.length > 0) {
      throw new Error(`criterios no cumplidos: ${problems.join('; ')}`);
    }
    console.log('RESULTADO consumer: OK');
  } catch (error) {
    failure = error;
  } finally {
    const alertasBorradas = mariaCount(
      `DELETE FROM alertas_riesgo WHERE correlation_id LIKE '${correlationPrefix}%'; SELECT ROW_COUNT();`,
      password,
    );
    const proyeccionesBorradas = mariaCount(
      `DELETE FROM proyecciones_rendimiento WHERE id_evento LIKE '${eventPrefix}%'; SELECT ROW_COUNT();`,
      password,
    );
    const eventosBorrados = mariaCount(
      `DELETE FROM eventos_reportes_procesados WHERE id_evento LIKE '${eventPrefix}%'; SELECT ROW_COUNT();`,
      password,
    );
    console.log(
      `limpieza_filas alertas=${alertasBorradas} proyecciones=${proyeccionesBorradas} eventos=${eventosBorrados}`,
    );
  }

  if (failure) {
    throw failure;
  }
}

const subcommand = process.argv[2];
if (subcommand === 'broker') {
  await broker();
} else if (subcommand === 'consumer') {
  await consumer();
} else {
  usage();
}

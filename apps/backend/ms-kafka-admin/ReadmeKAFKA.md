# Provisionamiento de topics Kafka – SIGA Académico

## 1. Objetivo principal

Definir y provisionar los topics requeridos por SIGA Académico para el manejo de eventos de rendimiento y asistencia estudiantil.

Los topics se declaran mediante beans `NewTopic` en `TopicProvisioningConfig.java`, dentro del módulo `ms-kafka-admin`.

## 2. Configuración general

Ambos topics utilizan la siguiente configuración:

| Propiedad | Valor |
|---|---|
| Particiones | 3 |
| Factor de replicación (RF) | 3 |
| Política de limpieza | `delete` |
| Retención propuesta | 365 días |
| `retention.ms` | `31536000000` |

El factor de replicación RF=3 permite mantener tres réplicas de cada partición, distribuidas entre los tres brokers del clúster Kafka.

## 3. Topic de rendimiento

**Nombre:** `siga.academico.rendimiento`

- Particiones: 3.
- Factor de replicación: 3.
- Clave de mensaje recomendada: `estudiante_asignatura`.

La clave compuesta permite asociar los eventos de rendimiento con un estudiante y una asignatura. Los productores deberán construir y enviar esta clave al publicar los mensajes.

## 4. Topic de asistencias

**Nombre:** `siga.academico.asistencias`

- Particiones: 3.
- Factor de replicación: 3.
- Clave de mensaje recomendada: `estudiante_curso`.

La clave compuesta permite agrupar los eventos de asistencia de un estudiante en un curso determinado. Los productores deberán enviar esta clave al publicar los mensajes.

## 5. Política de limpieza y retención

Se propone utilizar:

`cleanup.policy=delete`

Esta política permite conservar los eventos históricos durante el período de retención configurado y eliminar posteriormente los segmentos que hayan vencido.

Se propone una retención de 365 días, equivalente a `31536000000` milisegundos, para disponer de un historial prolongado de eventos académicos.

La política `delete` se prefiere sobre `compact`, ya que el objetivo es conservar el historial de eventos y no mantener únicamente el último valor asociado a cada clave.

**Importante:** El período de 365 días corresponde a una propuesta técnica pendiente de validación y aprobación institucional.

## 6. Provisionamiento

La clase `TopicProvisioningConfig.java` declara los dos beans `NewTopic`.

Spring Kafka, mediante `KafkaAdmin`, administra su creación al iniciar la aplicación, siempre que la autoconfiguración administrativa esté habilitada y exista conectividad con el clúster.

## 7. Consideraciones

- Las claves de mensaje son responsabilidad de los productores; no son propiedades configurables del topic.
- Las tres particiones permiten distribuir los mensajes y facilitan el procesamiento paralelo.
- Los mensajes con la misma clave se asignan normalmente a la misma partición, lo que permite mantener su orden dentro de ella.
- La retención definitiva debe ajustarse a las políticas institucionales de conservación de información.

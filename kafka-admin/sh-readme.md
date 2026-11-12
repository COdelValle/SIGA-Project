# Como usar en Consola
## Administración de TOPICS

---

**Crear Topic**
- ``./kafka-admin.sh create topic-event 3 3``

EJEMPLO:
- Topic: asistencia-eventos.
- Particiones: 3.
- Factor de replicación: 3.

---

**Listar Topics**
- ``./kafka-admin.sh list``

---

**Describir Topic**
- ``./kafka-admin.sh describe asistencia-eventos``

Ejemplo de objetos:

| Objeto        | Descripción |
|-----------------|--------------------------------|
| Partition	   | Número de partición |
| Leader	      |Broker que lidera la partición|
| Replicas	    |Brokers que almacenan la réplica|
| Isr	Réplicas | sincronizadas|

---

## Topics - Políticas de retención

### Ejemplos:

**Tiempo retención - límite de almacenamiento**

7 Días de retención
- ``./kafka-admin.sh retention asistencia-eventos 604800000``

limite de almacenamiento
- ``./kafka-admin.sh retention asistencia-eventos 604800000 1073741824``

**Consulta a configs del Topic**
- ``./kafka-admin.sh config asistencia-eventos``

---

## Configs de Brokers

base
- ``./kafka-admin.sh brokers``

**Consultar brokers disponibles**
- ``docker exec kafka-1 kafka-topics
  --bootstrap-server kafka-1:29092,kafka-2:29092,kafka-3:29092
  --describe``

---

## Consumer Groups

**Listar los consumer Groups**
- ``./kafka-admin.sh groups``

**Inspección específica de un consumer**
- ``./kafka-admin.sh group asistencia-consumer-group``

---
### Offsets

**Consulta de offset más antigüo disponible**
- ``./kafka-admin.sh offsets asistencia-eventos earliest``

**Consulta del más reciente**
- ``./kafka-admin.sh offsets asistencia-eventos latest``

---

### Lags

**Consultar Lags**
- ``./kafka-admin.sh lag``

**Consultar el Lag específico de un grupo**
- ``./kafka-admin.sh lag asistencia-consumer-group``

Se incluye:
| Objeto | Respuesta |
|-|-|
| CURRENT-OFFSET | Último offset confirmado por el consumidor|
| LOG-END-OFFSET| Último offset disponible en el broker|
| LAG| Diferencia entre ambos|

**Interpetación**

- **LAG = 0:** el consumidor está al día.
- **LAG > 0:** existen mensajes pendientes de consumir.
- **LAG creciente:** puede existir un consumidor lento, detenido o con capacidad insuficiente.


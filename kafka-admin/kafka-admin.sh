
#!/bin/bash

# ==========================================================
# KAFKA CLUSTER ADMINISTRATION
# Confluent Platform 7.5.0
# ==========================================================

CONTAINER="kafka-1"

BOOTSTRAP="kafka-1:29092,kafka-2:29092,kafka-3:29092"

# ----------------------------------------------------------
# FUNCION: Ejecutar comandos Kafka dentro del contenedor
# ----------------------------------------------------------

kafka_exec() {
    docker exec "$CONTAINER" "$@"
}

# FUNCION: Ayuda
usage() {
    echo ""
    echo "KAFKA CLUSTER ADMINISTRATION"
    echo "======================================"
    echo ""
    echo "Topics:"
    echo "  $0 create <topic> [partitions] [replication]"
    echo "  $0 list"
    echo "  $0 describe <topic>"
    echo "  $0 retention <topic> <retention.ms> [retention.bytes]"
    echo "  $0 config <topic>"
    echo ""
    echo "Brokers:"
    echo "  $0 brokers"
    echo ""
    echo "Consumer Groups:"
    echo "  $0 groups"
    echo "  $0 group <group-id>"
    echo "  $0 offsets <topic> [earliest|latest]"
    echo "  $0 lag [group-id]"
    echo ""
    echo "======================================"
}

# 1. CREAR TOPIC
create_topic() {

    TOPIC=$1
    PARTITIONS=${2:-3}
    REPLICATION=${3:-3}

    if [[ -z "$TOPIC" ]]; then
        echo "ERROR: Debes indicar el nombre del topic."
        exit 1
    fi

    echo "Creando topic: $TOPIC"

    kafka_exec kafka-topics \
        --bootstrap-server "$BOOTSTRAP" \
        --create \
        --topic "$TOPIC" \
        --partitions "$PARTITIONS" \
        --replication-factor "$REPLICATION"

}

# ----------------------------------------------------------
# 2. LISTAR TOPICS
# ----------------------------------------------------------

list_topics() {

    kafka_exec kafka-topics \
        --bootstrap-server "$BOOTSTRAP" \
        --list

}

# ----------------------------------------------------------
# 3. DESCRIBIR TOPIC
# ----------------------------------------------------------

describe_topic() {

    TOPIC=$1

    kafka_exec kafka-topics \
        --bootstrap-server "$BOOTSTRAP" \
        --describe \
        --topic "$TOPIC"

}

# ----------------------------------------------------------
# 4. ALTERAR RETENCION
# ----------------------------------------------------------

set_retention() {

    TOPIC=$1
    RETENTION=$2
    RETENTION_BYTES=$3

    if [[ -z "$TOPIC" || -z "$RETENTION" ]]; then
        echo "Uso: $0 retention <topic> <retention.ms> [retention.bytes]"
        exit 1
    fi

    CONFIG="retention.ms=$RETENTION"

    if [[ -n "$RETENTION_BYTES" ]]; then
        CONFIG="$CONFIG,retention.bytes=$RETENTION_BYTES"
    fi

    kafka_exec kafka-configs \
        --bootstrap-server "$BOOTSTRAP" \
        --entity-type topics \
        --entity-name "$TOPIC" \
        --alter \
        --add-config "$CONFIG"

}

# ----------------------------------------------------------
# 5. CONSULTAR CONFIGURACION DE TOPIC
# ----------------------------------------------------------

describe_config() {

    TOPIC=$1

    kafka_exec kafka-configs \
        --bootstrap-server "$BOOTSTRAP" \
        --entity-type topics \
        --entity-name "$TOPIC" \
        --describe

}

# ----------------------------------------------------------
# 6. VERIFICAR BROKERS
# ----------------------------------------------------------

list_brokers() {

    kafka_exec kafka-broker-api-versions \
        --bootstrap-server "$BOOTSTRAP"

}

# ----------------------------------------------------------
# 7. LISTAR CONSUMER GROUPS
# ----------------------------------------------------------

list_groups() {

    kafka_exec kafka-consumer-groups \
        --bootstrap-server "$BOOTSTRAP" \
        --list

}

# ----------------------------------------------------------
# 8. INSPECCIONAR CONSUMER GROUP
# ----------------------------------------------------------

describe_group() {

    GROUP=$1

    kafka_exec kafka-consumer-groups \
        --bootstrap-server "$BOOTSTRAP" \
        --describe \
        --group "$GROUP"

}

# ----------------------------------------------------------
# 9. CONSULTAR OFFSETS DE TOPIC
# ----------------------------------------------------------

get_offsets() {

    TOPIC=$1
    POSITION=${2:-latest}

    case "$POSITION" in
        earliest)
            TIME=-2
            ;;
        latest)
            TIME=-1
            ;;
        *)
            echo "Usa earliest o latest."
            exit 1
            ;;
    esac

    kafka_exec kafka-get-offsets \
        --bootstrap-server "$BOOTSTRAP" \
        --topic "$TOPIC" \
        --time "$TIME"

}

# ----------------------------------------------------------
# 10. MEDIR CONSUMER LAG
# ----------------------------------------------------------

get_lag() {

    GROUP=$1

    if [[ -n "$GROUP" ]]; then

        kafka_exec kafka-consumer-groups \
            --bootstrap-server "$BOOTSTRAP" \
            --describe \
            --group "$GROUP"

    else

        kafka_exec kafka-consumer-groups \
            --bootstrap-server "$BOOTSTRAP" \
            --describe \
            --all-groups

    fi

}

# ----------------------------------------------------------
# ROUTER PRINCIPAL
# ----------------------------------------------------------

case "$1" in

    create)
        create_topic "$2" "$3" "$4"
        ;;

    list)
        list_topics
        ;;

    describe)
        describe_topic "$2"
        ;;

    retention)
        set_retention "$2" "$3" "$4"
        ;;

    config)
        describe_config "$2"
        ;;

    brokers)
        list_brokers
        ;;

    groups)
        list_groups
        ;;

    group)
        describe_group "$2"
        ;;

    offsets)
        get_offsets "$2" "$3"
        ;;

    lag)
        get_lag "$2"
        ;;

    *)
        usage
        ;;

esac

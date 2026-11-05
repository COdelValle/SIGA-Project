package cl.siga.ms_kafka.dto;


public record CreateTopicRequest(
  String nombre,
  Integer particiones,
  Short replicas
) {
}

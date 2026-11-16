package cl.siga.ms_kafka.dto;


public record CreateTopicRequest(
  //Tiene por objetivo Crear peticiones de topic.
  String nombre,
  Integer particiones,
  Short replicas
) {
}


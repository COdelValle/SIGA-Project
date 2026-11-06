package cl.siga.ms_kafka.dto;

import java.util.UUID;

public record TopicDetailResponse(
  //Tiene por objetivo Dar la respuesta del topic con detalles.
  //UUID para referencia del elemento, no para el id administrativo.
  UUID id,
  String nombre,
  Integer particiones,
  Short replicas
){
}

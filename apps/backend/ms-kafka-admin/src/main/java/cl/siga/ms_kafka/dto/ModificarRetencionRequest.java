package cl.siga.ms_kafka.dto;

public record ModificarRetencionRequest(
  Long retentionMs,
  Long retentionBytes
) {
}

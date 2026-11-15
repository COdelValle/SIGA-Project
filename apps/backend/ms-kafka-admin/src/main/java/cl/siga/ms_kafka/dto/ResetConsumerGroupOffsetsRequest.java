
package cl.siga.ms_kafka.dto;

import lombok.Data;
import java.util.List;

@Data
public class ResetConsumerGroupOffsetsRequest {

  private String estrategia;

  private List<PartitionOffset> offsets;

  @Data
  public static class PartitionOffset {

    private String topic;

    private Integer particion;

    private Long offset;
  }
}


package com.cisco.wap.pinot.minion.autoscaler.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "minion-autoscaler")
public class MinionAutoscalerProperties {

  private String controllerUrl;
  private String authToken;
  private int minionMinReplica;
  private int minionMaxReplica;
  private String namespace;
  private String statefulSet;
  private String scaleStatusPollInterval;

}

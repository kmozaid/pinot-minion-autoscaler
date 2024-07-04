package com.cisco.wap.pinot.minion.autoscaler.config;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@Configuration
public class MinionAutoscalerConfig {

  @Bean
  public RestClient restClient(MinionAutoscalerProperties pinotConfig) {
    return RestClient.builder()
        .baseUrl(pinotConfig.getControllerUrl())
        .defaultHeader(HttpHeaders.AUTHORIZATION, pinotConfig.getAuthToken())
        .build();
  }

  @Bean
  public KubernetesClient k8sClient() {
    return new KubernetesClientBuilder().build();
  }

}

package com.cisco.wap.pinot.minion.autoscaler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@EnableScheduling
@EnableConfigurationProperties
@SpringBootApplication
public class MinionAutoscalerApp {

  public static void main(String[] args) {
    SpringApplication.run(MinionAutoscalerApp.class);
  }

}

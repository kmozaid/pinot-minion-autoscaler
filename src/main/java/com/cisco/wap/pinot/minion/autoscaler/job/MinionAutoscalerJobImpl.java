package com.cisco.wap.pinot.minion.autoscaler.job;

import com.cisco.wap.pinot.minion.autoscaler.service.MinionAutoscalerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MinionAutoscalerJobImpl implements MinionAutoscalerJob {

  private final MinionAutoscalerService minionAutoscalerService;

  @Autowired
  public MinionAutoscalerJobImpl(MinionAutoscalerService minionAutoscalerService) {
    this.minionAutoscalerService = minionAutoscalerService;
  }

  @Scheduled(cron = "${minion-autoscaler.cron}")
  public void scale() {
    try {
      log.info("starting job to check and scale");
      minionAutoscalerService.scale();
    } catch (Exception e) {
      log.error("Failed to scale minion", e);
    }
  }
}

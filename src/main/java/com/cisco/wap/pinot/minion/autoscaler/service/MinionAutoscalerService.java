package com.cisco.wap.pinot.minion.autoscaler.service;

public interface MinionAutoscalerService {

  void scale() throws InterruptedException;

}

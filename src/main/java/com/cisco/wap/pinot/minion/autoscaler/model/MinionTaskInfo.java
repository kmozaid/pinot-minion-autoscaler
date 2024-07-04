package com.cisco.wap.pinot.minion.autoscaler.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MinionTaskInfo {
  private final String taskType;
  private final int scheduledTasks;
  private final int concurrentTaskPerInstance;
}

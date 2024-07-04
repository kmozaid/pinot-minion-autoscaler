package com.cisco.wap.pinot.minion.autoscaler.service;

import com.cisco.wap.pinot.minion.autoscaler.client.PinotClient;
import com.cisco.wap.pinot.minion.autoscaler.config.MinionAutoscalerProperties;
import com.cisco.wap.pinot.minion.autoscaler.model.MinionTaskInfo;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.apps.StatefulSetStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.RollableScalableResource;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MinionAutoscalerServiceImpl implements MinionAutoscalerService {

  private static final long SCALE_STATUS_WAIT_DURATION_MINUTES = 30;
  private final MinionAutoscalerProperties pinotConfig;
  private final PinotClient pinotClient;
  private final KubernetesClient k8sClient;

  @Autowired
  public MinionAutoscalerServiceImpl(MinionAutoscalerProperties pinotConfig,
      PinotClient pinotClient,
      KubernetesClient k8sClient) {
    this.pinotConfig = pinotConfig;
    this.pinotClient = pinotClient;
    this.k8sClient = k8sClient;
  }

  @Override
  public void scale() throws InterruptedException {
    List<MinionTaskInfo> minionTaskInfoList = getMinionTaskInfos();

    int minionCountWithoutLimit = minionTaskInfoList.stream()
        .mapToInt(minionTaskInfo -> minionTaskInfo.getScheduledTasks()
            / minionTaskInfo.getConcurrentTaskPerInstance())
        .reduce(0, Integer::sum);

    int idealMinionReplica = Math.max(Math.min(minionCountWithoutLimit,
        pinotConfig.getMinionMaxReplica()), pinotConfig.getMinionMinReplica());

    RollableScalableResource<StatefulSet> rollableMinionStatefulSet = k8sClient.apps()
        .statefulSets()
        .inNamespace(pinotConfig.getNamespace())
        .withName(pinotConfig.getStatefulSet());
    int actualMinionReplica = rollableMinionStatefulSet.get().getSpec().getReplicas();

    log.info("minionCountWithoutLimit {}, idealMinionReplica {}, actualMinionReplica {}",
        minionCountWithoutLimit, idealMinionReplica, actualMinionReplica);

    if (idealMinionReplica > actualMinionReplica) {
      scaleInternal(rollableMinionStatefulSet, idealMinionReplica, actualMinionReplica);
    } else if (idealMinionReplica < actualMinionReplica) {
      if (idealMinionReplica > pinotConfig.getMinionMinReplica()) {
        log.info("Do not scale down to avoid wasting compute resources");
        /* Do not scale down because Pinot does not control which tasks run which
        minion workers, and the scaling logic does not control which minion workers
        to remove - if we scale down minion workers, workers with running tasks may
        be removed, those running tasks will be regenerated and retried. This wastes
        compute resources.*/
      } else if (idealMinionReplica == pinotConfig.getMinionMinReplica()
          && minionCountWithoutLimit == 0) {
        scaleInternal(rollableMinionStatefulSet, idealMinionReplica, actualMinionReplica);
      }
    }
  }

  private List<MinionTaskInfo> getMinionTaskInfos() {
    List<MinionTaskInfo> minionTaskInfoList = pinotClient.getInProgressTaskTypes()
        .stream()
        .map(taskType -> MinionTaskInfo.builder()
            .taskType(taskType)
            .scheduledTasks(pinotClient.getScheduledTasks(taskType))
            .concurrentTaskPerInstance(pinotClient.getNumConcurrentTasksPerInstance(taskType))
            .build())
        .collect(Collectors.toList());
    log.debug("Got minionTaskInfoList {}", minionTaskInfoList);
    return minionTaskInfoList;
  }

  private void scaleInternal(
      final RollableScalableResource<StatefulSet> rollableMinionStatefulSet,
      final int desiredReplica, final int currentReplica) throws InterruptedException {
    String scaleAction = desiredReplica > currentReplica ? "up" : "down";
    log.info("Scaling {} to desired replica {}", scaleAction, desiredReplica);
    rollableMinionStatefulSet.withTimeout(30, TimeUnit.SECONDS).scale(desiredReplica);
    AtomicBoolean scaleStatus = new AtomicBoolean(false);
    try {
      CompletableFuture.runAsync(() -> {
        while (true) {
          StatefulSetStatus status = rollableMinionStatefulSet.get().getStatus();
          int replicas = status.getReplicas();
          int readyReplicas = status.getReadyReplicas() != null ? status.getReadyReplicas() : 0;
          if (readyReplicas == replicas) {
            log.info("Scaled {} to desired replica {}", scaleAction, readyReplicas);
            scaleStatus.set(true);
            break;
          }
          log.info("Waiting for minion pods to be running...");
          try {
            Thread.sleep(Duration.parse(pinotConfig.getScaleStatusPollInterval()).toMillis());
          } catch (InterruptedException e) {
            throw new RuntimeException("Exception while sleeping for next status check", e);
          }
        }
      }).get(SCALE_STATUS_WAIT_DURATION_MINUTES, TimeUnit.MINUTES);
    } catch (ExecutionException | TimeoutException e) {
      // TODO:// send failure notification
      log.error("Scale operation did not complete in time", e);
    }

    if (scaleStatus.get()) {
      // TODO:// send success notification
    }
  }
}

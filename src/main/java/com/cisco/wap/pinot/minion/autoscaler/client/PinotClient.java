package com.cisco.wap.pinot.minion.autoscaler.client;

import com.alibaba.fastjson.JSONObject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PinotClient {

  private final RestClient restClient;

  @Autowired
  public PinotClient(RestClient restClient) {
    this.restClient = restClient;
  }

  public int getScheduledTasks(String taskType) {
    JSONObject response = restClient.get()
        .uri("/tasks/{pinot-task-type}/taskcounts", taskType)
        .retrieve()
        .toEntity(JSONObject.class)
        .getBody();
    if (response != null) {
      int scheduledTasks = 0;
      Set<String> subTasks = response.keySet();
      for (String subTask : subTasks) {
        JSONObject counts = response.getJSONObject(subTask);
        scheduledTasks += counts.getIntValue("running");
        scheduledTasks += counts.getIntValue("waiting");
      }
      return scheduledTasks;
    }
    return 0;
  }

  public List<String> getInProgressTaskTypes() {
    return getTaskTypes().stream()
        .filter(taskType -> {
          String status = restClient.get()
              .uri("/tasks/{taskType}/state", taskType)
              .retrieve()
              .toEntity(String.class)
              .getBody();
          return "\"IN_PROGRESS\"".equalsIgnoreCase(status);
        })
        .collect(Collectors.toList());
  }

  public List<String> getTaskTypes() {
    return restClient.get()
        .uri("/tasks/tasktypes")
        .retrieve()
        .toEntity(new ParameterizedTypeReference<List<String>>() {})
        .getBody();
  }

  public int getNumConcurrentTasksPerInstance(String taskType) {
    JSONObject response = restClient.get()
        .uri("/cluster/configs")
        .retrieve()
        .toEntity(JSONObject.class)
        .getBody();
    String configKey = taskType + ".numConcurrentTasksPerInstance";
    if (response != null && response.containsKey(configKey)) {
      return response.getIntValue(configKey);
    }
    return 1;
  }

}

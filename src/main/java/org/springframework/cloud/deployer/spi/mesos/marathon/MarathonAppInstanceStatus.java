package org.springframework.cloud.deployer.spi.mesos.marathon;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.util.StringUtils;

import mesosphere.marathon.client.model.v2.App;
import mesosphere.marathon.client.model.v2.Task;

/**
 * Adapts from the Marathon task API to AppInstanceStatus. An instance of this class
 * can also represent a missing application instance.
 *
 * @author Eric Bottard
 */
public class MarathonAppInstanceStatus implements AppInstanceStatus {

	private final App app;

	private final Task task;

	private MarathonAppInstanceStatus(App app, Task task) {
		this.app = app;
		this.task = task;
	}

	/**
	 * Construct a status from a running app task.
	 */
	static MarathonAppInstanceStatus up(App app, Task task) {
		return new MarathonAppInstanceStatus(app, task);
	}

	/**
	 * Construct a status from a missing app task (maybe it crashed, maybe Mesos could not offer enough resources, etc.)
	 */
	static MarathonAppInstanceStatus down(App app) {
		return new MarathonAppInstanceStatus(app, null);
	}


	@Override
	public String getId() {
		return task != null ? task.getId() : (app.getId() + "-failed-" + new Random().nextInt());
	}

	@Override
	public DeploymentState getState() {
		if (task == null) {
			return DeploymentState.unknown;
		}
		else {
			if (app.getInstances().intValue() > app.getTasksRunning().intValue()) {
				return DeploymentState.deploying;
			}
			else {
				return DeploymentState.deployed;
			}
		}
	}

	@Override
	public Map<String, String> getAttributes() {
		HashMap<String, String> result = new HashMap<>();
		if (task != null) {
			result.put("staged_at", task.getStagedAt());
			result.put("started_at", task.getStartedAt());
			result.put("host", task.getHost());
			result.put("ports", StringUtils.collectionToCommaDelimitedString(task.getPorts()));
		}
		return result;
	}
}

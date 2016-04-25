/*
 * Copyright 2015-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.deployer.spi.mesos.marathon;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.util.StringUtils;

import mesosphere.marathon.client.model.v2.App;
import mesosphere.marathon.client.model.v2.HealthCheckResult;
import mesosphere.marathon.client.model.v2.Task;

/**
 * Adapts from the Marathon task API to AppInstanceStatus. An instance of this class
 * can also represent a missing application instance.
 *
 * @author Eric Bottard
 * @author Thomas Risberg
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
			if (app.getLastTaskFailure() == null) {
				return DeploymentState.unknown;
			}
			else {
				return DeploymentState.failed;
			}
		}
		else {
			if (app.getInstances().intValue() > app.getTasksRunning().intValue()) {
				return DeploymentState.deploying;
			}
			else {
				Collection<HealthCheckResult> healthCheckResults = task.getHealthCheckResults();
				boolean alive = healthCheckResults != null && healthCheckResults.iterator().next().isAlive();
				if (!alive && app.getLastTaskFailure() != null) {
					return DeploymentState.failed;
				}
				return alive ? DeploymentState.deployed : DeploymentState.deploying;
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

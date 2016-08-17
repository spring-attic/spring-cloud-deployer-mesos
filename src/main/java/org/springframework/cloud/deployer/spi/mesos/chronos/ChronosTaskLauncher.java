/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.cloud.deployer.spi.mesos.chronos;

import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskStatus;
import org.springframework.cloud.mesos.chronos.client.Chronos;

/**
 * A task launcher that targets Mesos.
 *
 * @author Thomas Risberg
 */
public class ChronosTaskLauncher implements TaskLauncher {

	private ChronosTaskLauncherProperties properties = new ChronosTaskLauncherProperties();

	private Chronos chronos;

	public ChronosTaskLauncher(ChronosTaskLauncherProperties properties, Chronos chronos) {
		this.properties = properties;
		this.chronos = chronos;
	}

	@Override
	public String launch(AppDeploymentRequest request) {
		return null;
	}

	@Override
	public void cancel(String id) {

	}

	@Override
	public TaskStatus status(String id) {
		return null;
	}
}
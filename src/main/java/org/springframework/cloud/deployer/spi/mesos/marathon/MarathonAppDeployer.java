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


import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;

import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.model.v2.App;
import mesosphere.marathon.client.model.v2.Container;
import mesosphere.marathon.client.model.v2.Docker;
import mesosphere.marathon.client.model.v2.HealthCheck;
import mesosphere.marathon.client.model.v2.Port;
import mesosphere.marathon.client.model.v2.Task;
import mesosphere.marathon.client.utils.MarathonException;

/**
 * A deployer implementation for deploying apps on Marathon, using the
 * ModuleLauncher Docker image.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 * @author Thomas Risberg
 */
public class MarathonAppDeployer implements AppDeployer {

	private static final Logger logger = LoggerFactory.getLogger(MarathonAppDeployer.class);

	private MarathonProperties marathonProperties = new MarathonProperties();

	Marathon marathon;

	@Autowired
	public MarathonAppDeployer(MarathonProperties marathonProperties,
	                           Marathon marathon) {
		this.marathonProperties = marathonProperties;
		this.marathon = marathon;
	}

	@Override
	public String deploy(AppDeploymentRequest request) {

		logger.info("Deploying app: {}", request.getDefinition().getName());

		String groupId = request.getEnvironmentProperties().get(GROUP_PROPERTY_KEY);
		if (groupId == null) {
			groupId = "default";
		}
		App app = new App();

		AppStatus status = status(deduceAppId(request));
		if (!status.getState().equals(DeploymentState.unknown)) {
			throw new IllegalStateException(
					String.format("App '%s' is already deployed", request.getDefinition().getName()));
		}

		Container container = new Container();
		Docker docker = new Docker();
		String image = null;
		//TODO: what's the proper format for a Docker URI?
		try {
			image = request.getResource().getURI().getSchemeSpecificPart();
		} catch (IOException e) {
			throw new IllegalArgumentException("Unable to get URI for " + request.getResource(), e);
		}
		logger.info("Using Docker image: " + image);
		docker.setImage(image);
		Port port = new Port(8080);
		port.setHostPort(0);
		docker.setPortMappings(Arrays.asList(port));
		docker.setNetwork("BRIDGE");
		container.setDocker(docker);
		app.setContainer(container);

		app.setId(deduceAppId(request));

		Map<String, String> env = new HashMap<>();
		env.putAll(request.getDefinition().getProperties());
		env.putAll(request.getEnvironmentProperties());

		// Pass API Endpoint under this environment variable for discovery by marathon cloud connector
		// TODO: Service discovery?????
		//env.put("SPRING_CLOUD_MARATHON_HOST", marathonProperties.getApiEndpoint());

		app.setEnv(env);

		Double cpus = deduceCpus(request);
		Double memory = deduceMemory(request);

		app.setCpus(cpus);
		app.setMem(memory);
		app.setInstances(Integer.getInteger(request.getDefinition().getProperties().get(COUNT_PROPERTY_KEY)));

		HealthCheck healthCheck = new HealthCheck();
		healthCheck.setPath("/health");
		healthCheck.setGracePeriodSeconds(60 * 10);
		app.setHealthChecks(Arrays.asList(healthCheck));

		logger.debug("Creating app with definition: " + app.toString());
		try {
			marathon.createApp(app);
		}
		catch (MarathonException e) {
			throw new RuntimeException(e);
		}
		return app.getId();
	}

	@Override
	public void undeploy(String id) {
		logger.info("Undeploying module: {}", id);
		try {
			marathon.deleteApp(id);
		}
		catch (MarathonException e) {
			throw new RuntimeException(e);
		}

	}

	@Override
	public AppStatus status(String id) {
		AppStatus status;
		try {
			App app = marathon.getApp(id).getApp();
			status = buildStatus(id, app);
		}
		catch (MarathonException e) {
			if (e.getMessage().contains("Not Found")) {
				status = AppStatus.of(id).build();
			}
			else {
				throw new RuntimeException(e);
			}
		}
		logger.debug("Status for app: {} is {}", id, status);
		return status;
	}

	private String deduceAppId(AppDeploymentRequest request) {
		String groupId = request.getEnvironmentProperties().get(GROUP_PROPERTY_KEY);
		String name = request.getDefinition().getName();
		if (groupId != null) {
			return groupId + "-" + name;
		}
		else {
			return name;
		}
	}

	private Double deduceMemory(AppDeploymentRequest request) {
		String override = request.getEnvironmentProperties().get("spring.cloud.deployer.marathon.memory");
		return override != null ? Double.valueOf(override) : marathonProperties.getMemory();
	}

	private Double deduceCpus(AppDeploymentRequest request) {
		String override = request.getEnvironmentProperties().get("spring.cloud.deployer.marathon.cpu");
		return override != null ? Double.valueOf(override) : marathonProperties.getCpu();
	}

	private AppStatus buildStatus(String id, App app) {
		logger.debug("App " + id + " has " + app.getTasksRunning() + "/" + app.getInstances() + " tasks running");
		AppStatus.Builder result = AppStatus.of(id);
		int requestedInstances = app.getInstances();
		int actualInstances = 0;
		if (app.getTasks() != null) {
			for (Task task : app.getTasks()) {
				result.with(MarathonAppInstanceStatus.up(app, task));
				actualInstances++;
			}
		}
		for (int i = actualInstances; i < requestedInstances; i++) {
			result.with(MarathonAppInstanceStatus.down(app));
		}
		return result.build();
	}

}

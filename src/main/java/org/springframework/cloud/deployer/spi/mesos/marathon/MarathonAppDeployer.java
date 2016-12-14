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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.model.v2.App;
import mesosphere.marathon.client.model.v2.Container;
import mesosphere.marathon.client.model.v2.Docker;
import mesosphere.marathon.client.model.v2.Group;
import mesosphere.marathon.client.model.v2.HealthCheck;
import mesosphere.marathon.client.model.v2.Port;
import mesosphere.marathon.client.model.v2.Task;
import mesosphere.marathon.client.utils.MarathonException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppInstanceStatus;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A deployer implementation for deploying apps on Marathon, using the
 * ModuleLauncher Docker image.
 *
 * @author Eric Bottard
 * @author Ilayaperumal Gopinathan
 * @author Thomas Risberg
 */
public class MarathonAppDeployer implements AppDeployer {

	private static final Log logger = LogFactory.getLog(MarathonAppDeployer.class);

	private MarathonAppDeployerProperties properties = new MarathonAppDeployerProperties();

	Marathon marathon;

	@Autowired
	public MarathonAppDeployer(MarathonAppDeployerProperties properties,
	                           Marathon marathon) {
		this.properties = properties;
		this.marathon = marathon;
	}

	@Override
	public String deploy(AppDeploymentRequest request) {

		logger.info(String.format("Deploying app: %s", request.getDefinition().getName()));

		String appId = deduceAppId(request);

		boolean indexed = Boolean.valueOf(request.getDeploymentProperties().get(INDEXED_PROPERTY_KEY));

		if (indexed) {
			try {
				Group group = marathon.getGroup(appId);
				throw new IllegalStateException(
						String.format("App '%s' is already deployed", request.getDefinition().getName()));
			} catch (MarathonException ignore) {}
			Container container = createContainer(request);
			String countProperty = request.getDeploymentProperties().get(COUNT_PROPERTY_KEY);
			int count = (countProperty != null) ? Integer.parseInt(countProperty) : 1;
			for (int i = 0; i < count; i++) {
				String instanceId = appId + "/" + request.getDefinition().getName() + "-" + i;
				createAppDeployment(request, instanceId, container, Integer.valueOf(i));
			}
		}
		else {
			AppStatus status = status(appId);
			if (!status.getState().equals(DeploymentState.unknown)) {
				throw new IllegalStateException(
						String.format("App '%s' is already deployed", request.getDefinition().getName()));
			}
			Container container = createContainer(request);
			createAppDeployment(request, appId, container, null);
		}

		return appId;
	}

	private void createAppDeployment(AppDeploymentRequest request, String deploymentId, Container container, Integer index) {
		App app = new App();
		app.setContainer(container);
		app.setId(deploymentId);

		Map<String, String> env = new HashMap<>();
		env.putAll(request.getDefinition().getProperties());
		for (String envVar : properties.getEnvironmentVariables()) {
			String[] strings = envVar.split("=", 2);
			Assert.isTrue(strings.length == 2, "Invalid environment variable declared: " + envVar);
			env.put(strings[0], strings[1]);
		}
		if (index != null) {
			env.put(INSTANCE_INDEX_PROPERTY_KEY, index.toString());
		}
		app.setEnv(env);

		Collection<String> uris = deduceUris(request);
		app.setUris(uris);

		Double cpus = deduceCpus(request);
		Double memory = deduceMemory(request);
		Integer instances = index == null ? deduceInstances(request) : 1;

		app.setCpus(cpus);
		app.setMem(memory);
		app.setInstances(instances);

		HealthCheck healthCheck = new HealthCheck();
		healthCheck.setPath("/health");
		healthCheck.setGracePeriodSeconds(300);
		app.setHealthChecks(Arrays.asList(healthCheck));

		logger.debug("Creating app with definition:\n" + app.toString());
		try {
			marathon.createApp(app);
		}
		catch (MarathonException e) {
			throw new RuntimeException(e);
		}
	}

	private Container createContainer(AppDeploymentRequest request) {
		Container container = new Container();
		Docker docker = new Docker();
		String image = null;
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
		return container;
	}

	@Override
	public void undeploy(String id) {
		logger.info(String.format("Undeploying app: %s", id));
		Group group = null;
		try {
			group = marathon.getGroup(id);
		} catch (MarathonException ignore) {}
		if (group != null) {
			logger.info(String.format("Undeploying application deployments for group: %s", group.getId()));
			try {
				if (group.getGroups().size() > 0) {
					for (Group g : group.getGroups()) {
						deleteAppsForGroupDeployment(g.getId());
					}
				}
				else {
					deleteAppsForGroupDeployment(group.getId());
				}
			} catch (MarathonException e) {
				throw new RuntimeException(e);
			}
		}
		else {
			logger.info(String.format("Undeploying application deployment: %s", id));
			try {
				App app = marathon.getApp(id).getApp();
				logger.debug(String.format("Deleting application: %s", app.getId()));
				marathon.deleteApp(id);
				deleteTopLevelGroupForDeployment(id);
			} catch (MarathonException e) {
				if (e.getMessage().contains("Not Found")) {
					logger.debug(String.format("Caught: %s", e.getMessage()));
					try {
						deleteAppsForGroupDeployment(id);
					} catch (MarathonException e2) {
						throw new RuntimeException(e2);
					}
				}
				else {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private void deleteAppsForGroupDeployment(String groupId) throws MarathonException {
		Group group = marathon.getGroup(groupId);
		for (App app : group.getApps()) {
			logger.debug(String.format("Deleting application %s in group %s", app.getId(), groupId));
			marathon.deleteApp(app.getId());
		}
		group = marathon.getGroup(groupId);
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Group %s has %d applications and %d groups", group.getId(),
					group.getApps().size(), group.getGroups().size()));
		}
		if (group.getApps().size() == 0 && group.getGroups().size() == 0) {
			logger.info(String.format("Deleting group: %s", groupId));
			marathon.deleteGroup(groupId);
		}
		deleteTopLevelGroupForDeployment(groupId);
	}

	private void deleteTopLevelGroupForDeployment(String id) throws MarathonException {
		String topLevelGroupId = extractGroupId(id);
		if (topLevelGroupId != null) {
			Group topGroup = marathon.getGroup(topLevelGroupId);
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Top level group %s has %d applications and %d groups", topGroup.getId(),
						topGroup.getApps().size(), topGroup.getGroups().size()));
			}
			if (topGroup.getApps().size() == 0 && topGroup.getGroups().size() == 0) {
				logger.info(String.format("Deleting group: %s", topLevelGroupId));
				marathon.deleteGroup(topLevelGroupId);
			}
		}
	}

	@Override
	public AppStatus status(String id) {
		AppStatus status;
		try {
			App app = marathon.getApp(id).getApp();
			logger.debug(String.format("Building status for app: %s", id));
			status = buildAppStatus(id, app);
		} catch (MarathonException e) {
			if (e.getMessage().contains("Not Found")) {
				try {
					Group group = marathon.getGroup(id);
					logger.debug(String.format("Building status for group: %s", id));
					AppStatus.Builder result = AppStatus.of(id);
					for (App app : group.getApps()) {
						result.with(buildInstanceStatus(app.getId()));
					}
					status = result.build();
				} catch (MarathonException e1) {
					status = AppStatus.of(id).build();
				}
			}
			else {
				status = AppStatus.of(id).build();
			}
		}
		logger.debug(String.format("Status for app: %s is %s", id, status));
		return status;
	}

	private String deduceAppId(AppDeploymentRequest request) {
		String groupId = request.getDeploymentProperties().get(GROUP_PROPERTY_KEY);
		String name = request.getDefinition().getName();
		if (groupId != null) {
			return "/" + groupId + "/" + name;
		}
		else {
			return "/" + name;
		}
	}

	private String extractGroupId(String appId) {
		int index = appId.lastIndexOf('/');
		String groupId = null;
		if (index > 0) {
			groupId = appId.substring(0, index);
		}
		return groupId;
	}

	private Collection<String> deduceUris(AppDeploymentRequest request) {
		Set<String> additional = StringUtils.commaDelimitedListToSet(request.getDeploymentProperties().get("spring.cloud.deployer.mesos.marathon.uris"));
		HashSet<String> result = new HashSet<>(additional);
		result.addAll(properties.getUris());
		return result;
	}

	private Double deduceMemory(AppDeploymentRequest request) {
		String override = request.getDeploymentProperties().get(AppDeployer.MEMORY_PROPERTY_KEY);
		return override != null ? Double.valueOf(override) : properties.getMemory();
	}

	private Double deduceCpus(AppDeploymentRequest request) {
		String override = request.getDeploymentProperties().get(AppDeployer.CPU_PROPERTY_KEY);
		return override != null ? Double.valueOf(override) : properties.getCpu();
	}

	private Integer deduceInstances(AppDeploymentRequest request) {
		String value = request.getDeploymentProperties().get(COUNT_PROPERTY_KEY);
		return value != null ? Integer.valueOf(value) : Integer.valueOf("1");
	}

	private AppInstanceStatus buildInstanceStatus(String id) throws MarathonException {
		App appInstance = marathon.getApp(id).getApp();
		logger.debug("Deployment " + id + " has " + appInstance.getTasksRunning() + "/" + appInstance.getInstances() + " tasks running");
		if (appInstance.getTasks() != null) {
			// there should only be one task for this type of deployment
			MarathonAppInstanceStatus status = null;
			for (Task task : appInstance.getTasks()) {
				if (status == null) {
					status = MarathonAppInstanceStatus.up(appInstance, task);
				}
			}
			if (status == null) {
				status = MarathonAppInstanceStatus.down(appInstance);
			}
			return status;
		}
		else {
			return MarathonAppInstanceStatus.down(appInstance);
		}
	}

	private AppStatus buildAppStatus(String id, App app) {
		logger.debug("Deployment " + id + " has " + app.getTasksRunning() + "/" + app.getInstances() + " tasks running");
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

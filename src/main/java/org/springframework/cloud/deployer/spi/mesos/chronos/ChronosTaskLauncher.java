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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hashids.Hashids;

import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.mesos.constraints.Constraint;
import org.springframework.cloud.deployer.spi.task.LaunchState;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskStatus;
import org.springframework.cloud.mesos.chronos.client.Chronos;
import org.springframework.cloud.mesos.chronos.client.ChronosException;
import org.springframework.cloud.mesos.chronos.client.model.DockerContainer;
import org.springframework.cloud.mesos.chronos.client.model.DockerJob;
import org.springframework.cloud.mesos.chronos.client.model.Job;
import org.springframework.util.StringUtils;

/**
 * A task launcher that targets Mesos Chronos.
 *
 * @author Thomas Risberg
 */
public class ChronosTaskLauncher implements TaskLauncher {

	private static final Log logger = LogFactory.getLog(ChronosTaskLauncher.class);

	private ChronosTaskLauncherProperties properties = new ChronosTaskLauncherProperties();

	private Chronos chronos;

	public ChronosTaskLauncher(ChronosTaskLauncherProperties properties, Chronos chronos) {
		this.properties = properties;
		this.chronos = chronos;
	}

	@Override
	public String launch(AppDeploymentRequest request) {
		String jobName = createDeploymentId(request);
		String image = null;
		try {
			image = request.getResource().getURI().getSchemeSpecificPart();
		} catch (IOException e) {
			throw new IllegalArgumentException("Unable to get URI for " + request.getResource(), e);
		}
		logger.info("Using Docker image: " + image);
		DockerJob job = new DockerJob();
		job.setName(jobName);
		List<Map<String, String>> envVars = new ArrayList<>();
		Map<String, String> springApplicationJson = createSpringApplicationJson(request);
		if (springApplicationJson.size() > 0) {
			envVars.add(springApplicationJson);
		}
		logger.info("Using env: " + envVars);
		if (envVars.size() > 0) {
			job.setEnvironmentVariables(envVars);
		}
		job.setShell(false);
		job.setCommand("");
		List<String> args = createCommandArgs(request);
		if (args.size() > 0) {
			job.setArguments(args);
		}
		job.setSchedule("R1//P");
		job.setRetries(1);
		DockerContainer container = new DockerContainer();
		container.setImage(image);
		job.setContainer(container);
		Double cpus = deduceCpus(request);
		Double memory = deduceMemory(request);
		Collection<Constraint> constraints = deduceConstraints(request);
		job.setCpus(cpus);
		job.setMem(memory);
		job.setConstraints(constraints.stream().map(Constraint::toStringList).collect(Collectors.toList()));
		if (StringUtils.hasText(properties.getOwnerEmail())) {
			job.setOwner(properties.getOwnerEmail());
		}
		if (StringUtils.hasText(properties.getOwnerName())) {
			job.setOwnerName(properties.getOwnerName());
		}
		if (properties.getUris() != null && properties.getUris().length > 0) {
			job.setUris(Arrays.asList(properties.getUris()));
		}
		try {
			if (logger.isDebugEnabled()) {
				logger.debug("Launching Job with definition:\n" + job.toString());
			}
			chronos.createDockerJob(job);
		} catch (ChronosException e) {
			logger.error(e.getMessage(), e);
			throw new IllegalStateException(String.format("Error while creating job '%s'", jobName), e);
		}
		return jobName;
	}

	@Override
	public void cancel(String id) {
		try {
			chronos.deleteJobTasks(id);
		} catch (ChronosException e) {
			throw new IllegalStateException(String.format("Error while canceling job '%s'", id), e);
		}
	}

	@Override
	public TaskStatus status(String id) {
		String csv = null;
		try {
			csv = chronos.getGraphCsv();
		} catch (ChronosException e) {
			throw new IllegalStateException(String.format("Error while retrieving graph"), e);
		}
		List<Job> list = null;
		try {
			list = chronos.getJobs();
		} catch (ChronosException e) {
			throw new IllegalStateException(String.format("Error while retrieving jobs"), e);
		}
		Job job = null;
		for (Job j : list) {
			if (j.getName().equals(id)) {
				job = j;
				break;
			}
		}
		TaskStatus status = buildTaskStatus(properties, id, job, csv);
		logger.debug(String.format("Status for task: %s is %s", id, status));

		return status;
	}

	@Override
	public void cleanup(String id) {
		try {
			chronos.deleteJob(id);
		} catch (ChronosException e) {
			throw new IllegalStateException(String.format("Error while deleting job '%s'", id), e);
		}
	}

	@Override
	public void destroy(String taskName) {
	}

	protected String createDeploymentId(AppDeploymentRequest request) {
		String name = request.getDefinition().getName();
		Hashids hashids = new Hashids(name);
		String hashid = hashids.encode(System.currentTimeMillis());
		return name + "-" + hashid;
	}

	protected Map<String, String> createSpringApplicationJson(AppDeploymentRequest request) {
		String value = "{}";
		try {
			value = new ObjectMapper().writeValueAsString(
					Optional.ofNullable(request.getDefinition().getProperties())
							.orElse(Collections.emptyMap()));
		} catch (JsonProcessingException e) {}
		Map<String, String> springApp = new HashMap<>();
		if (!"{}".equals(value)) {
			springApp.put("name", "SPRING_APPLICATION_JSON");
			springApp.put("value", value);
		}
		return springApp;
	}

	protected List<String> createCommandArgs(AppDeploymentRequest request) {
		List<String> cmdArgs = new LinkedList<String>();
		// add provided command line args
		cmdArgs.addAll(request.getCommandlineArguments());
		logger.debug("Using command args: " + cmdArgs);
		return cmdArgs;
	}

	protected TaskStatus buildTaskStatus(ChronosTaskLauncherProperties properties, String id, Job job, String csv) {
		if (job == null) {
			return new TaskStatus(id, LaunchState.unknown, new HashMap<>());
		}
		String last = null;
		String state= null;
		if (StringUtils.hasText(csv)) {
			List<String> csvLines = Arrays.asList(csv.split("\\r?\\n"));
			for (String line : csvLines) {
				if (line.startsWith("node")) {
					List<String> values = Arrays.asList(line.split("\\s*,\\s*"));
					if (values.size() >= 4) {
						if (id.equals(values.get(1))) {
							last = values.get(2);
							state = values.get(3);
							break;
						}
					}
				}
			}
		}
		if ("running".equals(state)) {
			return new TaskStatus(id, LaunchState.running, new HashMap<>());
		}
		if ("queued".equals(state)) {
			return new TaskStatus(id, LaunchState.launching, new HashMap<>());
		}
		if ("success".equals(last)) {
			return new TaskStatus(id, LaunchState.complete, new HashMap<>());
		}
		else {
			// TODO: state == idle could indicate cancelled?
			return new TaskStatus(id, LaunchState.failed, new HashMap<>());
		}
	}

	private Double deduceMemory(AppDeploymentRequest request) {
		String override = request.getDeploymentProperties().get(AppDeployer.MEMORY_PROPERTY_KEY);
		return override != null ? Double.valueOf(override) : properties.getMemory();
	}

	private Double deduceCpus(AppDeploymentRequest request) {
		String override = request.getDeploymentProperties().get(AppDeployer.CPU_PROPERTY_KEY);
		return override != null ? Double.valueOf(override) : properties.getCpu();
	}

	private Collection<Constraint> deduceConstraints(AppDeploymentRequest request) {
		Set<Constraint> requestSpecific = StringUtils.commaDelimitedListToSet(request.getDeploymentProperties().get(prefix("constraints")))
			.stream().map(Constraint::new).collect(Collectors.toSet());
		Set<Constraint> result = new HashSet<>(properties.getConstraints());
		result.addAll(requestSpecific);
		return result;
	}

	private String prefix(String property) {
		return ChronosTaskLauncherProperties.PREFIX + "." + property;
	}
}

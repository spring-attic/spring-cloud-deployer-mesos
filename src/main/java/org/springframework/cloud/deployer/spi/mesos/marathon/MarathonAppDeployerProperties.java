/*
 * Copyright 2015-16 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.deployer.spi.mesos.marathon;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.deployer.spi.mesos.constraints.Constraint;

/**
 * Configuration properties for connecting to a Marathon installation.
 *
 * @author Eric Bottard
 * @author Thomas Risberg
 */
@ConfigurationProperties(MarathonAppDeployerProperties.PREFIX)
public class MarathonAppDeployerProperties {

	/*default*/ final static String PREFIX = "spring.cloud.deployer.mesos.marathon";

	/**
	 * The location of the Marathon REST endpoint.
	 */
	private String apiEndpoint = "http://m1.dcos/service/marathon";

	/**
	 * Secrets for a access a private registry to pull images.
	 */
	private String imagePullSecret;

	/**
	 * How much memory to allocate per module, can be overridden at deployment time.
	 */
	private double memory = 512.0D;

	/**
	 * How many CPUs to allocate per module, can be overridden at deployment time.
	 */
	private double cpu = 0.5D;

	/**
	 * Environment variables to set for any deployed app container.
	 */
	private String[] environmentVariables = new String[]{};

	/**
	 * A set of constraints to apply to any deployed app, as a comma separated set of (field operator param?) triplets.
	 */
	private Set<Constraint> constraints = new HashSet<>();

	/**
	 * URIs to set for any deployed app container (marathon will fetch content at that address and make it available
	 * to the container).
	 */
	private List<String> uris = new ArrayList<>(0);

	public double getMemory() {
		return memory;
	}

	public void setMemory(double memory) {
		this.memory = memory;
	}

	public double getCpu() {
		return cpu;
	}

	public void setCpu(double cpu) {
		this.cpu = cpu;
	}

	public String getApiEndpoint() {
		return apiEndpoint;
	}

	public void setApiEndpoint(String apiEndpoint) {
		this.apiEndpoint = apiEndpoint;
	}

	public String getImagePullSecret() {
		return imagePullSecret;
	}

	public void setImagePullSecret(String imagePullSecret) {
		this.imagePullSecret = imagePullSecret;
	}

	public String[] getEnvironmentVariables() {
		return environmentVariables;
	}

	public void setEnvironmentVariables(String[] environmentVariables) {
		this.environmentVariables = environmentVariables;
	}

	public List<String> getUris() {
		return uris;
	}

	public void setUris(List<String> uris) {
		this.uris = uris;
	}

	public Set<Constraint> getConstraints() {
		return constraints;
	}

	public void setConstraints(Set<Constraint> constraints) {
		this.constraints = constraints;
	}
}

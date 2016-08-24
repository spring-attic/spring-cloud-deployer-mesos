/*
 * Copyright 2015-16 the original author or authors.
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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for interacting with a Chronos service.
 *
 * @author Thomas Risberg
 */
@ConfigurationProperties("spring.cloud.deployer.mesos.chronos")
public class ChronosTaskLauncherProperties {

	/**
	 * The location of the Chronos REST endpoint.
	 */
	private String apiEndpoint = "http://m1.dcos/service/chronos";

	/**
	 * URIs for artifacts to be downloaded when the task is started.
	 */
	private String[] uris;

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
	 * Email address for task owner.
	 */
	private String ownerEmail;

	/**
	 * Name of task owner.
	 */
	private String ownerName;

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

	public String[] getUris() {
		return uris;
	}

	public void setUris(String[] uris) {
		this.uris = uris;
	}

	public String[] getEnvironmentVariables() {
		return environmentVariables;
	}

	public void setEnvironmentVariables(String[] environmentVariables) {
		this.environmentVariables = environmentVariables;
	}

	public String getOwnerEmail() {
		return ownerEmail;
	}

	public void setOwnerEmail(String ownerEmail) {
		this.ownerEmail = ownerEmail;
	}

	public String getOwnerName() {
		return ownerName;
	}

	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}
}

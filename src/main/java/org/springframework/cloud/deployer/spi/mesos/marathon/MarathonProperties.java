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

package org.springframework.cloud.deployer.spi.mesos.marathon;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for connecting to a Marathon installation.
 *
 * @author Eric Bottard
 * @author Thomas Risberg
 */
@ConfigurationProperties("spring.cloud.deployer.marathon")
public class MarathonProperties {

	/**
	 * The location of the Marathon REST endpoint.
	 */
	private String apiEndpoint = "http://10.141.141.10:8080";

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
//	private double cpu = 0.5D;
	private double cpu = 1.0D;

	/**
	 * The set of dependencies to add at runtime to the deployed module launcher.
	 * Typically includes the spring cloud marathon-connector library.
	 */
	private String includes;

	public String getIncludes() {
		return includes;
	}

	public void setIncludes(String includes) {
		this.includes = includes;
	}

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
}

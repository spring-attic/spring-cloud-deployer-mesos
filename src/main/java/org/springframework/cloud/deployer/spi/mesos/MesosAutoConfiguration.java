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

package org.springframework.cloud.deployer.spi.mesos;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.mesos.chronos.ChronosTaskLauncherProperties;
import org.springframework.cloud.deployer.spi.mesos.marathon.MarathonAppDeployer;
import org.springframework.cloud.deployer.spi.mesos.marathon.MarathonAppDeployerProperties;
import org.springframework.cloud.deployer.spi.mesos.chronos.ChronosTaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.mesos.chronos.client.Chronos;
import org.springframework.cloud.mesos.chronos.client.ChronosClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonClient;

/**
 * Spring Bean configuration for Mesos {@link MarathonAppDeployer} and {@link ChronosTaskLauncher}.
 *
 * @author Florian Rosenberg
 * @author Thomas Risberg
 */
@Configuration
@EnableConfigurationProperties({MarathonAppDeployerProperties.class, ChronosTaskLauncherProperties.class})
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class MesosAutoConfiguration {
	
	@Autowired
	private MarathonAppDeployerProperties marathonProperties;

	@Autowired
	private ChronosTaskLauncherProperties chronosProperties;

	@Bean
	public Marathon marathon() {
		Marathon marathon = MarathonClient.getInstance(marathonProperties.getApiEndpoint());
		return marathon;
	}

	@Bean
	public AppDeployer appDeployer(Marathon marathon) {
		return new MarathonAppDeployer(marathonProperties, marathon);
	}

	@Bean
	public Chronos chronos() {
		Chronos chronos = ChronosClient.getInstance(chronosProperties.getApiEndpoint());
		return chronos;
	}

	@Bean
	public TaskLauncher taskDeployer(Chronos chronos) {
		return new ChronosTaskLauncher(chronosProperties, chronos);
	}


}

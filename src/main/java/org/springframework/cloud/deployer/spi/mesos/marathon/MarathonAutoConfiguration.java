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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonClient;

/**
 * Spring Bean configuration for the {@link MarathonAppDeployer}.
 *
 * @author Florian Rosenberg
 * @author Thomas Risberg
 */
@Configuration
@EnableConfigurationProperties(MarathonProperties.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class MarathonAutoConfiguration {
	
	@Autowired
	private MarathonProperties properties;

	@Bean
	public Marathon marathon() {
		Marathon marathon = MarathonClient.getInstance(properties.getApiEndpoint());
		return marathon;
	}

	@Bean
	public AppDeployer appDeployer(Marathon marathon) {
		return new MarathonAppDeployer(properties, marathon);
	}

	@Bean
	public TaskLauncher taskDeployer(Marathon marathon) {
		// Not yet implemented
		return null;
	}


}

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

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.deployer.spi.mesos.marathon.MarathonAppDeployerProperties;
import org.springframework.cloud.mesos.chronos.client.Chronos;
import org.springframework.cloud.mesos.chronos.client.ChronosClient;
import org.springframework.cloud.stream.test.junit.AbstractExternalResourceTestSupport;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonClient;

/**
 * JUnit {@link org.junit.Rule} that detects the fact that a Chronos installation is available.
 *
 * @author Thomas Risberg
 */
public class ChronosTestSupport extends AbstractExternalResourceTestSupport<Chronos> {

	private ConfigurableApplicationContext context;

	protected ChronosTestSupport() {
		super("CHRONOS");
	}


	@Override
	protected void cleanupResource() throws Exception {
		context.close();
	}

	@Override
	protected void obtainResource() throws Exception {
		context = SpringApplication.run(Config.class);
		resource = context.getBean(Chronos.class);
		resource.getGraphCsv();
	}

	@Configuration
	@EnableAutoConfiguration
	@EnableConfigurationProperties(ChronosTaskLauncherProperties.class)
	public static class Config {

		@Bean
		public Chronos chronos(ChronosTaskLauncherProperties properties) {
			Chronos chronos = ChronosClient.getInstance(properties.getApiEndpoint());
			return chronos;
		}
	}
}

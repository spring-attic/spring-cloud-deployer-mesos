/*
 * Copyright 2016 the original author or authors.
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

import mesosphere.marathon.client.Marathon;
import mesosphere.marathon.client.MarathonClient;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.deployer.spi.mesos.dcos.DcosClusterProperties;
import org.springframework.cloud.deployer.spi.test.junit.AbstractExternalResourceTestSupport;
import org.springframework.cloud.mesos.dcos.client.DcosHeadersInterceptor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * JUnit {@link org.junit.Rule} that detects the fact that a Marathon installation is available.
 *
 * @author Thomas Risberg
 * @author Eric Bottard
 */
public class MarathonTestSupport extends AbstractExternalResourceTestSupport<Marathon> {

	private ConfigurableApplicationContext context;

	protected MarathonTestSupport() {
		super("MARATHON");
	}

	@Override
	protected void cleanupResource() throws Exception {
		context.close();
	}

	@Override
	protected void obtainResource() throws Exception {
		context = new SpringApplicationBuilder(Config.class).web(false).run();
		resource = context.getBean(Marathon.class);
		resource.getServerInfo();
	}

	@Configuration
	@EnableConfigurationProperties({MarathonAppDeployerProperties.class, DcosClusterProperties.class})
	public static class Config {

		@Bean
		public Marathon marathon(MarathonAppDeployerProperties marathonProperties,
		                         DcosClusterProperties dcosClusterProperties) {
			if (StringUtils.hasText(dcosClusterProperties.getAuthorizationToken())) {
				return MarathonClient.getInstance(marathonProperties.getApiEndpoint(),
						new DcosHeadersInterceptor(dcosClusterProperties.getAuthorizationToken()));
			}
			else {
				return MarathonClient.getInstance(marathonProperties.getApiEndpoint());
			}
		}
	}
}

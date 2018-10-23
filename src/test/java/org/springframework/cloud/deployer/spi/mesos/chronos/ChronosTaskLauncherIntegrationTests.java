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

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.spi.mesos.MesosAutoConfiguration;
import org.springframework.cloud.deployer.spi.mesos.TestConfig;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.test.AbstractTaskLauncherIntegrationTests;
import org.springframework.cloud.deployer.spi.test.Timeout;
import org.springframework.core.io.Resource;

/**
 * Integration tests for {@link ChronosTaskLauncher}.
 *
 * @author Thomas Risberg
 */
@SpringBootTest(classes = {TestConfig.class, MesosAutoConfiguration.class})
public class ChronosTaskLauncherIntegrationTests extends AbstractTaskLauncherIntegrationTests{

	@Autowired
	TaskLauncher taskLauncher;

	@ClassRule
	public static ChronosTestSupport chronosAvailable = new ChronosTestSupport();

	@Override
	protected TaskLauncher provideTaskLauncher() {
		return taskLauncher;
	}

	@Test
	@Override
	@Ignore("Currently reported as failed instead of cancelled")
	public void testSimpleCancel() throws InterruptedException {
		super.testSimpleCancel();
	}

	@Override
	protected Resource testApplication() {
		return new DockerResource("springcloud/spring-cloud-deployer-spi-test-app:latest");
	}

	@Override
	protected org.springframework.cloud.deployer.spi.test.Timeout deploymentTimeout() {
		return new Timeout(30, 5000);
	}
}

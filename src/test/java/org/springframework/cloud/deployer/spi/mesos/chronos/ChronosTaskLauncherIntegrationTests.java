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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.mesos.MesosAutoConfiguration;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskStatus;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.springframework.cloud.deployer.spi.task.LaunchState.complete;
import static org.springframework.cloud.deployer.spi.task.LaunchState.failed;
import static org.springframework.cloud.deployer.spi.test.EventuallyMatcher.eventually;

/**
 * Integration tests for {@link ChronosTaskLauncher}.
 *
 * @author Thomas Risberg
 */
@SpringApplicationConfiguration(classes = {MesosAutoConfiguration.class})
@RunWith(SpringJUnit4ClassRunner.class)
public class ChronosTaskLauncherIntegrationTests {
	private static final Log logger = LogFactory.getLog(ChronosTaskLauncherIntegrationTests.class);

	@Autowired
	TaskLauncher taskLauncher;

	@ClassRule
	public static ChronosTestSupport chronosAvailable = new ChronosTestSupport();

	@Test
	public void testSimpleLaunch() {
		logger.info(String.format("Testing %s...", "SimpleLaunch"));
		Map<String, String> properties = new HashMap<>();
		properties.put("killDelay", "1000");
		properties.put("exitCode", "0");
		AppDefinition definition = new AppDefinition(this.randomName(), properties);
		Resource resource = integrationTestTask();
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource);
		logger.info(String.format("Launching %s...", request.getDefinition().getName()));
		String deploymentId = taskLauncher.launch(request);
		logger.info(String.format("Launched %s", deploymentId));

		Timeout timeout = launchTimeout();
		assertThat(deploymentId, eventually(hasStatusThat(
				Matchers.<TaskStatus>hasProperty("state", is(complete))), timeout.maxAttempts, timeout.pause));

		((ChronosTaskLauncher)taskLauncher).cleanup(deploymentId);
	}

	@Test
	public void testReLaunch() {
		logger.info(String.format("Testing %s...", "ReLaunch"));
		Map<String, String> properties = new HashMap<>();
		properties.put("killDelay", "1000");
		properties.put("exitCode", "0");
		AppDefinition definition = new AppDefinition(this.randomName(), properties);
		Resource resource = integrationTestTask();
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource);
		logger.info(String.format("Launching %s...", request.getDefinition().getName()));
		String deploymentId = taskLauncher.launch(request);
		logger.info(String.format("Launched %s ", deploymentId));
		Timeout timeout = launchTimeout();
		Assert.assertThat(deploymentId, eventually(hasStatusThat(
				Matchers.<TaskStatus>hasProperty("state", Matchers.is(complete))), timeout.maxAttempts, timeout.pause));

		deploymentId = taskLauncher.launch(request);
		logger.info(String.format("Re-launched %s ", deploymentId));
		Assert.assertThat(deploymentId, eventually(hasStatusThat(
				Matchers.<TaskStatus>hasProperty("state", Matchers.is(complete))), timeout.maxAttempts, timeout.pause));

		((ChronosTaskLauncher)taskLauncher).cleanup(deploymentId);
	}

	@Test
	public void testFailedLaunch() {
		logger.info(String.format("Testing %s...", "FailedLaunch"));
		Map<String, String> properties = new HashMap<>();
		properties.put("killDelay", "1000");
		properties.put("exitCode", "1");
		AppDefinition definition = new AppDefinition(this.randomName(), properties);
		Resource resource = integrationTestTask();
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource);
		logger.info(String.format("Launching %s...", request.getDefinition().getName()));
		String deploymentId = taskLauncher.launch(request);
		logger.info(String.format("Launched %s ", deploymentId));

		Timeout timeout = launchTimeout();
		Assert.assertThat(deploymentId, eventually(hasStatusThat(
				Matchers.<TaskStatus>hasProperty("state", Matchers.is(failed))), timeout.maxAttempts, timeout.pause));

		((ChronosTaskLauncher)taskLauncher).cleanup(deploymentId);
	}

	/**
	 * Tests that command line args can be passed in.
	 */
	@Test
	public void testCommandLineArgs() {
		logger.info(String.format("Testing %s...", "CommandLineArgs"));
		Map<String, String> properties = new HashMap<>();
		properties.put("killDelay", "1000");
		AppDefinition definition = new AppDefinition(this.randomName(), properties);
		Resource resource = integrationTestTask();
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource, Collections.emptyMap(),
				Collections.singletonList("--exitCode=0"));
		logger.info(String.format("Launching %s...", request.getDefinition().getName()));
		String deploymentId = taskLauncher.launch(request);
		logger.info(String.format("Launched %s ", deploymentId));

		Timeout timeout = launchTimeout();
		Assert.assertThat(deploymentId, eventually(hasStatusThat(
				Matchers.<TaskStatus>hasProperty("state", Matchers.is(complete))), timeout.maxAttempts, timeout.pause));

		((ChronosTaskLauncher)taskLauncher).cleanup(deploymentId);
	}

	protected String randomName() {
		return "job-" + UUID.randomUUID().toString().substring(0, 18);
	}

	protected Resource integrationTestTask() {
		return new DockerResource("springcloud/spring-cloud-deployer-spi-test-app:latest");
	}

	protected Timeout launchTimeout() {
		return new Timeout(20, 5000);
	}

	protected Matcher<String> hasStatusThat(final Matcher<TaskStatus> statusMatcher) {
		return new BaseMatcher() {
			private TaskStatus status;

			public boolean matches(Object item) {
				this.status = ChronosTaskLauncherIntegrationTests.this.taskLauncher.status((String)item);
				return statusMatcher.matches(this.status);
			}

			public void describeMismatch(Object item, Description mismatchDescription) {
				mismatchDescription.appendText("status of ").appendValue(item).appendText(" ");
				statusMatcher.describeMismatch(this.status, mismatchDescription);
			}

			public void describeTo(Description description) {
				statusMatcher.describeTo(description);
			}
		};
	}

	protected static class Timeout {
		public final int maxAttempts;
		public final int pause;

		public Timeout(int maxAttempts, int pause) {
			this.maxAttempts = maxAttempts;
			this.pause = pause;
		}
	}
}

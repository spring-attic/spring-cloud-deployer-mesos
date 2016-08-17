package org.springframework.cloud.mesos.chronos.client;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.cloud.mesos.chronos.client.model.DockerContainer;
import org.springframework.cloud.mesos.chronos.client.model.Job;
import org.springframework.cloud.mesos.chronos.client.model.DockerJob;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static org.junit.Assert.assertEquals;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import java.util.List;

public class ChronosClientTests {

	@Rule
	public WireMockRule wireMockRule = new WireMockRule();

	Chronos client = ChronosClient.getInstance("http://localhost:8080");

	@Test
	public void testClientGetJobs() throws ChronosException {
		stubFor(get(urlEqualTo("/scheduler/jobs"))
				.willReturn(aResponse()
						.withHeader("Content-Type", "application/json")
						.withBody("[{\"name\":\"test1\",\"command\":\"pwd\",\"description\":\"My test job\"}," +
								"{\"name\":\"test2\",\"command\":\"ls\",\"description\":\"Test2\"}]")));
		List<Job> jobs = client.getJobs();
		assertEquals("Jobs found", 2, jobs.size());
		assertEquals("Job 1 data found", "test1", jobs.get(0).getName());
		assertEquals("Job 1 data found", "My test job", jobs.get(0).getDescription());
		assertEquals("Job 2 data found", "test2", jobs.get(1).getName());
	}

	@Test
	public void testClientAddJob() throws ChronosException {
		stubFor(post(urlEqualTo("/scheduler/iso8601")).willReturn(aResponse()));
		DockerJob job = new DockerJob();
		job.setName("test");
		job.setOwner("test@example.com");
		job.setCommand("docker run -e SPRING_APPLICATION_JSON='{\"killDelay\":\"10000\",\"exitCode\":\"0\"}' springcloud/spring-cloud-deployer-spi-test-app");
		job.setSchedule("R1//P");
		DockerContainer container = new DockerContainer();
		container.setImage("docker pull springcloud/spring-cloud-deployer-spi-test-app");
		job.setContainer(container);
		client.createJob(job);
	}

	@Test
	public void testClientStartJob() throws ChronosException {
		stubFor(put(urlEqualTo("/scheduler/job/test")).willReturn(aResponse()));
		client.startJob("test");
	}

	@Test
	public void testClientDeleteJob() throws ChronosException {
		stubFor(delete(urlEqualTo("/scheduler/job/test")).willReturn(aResponse()));
		client.deleteJob("test");
	}

	@Test
	public void testClientDeleteJobTasks() throws ChronosException {
		stubFor(delete(urlEqualTo("/scheduler/task/kill/test")).willReturn(aResponse()));
		client.deleteJobTasks("test");
	}

}

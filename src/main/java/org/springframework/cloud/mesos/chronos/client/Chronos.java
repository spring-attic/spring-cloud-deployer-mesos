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

package org.springframework.cloud.mesos.chronos.client;

import java.util.List;

import org.springframework.cloud.mesos.chronos.client.model.DockerJob;
import org.springframework.cloud.mesos.chronos.client.model.Job;

import feign.Headers;
import feign.Param;
import feign.RequestLine;

/**
 * Interface defining REST end-points to be used when interacting with Chronos
 *
 * @author Thomas Risberg
 */
public interface Chronos {

	@RequestLine("GET /v1/scheduler/jobs")
	List<Job> getJobs() throws ChronosException;

	@RequestLine("GET /v1/scheduler/graph/csv")
	@Headers("Accept: text/plain")
	String getGraphCsv() throws ChronosException;

	@RequestLine("POST /v1/scheduler/iso8601")
	void createJob(Job job) throws ChronosException;

	@RequestLine("POST /v1/scheduler/iso8601")
	void createDockerJob(DockerJob job) throws ChronosException;

	@RequestLine("PUT /v1/scheduler/job/{jobName}")
	void startJob(@Param("jobName") String jobName) throws ChronosException;

	@RequestLine("DELETE /v1/scheduler/job/{jobName}")
	void deleteJob(@Param("jobName") String jobName) throws ChronosException;

	@RequestLine("DELETE /v1/scheduler/task/kill/{jobName}")
	void deleteJobTasks(@Param("jobName") String jobName) throws ChronosException;

}

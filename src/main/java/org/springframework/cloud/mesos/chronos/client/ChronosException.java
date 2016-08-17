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

package org.springframework.cloud.mesos.chronos.client;

/**
 * Chronos Exception
 *
 * Based on similar exception class for Marathon - {@link mesosphere.marathon.client.utils.MarathonException}
 *
 * @author Thomas Risberg
 */
public class ChronosException extends Exception {
	private static final long serialVersionUID = 1L;
	private int status;
	private String message;

	public ChronosException(int status, String message) {
		this.status = status;
		this.message = message;
	}

    /**
     * Gets the HTTP status code of the failure, such as 404.
     */
    public int getStatus() {
        return status;
    }

    @Override
	public String getMessage() {
		return message + " (http status: " + status + ")";
	}

	@Override
	public String toString() {
		return message + " (http status: " + status + ")";
	}
}

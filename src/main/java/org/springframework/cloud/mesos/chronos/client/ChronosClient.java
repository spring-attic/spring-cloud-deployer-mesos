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

import static java.util.Arrays.asList;

import org.springframework.cloud.mesos.chronos.client.model.AbstractModel;

import feign.Feign;
import feign.Feign.Builder;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Response;
import feign.auth.BasicAuthRequestInterceptor;
import feign.codec.ErrorDecoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;

/**
 * Class for creating a Feign client for use with Chronos. Based on similar class
 * used with Marathon - {@link mesosphere.marathon.client.Marathon}
 *
 * @author Thomas Risberg
 */
public class ChronosClient {

	static class ChronosHeadersInterceptor implements RequestInterceptor {
		@Override
		public void apply(RequestTemplate template) {
			template.header("Accept", "application/json");
			template.header("Content-Type", "application/json");
		}
	}
	
	static class ChronosErrorDecoder implements ErrorDecoder {
		@Override
		public Exception decode(String methodKey, Response response) {
			System.out.println(methodKey);
			System.out.println(response);
			return new ChronosException(response.status(), response.reason());
		}
	}
	
	public static Chronos getInstance(String endpoint) {
		return getInstance(endpoint, null);
	}

	/**
	 * The generalized version of the method that allows more in-depth customizations via
	 * {@link RequestInterceptor}s.
	 *
	 * @param endpoint
	 * 		URL of Chronos
	 */
	public static Chronos getInstance(String endpoint, RequestInterceptor... interceptors) {
		Builder b = Feign.builder()
				.encoder(new GsonEncoder(AbstractModel.GSON))
				.decoder(new GsonDecoder(AbstractModel.GSON))
				.errorDecoder(new ChronosErrorDecoder());
		if (interceptors != null) {
			b.requestInterceptors(asList(interceptors));
		}
		b.requestInterceptor(new ChronosHeadersInterceptor());
		return b.target(Chronos.class, endpoint);
	}

	/**
	 * Creates a Chronos client proxy that performs HTTP basic authentication.
	 */
	public static Chronos getInstanceWithBasicAuth(String endpoint, String username, String password) {
		return getInstance(endpoint,new BasicAuthRequestInterceptor(username,password));
	}
}

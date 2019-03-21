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

import static java.util.Arrays.asList;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;

import org.springframework.cloud.mesos.chronos.client.model.AbstractModel;
import org.springframework.util.MimeTypeUtils;

import feign.Feign;
import feign.Feign.Builder;
import feign.FeignException;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.Response;
import feign.auth.BasicAuthRequestInterceptor;
import feign.codec.DecodeException;
import feign.codec.Decoder;
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
			template.header("Content-Type", "application/json");
		}
	}
	
	static class ChronosErrorDecoder implements ErrorDecoder {
		@Override
		public Exception decode(String methodKey, Response response) {
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
	 * @param endpoint URL for Chronos API
	 */
	public static Chronos getInstance(String endpoint, RequestInterceptor... interceptors) {
		Builder b = Feign.builder()
				.encoder(new GsonEncoder(AbstractModel.GSON))
				.decoder(new MultiDecoder())
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
		return getInstance(endpoint, new BasicAuthRequestInterceptor(username, password));
	}

	public static class MultiDecoder implements Decoder {

		GsonDecoder gsonDecoder = new GsonDecoder(AbstractModel.GSON);

		Decoder defaultDecoder = new Default();

		@Override
		public Object decode(Response response, Type type) throws IOException, DecodeException, FeignException {
			Collection<String> contentTypes = response.headers().get("Content-Type");
			if (contentTypes.contains(MimeTypeUtils.TEXT_PLAIN.toString())) {
				return defaultDecoder.decode(response, type);
			}
			else {
				return gsonDecoder.decode(response, type);
			}
		}
	}
}

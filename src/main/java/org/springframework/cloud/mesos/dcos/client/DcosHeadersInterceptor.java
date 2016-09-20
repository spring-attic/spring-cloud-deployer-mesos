package org.springframework.cloud.mesos.dcos.client;

import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * Class that implements {@link RequestInterceptor} to provide authorization header with the configured token.
 */
public class DcosHeadersInterceptor implements RequestInterceptor {

	private final String headerValue;

	public DcosHeadersInterceptor(String token) {
		this.headerValue = "token=" + token;
	}

	@Override
	public void apply(RequestTemplate template) {
		template.header("Authorization", new String[]{this.headerValue});
	}
}


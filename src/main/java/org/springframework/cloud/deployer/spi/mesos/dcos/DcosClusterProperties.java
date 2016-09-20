package org.springframework.cloud.deployer.spi.mesos.dcos;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for connecting to a DC/OS cluster.
 *
 * @author Thomas Risberg
 */
@ConfigurationProperties("spring.cloud.deployer.mesos.dcos")
public class DcosClusterProperties {

	/**
	 * Token for authorization header when connecting to secured DC/OS cluster.
	 */
	private String authorizationToken;

	public String getAuthorizationToken() {
		return authorizationToken;
	}

	public void setAuthorizationToken(String authorizationToken) {
		this.authorizationToken = authorizationToken;
	}
}

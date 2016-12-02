package org.springframework.cloud.deployer.spi.mesos.constraints;

import org.springframework.core.convert.converter.Converter;

/**
 * Created by ericbottard on 01/12/16.
 */
public class ConstraintConverter implements Converter<String, Constraint> {

	@Override
	public Constraint convert(String source) {
		return new Constraint(source);
	}
}

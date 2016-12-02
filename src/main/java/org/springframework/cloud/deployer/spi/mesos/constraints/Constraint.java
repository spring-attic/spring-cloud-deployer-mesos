package org.springframework.cloud.deployer.spi.mesos.constraints;


import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;


/**
 * Represents a placement constraint, with a field, operator and optional parameter.
 *
 * @author Eric Bottard
 */
public class Constraint {

	private static final Pattern PARSE_REGEX = Pattern.compile("(?<field>[^ ]+) (?<op>[^ ]+)( (?<param>.+))?");

	private final String field;

	private final String operator;

	private final String parameter;

	public Constraint(String raw) {
		Matcher matcher = PARSE_REGEX.matcher(raw);
		Assert.isTrue(matcher.matches(), "Could not parse [" + raw + "] as a Marathon constraint (field operator param?)");
		this.field = matcher.group("field");
		this.operator = matcher.group("op");
		this.parameter = matcher.group("param"); // may be null
	}

	public List<String> toStringList() {
		if (this.parameter != null) {
			return Arrays.asList(this.field, this.operator, this.parameter);
		}
		else {
			return Arrays.asList(this.field, this.operator);
		}
	}
}

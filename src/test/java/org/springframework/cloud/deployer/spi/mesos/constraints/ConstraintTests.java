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

package org.springframework.cloud.deployer.spi.mesos.constraints;

import static org.junit.Assert.assertThat;

import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.Test;

/**
 * Unit tests for {@link Constraint}.
 *
 * @author Eric Bottard
 */
public class ConstraintTests {

	@Test(expected = IllegalArgumentException.class)
	public void testMalformed() {
		new Constraint("some");
	}

	@Test
	public void testParsing() {
		assertThat(new Constraint("some op").toStringList(), IsIterableContainingInOrder.contains("some", "op"));
		assertThat(new Constraint("some op value").toStringList(), IsIterableContainingInOrder.contains("some", "op", "value"));
	}

}

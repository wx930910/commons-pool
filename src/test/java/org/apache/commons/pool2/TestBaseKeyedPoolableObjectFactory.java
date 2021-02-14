/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.pool2;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.junit.jupiter.api.Test;

/**
 */
public class TestBaseKeyedPoolableObjectFactory {

	public static BaseKeyedPooledObjectFactory<Object, Object> mockBaseKeyedPooledObjectFactory1() throws Exception {
		BaseKeyedPooledObjectFactory<Object, Object> mockInstance = spy(BaseKeyedPooledObjectFactory.class);
		doAnswer((stubInvo) -> {
			Object value = stubInvo.getArgument(0);
			return new DefaultPooledObject<>(value);
		}).when(mockInstance).wrap(any());
		return mockInstance;
	}

	@Test
	public void testDefaultMethods() throws Exception {
		final KeyedPooledObjectFactory<Object, Object> factory = TestBaseKeyedPoolableObjectFactory
				.mockBaseKeyedPooledObjectFactory1();

		factory.activateObject("key", null); // a no-op
		factory.passivateObject("key", null); // a no-op
		factory.destroyObject("key", null); // a no-op
		assertTrue(factory.validateObject("key", null)); // constant true
	}
}
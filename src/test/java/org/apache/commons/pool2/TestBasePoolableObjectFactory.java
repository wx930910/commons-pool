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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.junit.jupiter.api.Test;

/**
 */
public class TestBasePoolableObjectFactory {

	public static BasePooledObjectFactory<AtomicInteger> mockBasePooledObjectFactory1() throws Exception {
		BasePooledObjectFactory<AtomicInteger> mockInstance = spy(BasePooledObjectFactory.class);
		doAnswer((stubInvo) -> {
			PooledObject<AtomicInteger> p = stubInvo.getArgument(0);
			DestroyMode mode = stubInvo.getArgument(1);
			if (mode.equals(DestroyMode.ABANDONED)) {
				p.getObject().incrementAndGet();
			}
			return null;
		}).when(mockInstance).destroyObject(any(PooledObject.class), any(DestroyMode.class));
		doReturn(new AtomicInteger(0)).when(mockInstance).create();
		doAnswer((stubInvo) -> {
			AtomicInteger value = stubInvo.getArgument(0);
			return new DefaultPooledObject<>(value);
		}).when(mockInstance).wrap(any(AtomicInteger.class));
		return mockInstance;
	}

	@Test
	public void testDefaultMethods() throws Exception, Exception {
		final PooledObjectFactory<AtomicInteger> factory = TestBasePoolableObjectFactory.mockBasePooledObjectFactory1();

		factory.activateObject(null); // a no-op
		factory.passivateObject(null); // a no-op
		factory.destroyObject(null); // a no-op
		assertTrue(factory.validateObject(null)); // constant true
	}

	/**
	 * Default destroy does nothing to underlying AtomicInt, ABANDONED mode
	 * increments the value. Verify that destroy with no mode does default, destroy
	 * with ABANDONED mode increments.
	 *
	 * @throws Exception May occur in some failure modes
	 */
	@Test
	public void testDestroyModes() throws Exception, Exception {
		final PooledObjectFactory<AtomicInteger> factory = TestBasePoolableObjectFactory.mockBasePooledObjectFactory1();
		final PooledObject<AtomicInteger> pooledObj = factory.makeObject();
		final AtomicInteger obj = pooledObj.getObject();
		factory.destroyObject(pooledObj);
		assertEquals(0, obj.get());
		factory.destroyObject(pooledObj, DestroyMode.ABANDONED);
		assertEquals(1, obj.get());
	}
}

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class PoolTest {
	public static PooledObjectFactory<Foo> mockPooledObjectFactory1() throws Exception {
		long mockFieldVariableVALIDATION_WAIT_IN_MILLIS = 1000;
		PooledObjectFactory<Foo> mockInstance = spy(PooledObjectFactory.class);
		doAnswer((stubInvo) -> {
			return new DefaultPooledObject<>(new Foo());
		}).when(mockInstance).makeObject();
		doAnswer((stubInvo) -> {
			try {
				Thread.sleep(mockFieldVariableVALIDATION_WAIT_IN_MILLIS);
			} catch (final InterruptedException e) {
				Thread.interrupted();
			}
			return false;
		}).when(mockInstance).validateObject(any());
		return mockInstance;
	}

	private static final CharSequence COMMONS_POOL_EVICTIONS_TIMER_THREAD_NAME = "commons-pool-EvictionTimer";
	private static final long EVICTION_PERIOD_IN_MILLIS = 100;

	private static class Foo {
	}

	@Test
	public void testPool() throws Exception {
		final GenericObjectPoolConfig poolConfig = new GenericObjectPoolConfig();
		poolConfig.setTestWhileIdle(true /* testWhileIdle */);
		final PooledObjectFactory<Foo> pooledFooFactory = PoolTest.mockPooledObjectFactory1();
		try (GenericObjectPool<Foo> pool = new GenericObjectPool<>(pooledFooFactory, poolConfig)) {
			pool.setTimeBetweenEvictionRunsMillis(EVICTION_PERIOD_IN_MILLIS);
			pool.addObject();
			try {
				Thread.sleep(EVICTION_PERIOD_IN_MILLIS);
			} catch (final InterruptedException e) {
				Thread.interrupted();
			}
		}
		final Thread[] threads = new Thread[Thread.activeCount()];
		Thread.enumerate(threads);
		for (final Thread thread : threads) {
			if (thread == null) {
				continue;
			}
			final String name = thread.getName();
			assertFalse(name.contains(COMMONS_POOL_EVICTIONS_TIMER_THREAD_NAME), name);
		}
	}
}
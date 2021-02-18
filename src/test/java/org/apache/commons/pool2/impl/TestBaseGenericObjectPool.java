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

package org.apache.commons.pool2.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.TestGenericObjectPool.SimpleFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 */
public class TestBaseGenericObjectPool {

	BaseGenericObjectPool<String> pool = null;
	SimpleFactory factory = null;

	@BeforeEach
	public void setUp() throws Exception {
		factory = new SimpleFactory();
		pool = new GenericObjectPool<>(factory);
	}

	@AfterEach
	public void tearDown() throws Exception {
		pool.close();
		pool = null;
		factory = null;
	}

	@Test
	public void testBorrowWaitStatistics() {
		final DefaultPooledObject<String> p = (DefaultPooledObject<String>) factory.makeObject();
		pool.updateStatsBorrow(p, 10);
		pool.updateStatsBorrow(p, 20);
		pool.updateStatsBorrow(p, 20);
		pool.updateStatsBorrow(p, 30);
		assertEquals(20, pool.getMeanBorrowWaitTimeMillis(), Double.MIN_VALUE);
		assertEquals(30, pool.getMaxBorrowWaitTimeMillis(), 0);
	}

	public void testBorrowWaitStatisticsMax() {
		final DefaultPooledObject<String> p = (DefaultPooledObject<String>) factory.makeObject();
		assertEquals(0, pool.getMaxBorrowWaitTimeMillis(), Double.MIN_VALUE);
		pool.updateStatsBorrow(p, 0);
		assertEquals(0, pool.getMaxBorrowWaitTimeMillis(), Double.MIN_VALUE);
		pool.updateStatsBorrow(p, 20);
		assertEquals(20, pool.getMaxBorrowWaitTimeMillis(), Double.MIN_VALUE);
		pool.updateStatsBorrow(p, 20);
		assertEquals(20, pool.getMaxBorrowWaitTimeMillis(), Double.MIN_VALUE);
		pool.updateStatsBorrow(p, 10);
		assertEquals(20, pool.getMaxBorrowWaitTimeMillis(), Double.MIN_VALUE);
	}

	@Test
	public void testActiveTimeStatistics() {
		for (int i = 0; i < 99; i++) { // must be < MEAN_TIMING_STATS_CACHE_SIZE
			pool.updateStatsReturn(i);
		}
		assertEquals(49, pool.getMeanActiveTimeMillis(), Double.MIN_VALUE);
	}

	@Test
	public void testEvictionTimerMultiplePools()
			throws InterruptedException, Exception, Exception, Exception, Exception {
		final BasePooledObjectFactory<AtomicInteger> factory = spy(BasePooledObjectFactory.class);
		long[] factoryCreateLatency = new long[] { 0 };
		long[] factoryDestroyLatency = new long[] { 0 };
		long[] factoryValidateLatency = new long[] { 0 };
		long[] factoryPassivateLatency = new long[] { 0 };
		long[] factoryActivateLatency = new long[] { 0 };
		doAnswer((stubInvo) -> {
			PooledObject<AtomicInteger> instance = stubInvo.getArgument(0);
			try {
				Thread.sleep(factoryValidateLatency[0]);
			} catch (final InterruptedException ex) {
			}
			return instance.getObject().intValue() == 1;
		}).when(factory).validateObject(any());
		doAnswer((stubInvo) -> {
			PooledObject<AtomicInteger> p = stubInvo.getArgument(0);
			p.getObject().incrementAndGet();
			try {
				Thread.sleep(factoryActivateLatency[0]);
			} catch (final InterruptedException ex) {
			}
			return null;
		}).when(factory).activateObject(any());
		doAnswer((stubInvo) -> {
			try {
				Thread.sleep(factoryCreateLatency[0]);
			} catch (final InterruptedException ex) {
			}
			return new AtomicInteger(0);
		}).when(factory).create();
		doAnswer((stubInvo) -> {
			try {
				Thread.sleep(factoryDestroyLatency[0]);
			} catch (final InterruptedException ex) {
			}
			return null;
		}).when(factory).destroyObject(any());
		doAnswer((stubInvo) -> {
			AtomicInteger integer = stubInvo.getArgument(0);
			return new DefaultPooledObject<>(integer);
		}).when(factory).wrap(any());
		doAnswer((stubInvo) -> {
			PooledObject<AtomicInteger> p = stubInvo.getArgument(0);
			p.getObject().decrementAndGet();
			try {
				Thread.sleep(factoryPassivateLatency[0]);
			} catch (final InterruptedException ex) {
			}
			return null;
		}).when(factory).passivateObject(any());
		factoryValidateLatency[0] = 50;
		try (final GenericObjectPool<AtomicInteger> evictingPool = new GenericObjectPool<>(factory)) {
			evictingPool.setTimeBetweenEvictionRunsMillis(100);
			evictingPool.setNumTestsPerEvictionRun(5);
			evictingPool.setTestWhileIdle(true);
			evictingPool.setMinEvictableIdleTimeMillis(50);
			for (int i = 0; i < 10; i++) {
				try {
					evictingPool.addObject();
				} catch (final Exception e) {
					e.printStackTrace();
				}
			}

			for (int i = 0; i < 1000; i++) {
				try (final GenericObjectPool<AtomicInteger> nonEvictingPool = new GenericObjectPool<>(factory)) {
					// empty
				}
			}

			Thread.sleep(1000);
			assertEquals(0, evictingPool.getNumIdle());
		}
	}
}

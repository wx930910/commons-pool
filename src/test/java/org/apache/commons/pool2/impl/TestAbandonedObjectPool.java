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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.commons.pool2.DestroyMode;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.TrackedUse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * TestCase for AbandonedObjectPool
 */
public class TestAbandonedObjectPool {
	public static PooledObjectFactory<PooledTestObject> mockPooledObjectFactory1() throws Exception {
		long mockFieldVariableValidateLatency;
		long mockFieldVariableDestroyLatency;
		PooledObjectFactory<PooledTestObject> mockInstance = spy(PooledObjectFactory.class);
		mockFieldVariableDestroyLatency = 0;
		mockFieldVariableValidateLatency = 0;
		doAnswer((stubInvo) -> {
			return new DefaultPooledObject<>(new PooledTestObject());
		}).when(mockInstance).makeObject();
		doAnswer((stubInvo) -> {
			PooledObject<PooledTestObject> obj = stubInvo.getArgument(0);
			obj.getObject().setActive(true);
			return null;
		}).when(mockInstance).activateObject(any());
		doAnswer((stubInvo) -> {
			PooledObject<PooledTestObject> obj = stubInvo.getArgument(0);
			DestroyMode mode = stubInvo.getArgument(1);
			obj.getObject().setActive(false);
			Thread.yield();
			if (mockFieldVariableDestroyLatency != 0) {
				Thread.sleep(mockFieldVariableDestroyLatency);
			}
			obj.getObject().destroy(mode);
			return null;
		}).when(mockInstance).destroyObject(any(PooledObject.class), any(DestroyMode.class));
		doAnswer((stubInvo) -> {
			try {
				Thread.sleep(mockFieldVariableValidateLatency);
			} catch (final Exception ex) {
			}
			return true;
		}).when(mockInstance).validateObject(any());
		doAnswer((stubInvo) -> {
			PooledObject<PooledTestObject> obj = stubInvo.getArgument(0);
			obj.getObject().setActive(false);
			return null;
		}).when(mockInstance).passivateObject(any());
		doAnswer((stubInvo) -> {
			PooledObject<PooledTestObject> obj = stubInvo.getArgument(0);
			mockInstance.destroyObject(obj, DestroyMode.NORMAL);
			return null;
		}).when(mockInstance).destroyObject(any(PooledObject.class));
		return mockInstance;
	}

	public static PooledObjectFactory<PooledTestObject> mockPooledObjectFactory2(final long destroyLatency,
			final long validateLatency) throws Exception {
		long mockFieldVariableValidateLatency;
		long mockFieldVariableDestroyLatency;
		PooledObjectFactory<PooledTestObject> mockInstance = spy(PooledObjectFactory.class);
		mockFieldVariableDestroyLatency = destroyLatency;
		mockFieldVariableValidateLatency = validateLatency;
		doAnswer((stubInvo) -> {
			return new DefaultPooledObject<>(new PooledTestObject());
		}).when(mockInstance).makeObject();
		doAnswer((stubInvo) -> {
			PooledObject<PooledTestObject> obj = stubInvo.getArgument(0);
			obj.getObject().setActive(true);
			return null;
		}).when(mockInstance).activateObject(any());
		doAnswer((stubInvo) -> {
			PooledObject<PooledTestObject> obj = stubInvo.getArgument(0);
			DestroyMode mode = stubInvo.getArgument(1);
			obj.getObject().setActive(false);
			Thread.yield();
			if (mockFieldVariableDestroyLatency != 0) {
				Thread.sleep(mockFieldVariableDestroyLatency);
			}
			obj.getObject().destroy(mode);
			return null;
		}).when(mockInstance).destroyObject(any(PooledObject.class), any(DestroyMode.class));
		doAnswer((stubInvo) -> {
			try {
				Thread.sleep(mockFieldVariableValidateLatency);
			} catch (final Exception ex) {
			}
			return true;
		}).when(mockInstance).validateObject(any());
		doAnswer((stubInvo) -> {
			PooledObject<PooledTestObject> obj = stubInvo.getArgument(0);
			obj.getObject().setActive(false);
			return null;
		}).when(mockInstance).passivateObject(any());
		doAnswer((stubInvo) -> {
			PooledObject<PooledTestObject> obj = stubInvo.getArgument(0);
			mockInstance.destroyObject(obj, DestroyMode.NORMAL);
			return null;
		}).when(mockInstance).destroyObject(any(PooledObject.class));
		return mockInstance;
	}

	private GenericObjectPool<PooledTestObject> pool = null;
	private AbandonedConfig abandonedConfig = null;

	@BeforeEach
	public void setUp() throws Exception {
		abandonedConfig = new AbandonedConfig();

		// -- Uncomment the following line to enable logging --
		// abandonedConfig.setLogAbandoned(true);

		abandonedConfig.setRemoveAbandonedOnBorrow(true);
		abandonedConfig.setRemoveAbandonedTimeout(1);
		pool = new GenericObjectPool<>(TestAbandonedObjectPool.mockPooledObjectFactory1(),
				new GenericObjectPoolConfig<PooledTestObject>(), abandonedConfig);
	}

	@AfterEach
	public void tearDown() throws Exception {
		final String poolName = pool.getJmxName().toString();
		pool.clear();
		pool.close();
		pool = null;

		final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
		final Set<ObjectName> result = mbs
				.queryNames(new ObjectName("org.apache.commoms.pool2:type=GenericObjectPool,*"), null);
		// There should be no registered pools at this point
		final int registeredPoolCount = result.size();
		final StringBuilder msg = new StringBuilder("Current pool is: ");
		msg.append(poolName);
		msg.append("  Still open pools are: ");
		for (final ObjectName name : result) {
			// Clean these up ready for the next test
			msg.append(name.toString());
			msg.append(" created via\n");
			msg.append(mbs.getAttribute(name, "CreationStackTrace"));
			msg.append('\n');
			mbs.unregisterMBean(name);
		}
		assertEquals(0, registeredPoolCount, msg.toString());
	}

	/**
	 * Tests fix for Bug 28579, a bug in AbandonedObjectPool that causes numActive
	 * to go negative in GenericObjectPool
	 *
	 * @throws Exception May occur in some failure modes
	 */
	@Test
	public void testConcurrentInvalidation() throws Exception {
		final int POOL_SIZE = 30;
		pool.setMaxTotal(POOL_SIZE);
		pool.setMaxIdle(POOL_SIZE);
		pool.setBlockWhenExhausted(false);

		// Exhaust the connection pool
		final ArrayList<PooledTestObject> vec = new ArrayList<>();
		for (int i = 0; i < POOL_SIZE; i++) {
			vec.add(pool.borrowObject());
		}

		// Abandon all borrowed objects
		for (final PooledTestObject element : vec) {
			element.setAbandoned(true);
		}

		// Try launching a bunch of borrows concurrently. Abandoned sweep will be
		// triggered for each.
		final int CONCURRENT_BORROWS = 5;
		final Thread[] threads = new Thread[CONCURRENT_BORROWS];
		for (int i = 0; i < CONCURRENT_BORROWS; i++) {
			threads[i] = new ConcurrentBorrower(vec);
			threads[i].start();
		}

		// Wait for all the threads to finish
		for (int i = 0; i < CONCURRENT_BORROWS; i++) {
			threads[i].join();
		}

		// Return all objects that have not been destroyed
		for (final PooledTestObject pto : vec) {
			if (pto.isActive()) {
				pool.returnObject(pto);
			}
		}

		// Now, the number of active instances should be 0
		assertTrue(pool.getNumActive() == 0, "numActive should have been 0, was " + pool.getNumActive());
	}

	/**
	 * Verify that an object that gets flagged as abandoned and is subsequently
	 * returned is destroyed instead of being returned to the pool (and possibly
	 * later destroyed inappropriately).
	 *
	 * @throws Exception May occur in some failure modes
	 */
	@Test
	public void testAbandonedReturn() throws Exception {
		abandonedConfig = new AbandonedConfig();
		abandonedConfig.setRemoveAbandonedOnBorrow(true);
		abandonedConfig.setRemoveAbandonedTimeout(1);
		pool.close(); // Unregister pool created by setup
		pool = new GenericObjectPool<>(TestAbandonedObjectPool.mockPooledObjectFactory2(200, 0),
				new GenericObjectPoolConfig<PooledTestObject>(), abandonedConfig);
		final int n = 10;
		pool.setMaxTotal(n);
		pool.setBlockWhenExhausted(false);
		PooledTestObject obj = null;
		for (int i = 0; i < n - 2; i++) {
			obj = pool.borrowObject();
		}
		Objects.requireNonNull(obj, "Unable to borrow object from pool");
		final int deadMansHash = obj.hashCode();
		final ConcurrentReturner returner = new ConcurrentReturner(obj);
		Thread.sleep(2000); // abandon checked out instances
		// Now start a race - returner waits until borrowObject has kicked
		// off removeAbandoned and then returns an instance that borrowObject
		// will deem abandoned. Make sure it is not returned to the borrower.
		returner.start(); // short delay, then return instance
		assertTrue(pool.borrowObject().hashCode() != deadMansHash);
		assertEquals(0, pool.getNumIdle());
		assertEquals(1, pool.getNumActive());
	}

	/**
	 * Verify that an object that gets flagged as abandoned and is subsequently
	 * invalidated is only destroyed (and pool counter decremented) once.
	 *
	 * @throws Exception May occur in some failure modes
	 */
	@Test
	public void testAbandonedInvalidate() throws Exception {
		abandonedConfig = new AbandonedConfig();
		abandonedConfig.setRemoveAbandonedOnMaintenance(true);
		abandonedConfig.setRemoveAbandonedTimeout(1);
		pool.close(); // Unregister pool created by setup
		pool = new GenericObjectPool<>(TestAbandonedObjectPool.mockPooledObjectFactory2(200, 0),
				new GenericObjectPoolConfig<PooledTestObject>(), abandonedConfig);
		final int n = 10;
		pool.setMaxTotal(n);
		pool.setBlockWhenExhausted(false);
		pool.setTimeBetweenEvictionRunsMillis(500);
		PooledTestObject obj = null;
		for (int i = 0; i < 5; i++) {
			obj = pool.borrowObject();
		}
		Thread.sleep(1000); // abandon checked out instances and let evictor start
		pool.invalidateObject(obj); // Should not trigger another destroy / decrement
		Thread.sleep(2000); // give evictor time to finish destroys
		assertEquals(0, pool.getNumActive());
		assertEquals(5, pool.getDestroyedCount());
	}

	public void testDestroyModeAbandoned() throws Exception {
		abandonedConfig = new AbandonedConfig();
		abandonedConfig.setRemoveAbandonedOnMaintenance(true);
		abandonedConfig.setRemoveAbandonedTimeout(1);
		pool.close(); // Unregister pool created by setup
		pool = new GenericObjectPool<>(TestAbandonedObjectPool.mockPooledObjectFactory2(0, 0),
				new GenericObjectPoolConfig<PooledTestObject>(), abandonedConfig);
		pool.setTimeBetweenEvictionRunsMillis(50);
		// Borrow an object, wait long enough for it to be abandoned
		final PooledTestObject obj = pool.borrowObject();
		Thread.sleep(100);
		assertTrue(obj.isDetached());
	}

	public void testDestroyModeNormal() throws Exception {
		abandonedConfig = new AbandonedConfig();
		pool.close(); // Unregister pool created by setup
		pool = new GenericObjectPool<>(TestAbandonedObjectPool.mockPooledObjectFactory2(0, 0));
		pool.setMaxIdle(0);
		final PooledTestObject obj = pool.borrowObject();
		pool.returnObject(obj);
		assertTrue(obj.isDestroyed());
		assertFalse(obj.isDetached());
	}

	/**
	 * Verify that an object that the evictor identifies as abandoned while it is in
	 * process of being returned to the pool is not destroyed.
	 *
	 * @throws Exception May occur in some failure modes
	 */
	@Test
	public void testRemoveAbandonedWhileReturning() throws Exception {
		abandonedConfig = new AbandonedConfig();
		abandonedConfig.setRemoveAbandonedOnMaintenance(true);
		abandonedConfig.setRemoveAbandonedTimeout(1);
		pool.close(); // Unregister pool created by setup
		pool = new GenericObjectPool<>(TestAbandonedObjectPool.mockPooledObjectFactory2(0, 1000),
				new GenericObjectPoolConfig<PooledTestObject>(), abandonedConfig);
		final int n = 10;
		pool.setMaxTotal(n);
		pool.setBlockWhenExhausted(false);
		pool.setTimeBetweenEvictionRunsMillis(500);
		pool.setTestOnReturn(true);
		// Borrow an object, wait long enough for it to be abandoned
		// then arrange for evictor to run while it is being returned
		// validation takes a second, evictor runs every 500 ms
		final PooledTestObject obj = pool.borrowObject();
		Thread.sleep(50); // abandon obj
		pool.returnObject(obj); // evictor will run during validation
		final PooledTestObject obj2 = pool.borrowObject();
		assertEquals(obj, obj2); // should get original back
		assertTrue(!obj2.isDestroyed()); // and not destroyed
	}

	/**
	 * Test case for https://issues.apache.org/jira/browse/DBCP-260. Borrow and
	 * abandon all the available objects then attempt to borrow one further object
	 * which should block until the abandoned objects are removed. We don't want the
	 * test to block indefinitely when it fails so use maxWait be check we don't
	 * actually have to wait that long.
	 *
	 * @throws Exception May occur in some failure modes
	 */
	@Test
	public void testWhenExhaustedBlock() throws Exception {
		abandonedConfig.setRemoveAbandonedOnMaintenance(true);
		pool.setAbandonedConfig(abandonedConfig);
		pool.setTimeBetweenEvictionRunsMillis(500);

		pool.setMaxTotal(1);

		@SuppressWarnings("unused") // This is going to be abandoned
		final PooledTestObject o1 = pool.borrowObject();

		final long startMillis = System.currentTimeMillis();
		final PooledTestObject o2 = pool.borrowObject(5000);
		final long endMillis = System.currentTimeMillis();

		pool.returnObject(o2);

		assertTrue(endMillis - startMillis < 5000);
	}

	/**
	 * JIRA: POOL-300
	 */
	@Test
	public void testStackTrace() throws Exception {
		abandonedConfig.setRemoveAbandonedOnMaintenance(true);
		abandonedConfig.setLogAbandoned(true);
		abandonedConfig.setRemoveAbandonedTimeout(1);
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final BufferedOutputStream bos = new BufferedOutputStream(baos);
		final PrintWriter pw = new PrintWriter(bos);
		abandonedConfig.setLogWriter(pw);
		pool.setAbandonedConfig(abandonedConfig);
		pool.setTimeBetweenEvictionRunsMillis(100);
		final PooledTestObject o1 = pool.borrowObject();
		Thread.sleep(2000);
		assertTrue(o1.isDestroyed());
		bos.flush();
		assertTrue(baos.toString().indexOf("Pooled object") >= 0);
	}

	class ConcurrentBorrower extends Thread {
		private final ArrayList<PooledTestObject> _borrowed;

		public ConcurrentBorrower(final ArrayList<PooledTestObject> borrowed) {
			_borrowed = borrowed;
		}

		@Override
		public void run() {
			try {
				_borrowed.add(pool.borrowObject());
			} catch (final Exception e) {
				// expected in most cases
			}
		}
	}

	class ConcurrentReturner extends Thread {
		private final PooledTestObject returned;

		public ConcurrentReturner(final PooledTestObject obj) {
			returned = obj;
		}

		@Override
		public void run() {
			try {
				sleep(20);
				pool.returnObject(returned);
			} catch (final Exception e) {
				// ignore
			}
		}
	}
}

class PooledTestObject implements TrackedUse {
	private boolean active = false;
	private boolean destroyed = false;
	private int _hash = 0;
	private boolean _abandoned = false;
	private static final AtomicInteger hash = new AtomicInteger();
	private boolean detached = false; // destroy-abandoned "detaches"

	public PooledTestObject() {
		_hash = hash.incrementAndGet();
	}

	public synchronized void setActive(final boolean b) {
		active = b;
	}

	public synchronized boolean isActive() {
		return active;
	}

	public void destroy(final DestroyMode mode) {
		destroyed = true;
		if (mode.equals(DestroyMode.ABANDONED)) {
			detached = true;
		}
	}

	public boolean isDestroyed() {
		return destroyed;
	}

	public boolean isDetached() {
		return detached;
	}

	@Override
	public int hashCode() {
		return _hash;
	}

	public void setAbandoned(final boolean b) {
		_abandoned = b;
	}

	@Override
	public long getLastUsed() {
		if (_abandoned) {
			// Abandoned object sweep will occur no matter what the value of
			// removeAbandonedTimeout,
			// because this indicates that this object was last used decades ago
			return 1;
		}
		// Abandoned object sweep won't clean up this object
		return 0;
	}

	@Override
	public boolean equals(final Object obj) {
		if (!(obj instanceof PooledTestObject)) {
			return false;
		}
		return obj.hashCode() == hashCode();
	}
}

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.junit.After;
import org.junit.Test;

/**
 */
public class TestSoftRefOutOfMemory {
	public static BasePooledObjectFactory<String> mockBasePooledObjectFactory3(final OomeTrigger trigger)
			throws Exception {
		OomeTrigger mockFieldVariableTrigger;
		BasePooledObjectFactory<String> mockInstance = spy(BasePooledObjectFactory.class);
		mockFieldVariableTrigger = trigger;
		doAnswer((stubInvo) -> {
			if (mockFieldVariableTrigger.equals(OomeTrigger.CREATE)) {
				throw new OutOfMemoryError();
			}
			return new String();
		}).when(mockInstance).create();
		doAnswer((stubInvo) -> {
			String value = stubInvo.getArgument(0);
			return new DefaultPooledObject<>(value);
		}).when(mockInstance).wrap(any());
		doAnswer((stubInvo) -> {
			if (mockFieldVariableTrigger.equals(OomeTrigger.VALIDATE)) {
				throw new OutOfMemoryError();
			}
			if (mockFieldVariableTrigger.equals(OomeTrigger.DESTROY)) {
				return false;
			}
			return true;
		}).when(mockInstance).validateObject(any());
		doAnswer((stubInvo) -> {
			if (mockFieldVariableTrigger.equals(OomeTrigger.DESTROY)) {
				throw new OutOfMemoryError();
			}
			stubInvo.callRealMethod();
			return null;
		}).when(mockInstance).destroyObject(any());
		return mockInstance;
	}

	public static BasePooledObjectFactory<String> mockBasePooledObjectFactory2(final int size) throws Exception {
		int[] mockFieldVariableCounter = new int[] { 0 };
		String mockFieldVariableBuffer;
		BasePooledObjectFactory<String> mockInstance = spy(BasePooledObjectFactory.class);
		final char[] data = new char[size];
		Arrays.fill(data, '.');
		mockFieldVariableBuffer = new String(data);
		doAnswer((stubInvo) -> {
			String value = stubInvo.getArgument(0);
			return new DefaultPooledObject<>(value);
		}).when(mockInstance).wrap(any());
		doAnswer((stubInvo) -> {
			mockFieldVariableCounter[0]++;
			return String.valueOf(mockFieldVariableCounter[0]) + mockFieldVariableBuffer;
		}).when(mockInstance).create();
		return mockInstance;
	}

	public static BasePooledObjectFactory<String> mockBasePooledObjectFactory1() throws Exception {
		int[] mockFieldVariableCounter = new int[] { 0 };
		BasePooledObjectFactory<String> mockInstance = spy(BasePooledObjectFactory.class);
		doAnswer((stubInvo) -> {
			String value = stubInvo.getArgument(0);
			return new DefaultPooledObject<>(value);
		}).when(mockInstance).wrap(any());
		doAnswer((stubInvo) -> {
			mockFieldVariableCounter[0]++;
			return new String(String.valueOf(mockFieldVariableCounter[0]));
		}).when(mockInstance).create();
		return mockInstance;
	}

	private SoftReferenceObjectPool<String> pool;

	@After
	public void tearDown() throws Exception {
		if (pool != null) {
			pool.close();
			pool = null;
		}
		System.gc();
	}

	@Test
	public void testOutOfMemory() throws Exception, Exception {
		pool = new SoftReferenceObjectPool<>(TestSoftRefOutOfMemory.mockBasePooledObjectFactory1());

		String obj = pool.borrowObject();
		assertEquals("1", obj);
		pool.returnObject(obj);
		obj = null;

		assertEquals(1, pool.getNumIdle());

		final List<byte[]> garbage = new LinkedList<>();
		final Runtime runtime = Runtime.getRuntime();
		while (pool.getNumIdle() > 0) {
			try {
				long freeMemory = runtime.freeMemory();
				if (freeMemory > Integer.MAX_VALUE) {
					freeMemory = Integer.MAX_VALUE;
				}
				garbage.add(new byte[Math.min(1024 * 1024, (int) freeMemory / 2)]);
			} catch (final OutOfMemoryError oome) {
				System.gc();
			}
			System.gc();
		}
		garbage.clear();
		System.gc();

		obj = pool.borrowObject();
		assertEquals("2", obj);
		pool.returnObject(obj);
		obj = null;

		assertEquals(1, pool.getNumIdle());
	}

	@Test
	public void testOutOfMemory1000() throws Exception, Exception {
		pool = new SoftReferenceObjectPool<>(TestSoftRefOutOfMemory.mockBasePooledObjectFactory1());

		for (int i = 0; i < 1000; i++) {
			pool.addObject();
		}

		String obj = pool.borrowObject();
		assertEquals("1000", obj);
		pool.returnObject(obj);
		obj = null;

		assertEquals(1000, pool.getNumIdle());

		final List<byte[]> garbage = new LinkedList<>();
		final Runtime runtime = Runtime.getRuntime();
		while (pool.getNumIdle() > 0) {
			try {
				long freeMemory = runtime.freeMemory();
				if (freeMemory > Integer.MAX_VALUE) {
					freeMemory = Integer.MAX_VALUE;
				}
				garbage.add(new byte[Math.min(1024 * 1024, (int) freeMemory / 2)]);
			} catch (final OutOfMemoryError oome) {
				System.gc();
			}
			System.gc();
		}
		garbage.clear();
		System.gc();

		obj = pool.borrowObject();
		assertEquals("1001", obj);
		pool.returnObject(obj);
		obj = null;

		assertEquals(1, pool.getNumIdle());
	}

	@Test
	public void testOutOfMemoryLarge() throws Exception, Exception {
		pool = new SoftReferenceObjectPool<>(TestSoftRefOutOfMemory.mockBasePooledObjectFactory2(1000000));

		String obj = pool.borrowObject();
		assertTrue(obj.startsWith("1."));
		pool.returnObject(obj);
		obj = null;

		assertEquals(1, pool.getNumIdle());

		final List<byte[]> garbage = new LinkedList<>();
		final Runtime runtime = Runtime.getRuntime();
		while (pool.getNumIdle() > 0) {
			try {
				long freeMemory = runtime.freeMemory();
				if (freeMemory > Integer.MAX_VALUE) {
					freeMemory = Integer.MAX_VALUE;
				}
				garbage.add(new byte[Math.min(1024 * 1024, (int) freeMemory / 2)]);
			} catch (final OutOfMemoryError oome) {
				System.gc();
			}
			System.gc();
		}
		garbage.clear();
		System.gc();

		obj = pool.borrowObject();
		assertTrue(obj.startsWith("2."));
		pool.returnObject(obj);
		obj = null;

		assertEquals(1, pool.getNumIdle());
	}

	/**
	 * Makes sure an {@link OutOfMemoryError} isn't swallowed.
	 *
	 * @throws Exception May occur in some failure modes
	 */
	@Test
	public void testOutOfMemoryError() throws Exception, Exception, Exception, Exception {
		pool = new SoftReferenceObjectPool<>(TestSoftRefOutOfMemory.mockBasePooledObjectFactory3(OomeTrigger.CREATE));

		try {
			pool.borrowObject();
			fail("Expected out of memory.");
		} catch (final OutOfMemoryError ex) {
			// expected
		}
		pool.close();

		pool = new SoftReferenceObjectPool<>(TestSoftRefOutOfMemory.mockBasePooledObjectFactory3(OomeTrigger.VALIDATE));

		try {
			pool.borrowObject();
			fail("Expected out of memory.");
		} catch (final OutOfMemoryError ex) {
			// expected
		}
		pool.close();

		pool = new SoftReferenceObjectPool<>(TestSoftRefOutOfMemory.mockBasePooledObjectFactory3(OomeTrigger.DESTROY));

		try {
			pool.borrowObject();
			fail("Expected out of memory.");
		} catch (final OutOfMemoryError ex) {
			// expected
		}
		pool.close();
	}

	private enum OomeTrigger {
		CREATE, VALIDATE, DESTROY
	}
}
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
package org.apache.commons.pool2.proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.KeyedPooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class BaseTestProxiedKeyedObjectPool {

	public static BaseKeyedPooledObjectFactory<String, TestObject> mockBaseKeyedPooledObjectFactory1() {
		BaseKeyedPooledObjectFactory<String, TestObject> mockInstance = spy(BaseKeyedPooledObjectFactory.class);
		try {
			doAnswer((stubInvo) -> {
				TestObject value = stubInvo.getArgument(0);
				return new DefaultPooledObject<>(value);
			}).when(mockInstance).wrap(any());
			doReturn(new TestObjectImpl()).when(mockInstance).create(any());
		} catch (Throwable exception) {
			exception.printStackTrace();
		}
		return mockInstance;
	}

	private static final String KEY1 = "key1";
	private static final String DATA1 = "data1";

	private KeyedObjectPool<String, TestObject> pool = null;

	@BeforeEach
	public void setUp() {
		final GenericKeyedObjectPoolConfig<TestObject> config = new GenericKeyedObjectPoolConfig<>();
		config.setMaxTotal(3);

		final KeyedPooledObjectFactory<String, TestObject> factory = BaseTestProxiedKeyedObjectPool
				.mockBaseKeyedPooledObjectFactory1();

		@SuppressWarnings("resource")
		final KeyedObjectPool<String, TestObject> innerPool = new GenericKeyedObjectPool<>(factory, config);

		pool = new ProxiedKeyedObjectPool<>(innerPool, getproxySource());
	}

	protected abstract ProxySource<TestObject> getproxySource();

	@Test
	public void testBorrowObject() throws Exception {
		final TestObject obj = pool.borrowObject(KEY1);
		assertNotNull(obj);

		// Make sure proxied methods are working
		obj.setData(DATA1);
		assertEquals(DATA1, obj.getData());

		pool.returnObject(KEY1, obj);
	}

	@Test
	public void testAccessAfterReturn() throws Exception {
		final TestObject obj = pool.borrowObject(KEY1);
		assertNotNull(obj);

		// Make sure proxied methods are working
		obj.setData(DATA1);
		assertEquals(DATA1, obj.getData());

		pool.returnObject(KEY1, obj);

		assertNotNull(obj);
		assertThrows(IllegalStateException.class, () -> obj.getData());
	}

	@Test
	public void testAccessAfterInvalidate() throws Exception {
		final TestObject obj = pool.borrowObject(KEY1);
		assertNotNull(obj);

		// Make sure proxied methods are working
		obj.setData(DATA1);
		assertEquals(DATA1, obj.getData());

		pool.invalidateObject(KEY1, obj);

		assertNotNull(obj);

		assertThrows(IllegalStateException.class, () -> obj.getData());

	}

	@Test
	public void testPassThroughMethods01() throws Exception {
		assertEquals(0, pool.getNumActive());
		assertEquals(0, pool.getNumIdle());

		pool.addObject(KEY1);

		assertEquals(0, pool.getNumActive());
		assertEquals(1, pool.getNumIdle());

		pool.clear();

		assertEquals(0, pool.getNumActive());
		assertEquals(0, pool.getNumIdle());
	}

	@Test
	public void testPassThroughMethods02() throws Exception {
		pool.close();
		assertThrows(IllegalStateException.class, () -> pool.addObject(KEY1));
	}

	protected interface TestObject {
		String getData();

		void setData(String data);
	}

	private static class TestObjectImpl implements TestObject {

		private String data;

		@Override
		public String getData() {
			return data;
		}

		@Override
		public void setData(final String data) {
			this.data = data;
		}
	}

}

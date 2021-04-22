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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.TestBaseObjectPool;

/**
 */
public class TestSoftReferenceObjectPool extends TestBaseObjectPool {

	public static BasePooledObjectFactory<String> mockBasePooledObjectFactory1() {
		int[] mockFieldVariableCounter = new int[] { 0 };
		BasePooledObjectFactory<String> mockInstance = spy(BasePooledObjectFactory.class);
		try {
			doAnswer((stubInvo) -> {
				return String.valueOf(mockFieldVariableCounter[0]++);
			}).when(mockInstance).create();
			doAnswer((stubInvo) -> {
				String value = stubInvo.getArgument(0);
				return new DefaultPooledObject<>(value);
			}).when(mockInstance).wrap(any(String.class));
		} catch (Throwable exception) {
			exception.printStackTrace();
		}
		return mockInstance;
	}

	@Override
	protected ObjectPool<String> makeEmptyPool(final int cap) {
		return new SoftReferenceObjectPool<>(TestSoftReferenceObjectPool.mockBasePooledObjectFactory1());
	}

	@Override
	protected ObjectPool<Object> makeEmptyPool(final PooledObjectFactory<Object> factory) {
		return new SoftReferenceObjectPool<>(factory);
	}

	@Override
	protected Object getNthObject(final int n) {
		return String.valueOf(n);
	}

	@Override
	protected boolean isLifo() {
		return false;
	}

	@Override
	protected boolean isFifo() {
		return false;
	}
}

/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.pulsar.core;

import static org.mockito.Mockito.spy;

import org.apache.pulsar.client.api.Consumer;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.pulsar.listener.DefaultPulsarMessageListenerContainer;
import org.springframework.util.Assert;

public final class ConsumerTestUtils {

	private ConsumerTestUtils() {

	}

	/**
	 * Provides a Mockito spy object for the message listener container.
	 * @param container container to spy on
	 * @return the spied container object
	 */
	public static Consumer<?> spyOnConsumer(DefaultPulsarMessageListenerContainer<String> container) {
		Consumer<?> consumer = getPropertyValue(container, "listenerConsumer.consumer", Consumer.class);
		consumer = spy(consumer);
		new DirectFieldAccessor(getPropertyValue(container, "listenerConsumer")).setPropertyValue("consumer", consumer);
		return consumer;
	}

	/**
	 * Uses nested {@link DirectFieldAccessor}s to obtain a property using dotted notation
	 * to traverse fields; e.g. "foo.bar.baz" will obtain a reference to the baz field of
	 * the bar field of foo. Adopted from Spring Integration.
	 * @param root The object.
	 * @param propertyPath The path.
	 * @return The field.
	 */
	public static Object getPropertyValue(Object root, String propertyPath) {
		Object value = null;
		DirectFieldAccessor accessor = new DirectFieldAccessor(root);
		String[] tokens = propertyPath.split("\\.");
		for (int i = 0; i < tokens.length; i++) {
			value = accessor.getPropertyValue(tokens[i]);
			if (value != null) {
				accessor = new DirectFieldAccessor(value);
			}
			else if (i == tokens.length - 1) {
				return null;
			}
			else {
				throw new IllegalArgumentException("intermediate property '" + tokens[i] + "' is null");
			}
		}
		return value;
	}

	@SuppressWarnings("unchecked")
	public static <T> T getPropertyValue(Object root, String propertyPath, Class<T> type) {
		Object value = getPropertyValue(root, propertyPath);
		if (value != null) {
			Assert.isAssignable(type, value.getClass());
		}
		return (T) value;
	}

}

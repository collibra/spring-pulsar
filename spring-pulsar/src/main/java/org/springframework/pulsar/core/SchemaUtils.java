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

import org.apache.pulsar.client.api.Schema;

/**
 * Utility class for Pulsar schema inference.
 *
 * @author Soby Chacko
 * @author Alexander Preuß
 */
public final class SchemaUtils {

	private SchemaUtils() {
	}

	public static <T> Schema<T> getSchema(T message) {
		return getSchema(message.getClass());
	}

	public static <T> Schema<T> getSchema(Class<?> messageClass) {
		return getSchema(messageClass, true);
	}

	@SuppressWarnings("unchecked")
	public static <T> Schema<T> getSchema(Class<?> messageClass, boolean returnDefault) {
		switch (messageClass.getName()) {
			case "java.lang.String":
				return (Schema<T>) Schema.STRING;
			case "[B":
				return (Schema<T>) Schema.BYTES;
			case "java.lang.Byte":
			case "byte":
				return (Schema<T>) Schema.INT8;
			case "java.lang.Short":
			case "short":
				return (Schema<T>) Schema.INT16;
			case "java.lang.Integer":
			case "int":
				return (Schema<T>) Schema.INT32;
			case "java.lang.Long":
			case "long":
				return (Schema<T>) Schema.INT64;
			case "java.lang.Boolean":
			case "boolean":
				return (Schema<T>) Schema.BOOL;
			case "java.nio.ByteBuffer":
				return (Schema<T>) Schema.BYTEBUFFER;
			case "java.util.Date":
				return (Schema<T>) Schema.DATE;
			case "java.lang.Double":
			case "double":
				return (Schema<T>) Schema.DOUBLE;
			case "java.lang.Float":
			case "float":
				return (Schema<T>) Schema.FLOAT;
			case "java.time.Instant":
				return (Schema<T>) Schema.INSTANT;
			case "java.time.LocalDate":
				return (Schema<T>) Schema.LOCAL_DATE;
			case "java.time.LocalDateTime":
				return (Schema<T>) Schema.LOCAL_DATE_TIME;
			case "java.time.LocalTime":
				return (Schema<T>) Schema.LOCAL_TIME;
			default:
				return (returnDefault ? (Schema<T>) Schema.BYTES : null);
		}
	}

}

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

package org.springframework.pulsar.listener;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.pulsar.client.api.Messages;
import org.apache.pulsar.common.schema.KeyValue;
import org.apache.pulsar.common.schema.SchemaType;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.pulsar.annotation.EnablePulsar;
import org.springframework.pulsar.annotation.PulsarListener;

/**
 * Test configurations for {@link PulsarListenerTests}.
 *
 * @author Ali Ustek
 */
@EnablePulsar
@Configuration
class SchemaTestConfig {

	private final CountDownLatch jsonLatch = new CountDownLatch(3);

	private final CountDownLatch jsonBatchLatch = new CountDownLatch(3);

	private final CountDownLatch avroLatch = new CountDownLatch(3);

	private final CountDownLatch avroBatchLatch = new CountDownLatch(3);

	private final CountDownLatch keyvalueLatch = new CountDownLatch(3);

	private final CountDownLatch keyvalueBatchLatch = new CountDownLatch(3);

	private final CountDownLatch protobufLatch = new CountDownLatch(3);

	private final CountDownLatch protobufBatchLatch = new CountDownLatch(3);

	@PulsarListener(id = "jsonListener", topics = "json-topic", subscriptionName = "subscription-4",
			schemaType = SchemaType.JSON, properties = { "subscriptionInitialPosition=Earliest" })
	void listenJson(PulsarListenerTests.User message) {
		jsonLatch.countDown();
	}

	@PulsarListener(id = "jsonBatchListener", topics = "json-topic", subscriptionName = "subscription-5",
			schemaType = SchemaType.JSON, batch = true, properties = { "subscriptionInitialPosition=Earliest" })
	void listenJsonBatch(List<PulsarListenerTests.User> messages) {
		messages.forEach(m -> jsonBatchLatch.countDown());
	}

	@PulsarListener(id = "avroListener", topics = "avro-topic", subscriptionName = "subscription-6",
			schemaType = SchemaType.AVRO, properties = { "subscriptionInitialPosition=Earliest" })
	void listenAvro(PulsarListenerTests.User message) {
		avroLatch.countDown();
	}

	@PulsarListener(id = "avroBatchListener", topics = "avro-topic", subscriptionName = "subscription-7",
			schemaType = SchemaType.AVRO, batch = true, properties = { "subscriptionInitialPosition=Earliest" })
	void listenAvroBatch(Messages<PulsarListenerTests.User> messages) {
		messages.forEach(m -> avroBatchLatch.countDown());
	}

	@PulsarListener(id = "keyvalueListener", topics = "keyvalue-topic", subscriptionName = "subscription-8",
			schemaType = SchemaType.KEY_VALUE, properties = { "subscriptionInitialPosition=Earliest" })
	void listenKeyvalue(KeyValue<String, Integer> message) {
		keyvalueLatch.countDown();
	}

	@PulsarListener(id = "keyvalueBatchListener", topics = "keyvalue-topic", subscriptionName = "subscription-9",
			schemaType = SchemaType.KEY_VALUE, batch = true, properties = { "subscriptionInitialPosition=Earliest" })
	void listenKeyvalueBatch(List<KeyValue<String, Integer>> messages) {
		messages.forEach(m -> keyvalueBatchLatch.countDown());
	}

	@PulsarListener(id = "protobufListener", topics = "protobuf-topic", subscriptionName = "subscription-10",
			schemaType = SchemaType.PROTOBUF, properties = { "subscriptionInitialPosition=Earliest" })
	void listenProtobuf(Proto.Person message) {
		protobufLatch.countDown();
	}

	@PulsarListener(id = "protobufBatchListener", topics = "protobuf-topic", subscriptionName = "subscription-11",
			schemaType = SchemaType.PROTOBUF, batch = true, properties = { "subscriptionInitialPosition=Earliest" })
	void listenProtobufBatch(List<Proto.Person> messages) {
		messages.forEach(m -> protobufBatchLatch.countDown());
	}

	@Bean
	@Qualifier("jsonLatch")
	public CountDownLatch getJsonLatch() {
		return jsonLatch;
	}

	@Bean
	@Qualifier("jsonBatchLatch")
	public CountDownLatch getJsonBatchLatch() {
		return jsonBatchLatch;
	}

	@Bean
	@Qualifier("avroLatch")
	public CountDownLatch getAvroLatch() {
		return avroLatch;
	}

	@Bean
	@Qualifier("avroBatchLatch")
	public CountDownLatch getAvroBatchLatch() {
		return avroBatchLatch;
	}

	@Bean
	@Qualifier("keyvalueLatch")
	public CountDownLatch getKeyvalueLatch() {
		return keyvalueLatch;
	}

	@Bean
	@Qualifier("keyvalueBatchLatch")
	public CountDownLatch getKeyvalueBatchLatch() {
		return keyvalueBatchLatch;
	}

	@Bean
	@Qualifier("protobufLatch")
	public CountDownLatch getProtobufLatch() {
		return protobufLatch;
	}

	@Bean
	@Qualifier("protobufBatchLatch")
	public CountDownLatch getProtobufBatchLatch() {
		return protobufBatchLatch;
	}

}

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.Messages;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.pulsar.annotation.EnablePulsar;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.pulsar.support.PulsarHeaders;

/**
 * Test configurations for {@link PulsarListenerTests}.
 *
 * @author Ali Ustek
 */
@EnablePulsar
@Configuration
class PulsarListerWithHeadersConfig {

	private final CountDownLatch simpleListenerLatch = new CountDownLatch(1);

	private final CountDownLatch pulsarMessageListenerLatch = new CountDownLatch(1);

	private final CountDownLatch springMessagingMessageListenerLatch = new CountDownLatch(1);

	private final CountDownLatch simpleBatchListenerLatch = new CountDownLatch(1);

	private final CountDownLatch pulsarMessageBatchListenerLatch = new CountDownLatch(1);

	private final CountDownLatch springMessagingMessageBatchListenerLatch = new CountDownLatch(1);

	private final CountDownLatch pulsarMessagesBatchListenerLatch = new CountDownLatch(1);

	@PulsarListener(subscriptionName = "simple-listener-with-headers-sub", topics = "simpleListenerWithHeaders")
	void simpleListenerWithHeaders(String data, @Header(PulsarHeaders.MESSAGE_ID) MessageId messageId,
			@Header(PulsarHeaders.TOPIC_NAME) String topicName, @Header(PulsarHeaders.RAW_DATA) byte[] rawData,
			@Header("foo") String foo) {
		PulsarListenerTests.capturedData = data;
		PulsarListenerTests.messageId = messageId;
		PulsarListenerTests.topicName = topicName;
		PulsarListenerTests.fooValue = foo;
		PulsarListenerTests.rawData = rawData;
		simpleListenerLatch.countDown();
	}

	@PulsarListener(subscriptionName = "pulsar-message-listener-with-headers-sub",
			topics = "pulsarMessageListenerWithHeaders")
	void pulsarMessageListenerWithHeaders(Message<String> data, @Header(PulsarHeaders.MESSAGE_ID) MessageId messageId,
			@Header(PulsarHeaders.TOPIC_NAME) String topicName, @Header(PulsarHeaders.RAW_DATA) byte[] rawData,
			@Header("foo") String foo) {
		PulsarListenerTests.capturedData = data.getValue();
		PulsarListenerTests.messageId = messageId;
		PulsarListenerTests.topicName = topicName;
		PulsarListenerTests.fooValue = foo;
		PulsarListenerTests.rawData = rawData;
		pulsarMessageListenerLatch.countDown();
	}

	@PulsarListener(subscriptionName = "pulsar-message-listener-with-headers-sub",
			topics = "springMessagingMessageListenerWithHeaders")
	void springMessagingMessageListenerWithHeaders(org.springframework.messaging.Message<String> data,
			@Header(PulsarHeaders.MESSAGE_ID) MessageId messageId, @Header(PulsarHeaders.RAW_DATA) byte[] rawData,
			@Header(PulsarHeaders.TOPIC_NAME) String topicName, @Header("foo") String foo) {
		PulsarListenerTests.capturedData = data.getPayload();
		PulsarListenerTests.messageId = messageId;
		PulsarListenerTests.topicName = topicName;
		PulsarListenerTests.fooValue = foo;
		PulsarListenerTests.rawData = rawData;
		springMessagingMessageListenerLatch.countDown();
	}

	@PulsarListener(subscriptionName = "simple-batch-listener-with-headers-sub",
			topics = "simpleBatchListenerWithHeaders", batch = true)
	void simpleBatchListenerWithHeaders(List<String> data, @Header(PulsarHeaders.MESSAGE_ID) List<MessageId> messageIds,
			@Header(PulsarHeaders.TOPIC_NAME) List<String> topicNames, @Header("foo") List<String> fooValues) {
		PulsarListenerTests.capturedBatchData = data;
		PulsarListenerTests.batchMessageIds = messageIds;
		PulsarListenerTests.batchTopicNames = topicNames;
		PulsarListenerTests.batchFooValues = fooValues;
		simpleBatchListenerLatch.countDown();
	}

	@PulsarListener(subscriptionName = "pulsarMessage-batch-listener-with-headers-sub",
			topics = "pulsarMessageBatchListenerWithHeaders", batch = true)
	void pulsarMessageBatchListenerWithHeaders(List<Message<String>> data,
			@Header(PulsarHeaders.MESSAGE_ID) List<MessageId> messageIds,
			@Header(PulsarHeaders.TOPIC_NAME) List<String> topicNames, @Header("foo") List<String> fooValues) {

		PulsarListenerTests.capturedBatchData = data.stream().map(Message::getValue).collect(Collectors.toList());

		PulsarListenerTests.batchMessageIds = messageIds;
		PulsarListenerTests.batchTopicNames = topicNames;
		PulsarListenerTests.batchFooValues = fooValues;
		pulsarMessageBatchListenerLatch.countDown();
	}

	@PulsarListener(subscriptionName = "spring-messaging-message-batch-listener-with-headers-sub",
			topics = "springMessagingMessageBatchListenerWithHeaders", batch = true)
	void springMessagingMessageBatchListenerWithHeaders(List<org.springframework.messaging.Message<String>> data,
			@Header(PulsarHeaders.MESSAGE_ID) List<MessageId> messageIds,
			@Header(PulsarHeaders.TOPIC_NAME) List<String> topicNames, @Header("foo") List<String> fooValues) {

		PulsarListenerTests.capturedBatchData = data.stream().map(org.springframework.messaging.Message::getPayload)
				.collect(Collectors.toList());

		PulsarListenerTests.batchMessageIds = messageIds;
		PulsarListenerTests.batchTopicNames = topicNames;
		PulsarListenerTests.batchFooValues = fooValues;
		springMessagingMessageBatchListenerLatch.countDown();
	}

	@PulsarListener(subscriptionName = "pulsarMessages-batch-listener-with-headers-sub",
			topics = "pulsarMessagesBatchListenerWithHeaders", batch = true)
	void pulsarMessagesBatchListenerWithHeaders(Messages<String> data,
			@Header(PulsarHeaders.MESSAGE_ID) List<MessageId> messageIds,
			@Header(PulsarHeaders.TOPIC_NAME) List<String> topicNames, @Header("foo") List<String> fooValues) {
		List<String> list = new ArrayList<>();
		data.iterator().forEachRemaining(m -> list.add(m.getValue()));
		PulsarListenerTests.capturedBatchData = list;
		PulsarListenerTests.batchMessageIds = messageIds;
		PulsarListenerTests.batchTopicNames = topicNames;
		PulsarListenerTests.batchFooValues = fooValues;
		pulsarMessagesBatchListenerLatch.countDown();
	}

	@Bean
	@Qualifier("simpleListenerLatch")
	public CountDownLatch getSimpleListenerLatch() {
		return simpleListenerLatch;
	}

	@Bean
	@Qualifier("pulsarMessageListenerLatch")
	public CountDownLatch getPulsarMessageListenerLatch() {
		return pulsarMessageListenerLatch;
	}

	@Bean
	@Qualifier("springMessagingMessageListenerLatch")
	public CountDownLatch getSpringMessagingMessageListenerLatch() {
		return springMessagingMessageListenerLatch;
	}

	@Bean
	@Qualifier("simpleBatchListenerLatch")
	public CountDownLatch getSimpleBatchListenerLatch() {
		return simpleBatchListenerLatch;
	}

	@Bean
	@Qualifier("pulsarMessageBatchListenerLatch")
	public CountDownLatch getPulsarMessageBatchListenerLatch() {
		return pulsarMessageBatchListenerLatch;
	}

	@Bean
	@Qualifier("springMessagingMessageBatchListenerLatch")
	public CountDownLatch getSpringMessagingMessageBatchListenerLatch() {
		return springMessagingMessageBatchListenerLatch;
	}

	@Bean
	@Qualifier("pulsarMessagesBatchListenerLatch")
	public CountDownLatch getPulsarMessagesBatchListenerLatch() {
		return pulsarMessagesBatchListenerLatch;
	}

}

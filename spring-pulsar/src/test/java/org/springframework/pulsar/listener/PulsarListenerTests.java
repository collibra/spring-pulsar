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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.client.impl.schema.AvroSchema;
import org.apache.pulsar.client.impl.schema.JSONSchema;
import org.apache.pulsar.client.impl.schema.ProtobufSchema;
import org.apache.pulsar.common.schema.KeyValue;
import org.apache.pulsar.common.schema.KeyValueEncodingType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.pulsar.annotation.EnablePulsar;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.pulsar.config.ConcurrentPulsarListenerContainerFactory;
import org.springframework.pulsar.config.PulsarClientConfiguration;
import org.springframework.pulsar.config.PulsarClientFactoryBean;
import org.springframework.pulsar.config.PulsarListenerContainerFactory;
import org.springframework.pulsar.config.PulsarListenerEndpointRegistry;
import org.springframework.pulsar.core.DefaultPulsarConsumerFactory;
import org.springframework.pulsar.core.DefaultPulsarProducerFactory;
import org.springframework.pulsar.core.PulsarAdministration;
import org.springframework.pulsar.core.PulsarConsumerFactory;
import org.springframework.pulsar.core.PulsarProducerFactory;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.pulsar.core.PulsarTestContainerSupport;
import org.springframework.pulsar.core.PulsarTopic;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Soby Chacko
 * @author Alexander Preuß
 */
@SpringJUnitConfig
@DirtiesContext
public class PulsarListenerTests implements PulsarTestContainerSupport {

	static CountDownLatch latch = new CountDownLatch(1);

	static CountDownLatch latch1 = new CountDownLatch(3);

	static CountDownLatch latch2 = new CountDownLatch(3);

	static volatile String capturedData;
	static volatile MessageId messageId;
	static volatile String topicName;
	static volatile String fooValue;
	static volatile byte[] rawData;
	static volatile List<String> capturedBatchData;
	static volatile List<MessageId> batchMessageIds;
	static volatile List<String> batchTopicNames;
	static volatile List<String> batchFooValues;

	@Autowired
	PulsarTemplate<String> pulsarTemplate;

	@Autowired
	private PulsarClient pulsarClient;

	@Configuration(proxyBeanMethods = false)
	@EnablePulsar
	public static class TopLevelConfig {

		@Bean
		public PulsarProducerFactory<String> pulsarProducerFactory(PulsarClient pulsarClient) {
			Map<String, Object> config = new HashMap<>();
			config.put("topicName", "foo-1");
			return new DefaultPulsarProducerFactory<>(pulsarClient, config);
		}

		@Bean
		public PulsarClientFactoryBean pulsarClientFactoryBean(PulsarClientConfiguration pulsarClientConfiguration) {
			return new PulsarClientFactoryBean(pulsarClientConfiguration);
		}

		@Bean
		public PulsarClientConfiguration pulsarClientConfiguration() {
			return new PulsarClientConfiguration(Map.of("serviceUrl", PulsarTestContainerSupport.getPulsarBrokerUrl()));
		}

		@Bean
		public PulsarTemplate<String> pulsarTemplate(PulsarProducerFactory<String> pulsarProducerFactory) {
			return new PulsarTemplate<>(pulsarProducerFactory);
		}

		@Bean
		public PulsarConsumerFactory<?> pulsarConsumerFactory(PulsarClient pulsarClient) {
			Map<String, Object> config = new HashMap<>();
			return new DefaultPulsarConsumerFactory<>(pulsarClient, config);
		}

		@Bean
		PulsarListenerContainerFactory<?> pulsarListenerContainerFactory(
				PulsarConsumerFactory<Object> pulsarConsumerFactory) {
			final ConcurrentPulsarListenerContainerFactory<?> pulsarListenerContainerFactory = new ConcurrentPulsarListenerContainerFactory<>();
			pulsarListenerContainerFactory.setPulsarConsumerFactory(pulsarConsumerFactory);
			return pulsarListenerContainerFactory;
		}

		@Bean
		PulsarAdministration pulsarAdministration() {
			return new PulsarAdministration(
					PulsarAdmin.builder().serviceHttpUrl(PulsarTestContainerSupport.getHttpServiceUrl()));
		}

		@Bean
		PulsarTopic partitionedTopic() {
			return PulsarTopic.builder("persistent://public/default/concurrency-on-pl").numberOfPartitions(3).build();
		}

	}

	@Nested
	@ContextConfiguration(classes = TestPulsarListenersForBasicScenario.class)
	class PulsarListenerBasicTestCases {

		@Test
		void testPulsarListenerWithTopicsPattern(@Autowired PulsarListenerEndpointRegistry registry) throws Exception {
			PulsarMessageListenerContainer baz = registry.getListenerContainer("baz");
			PulsarContainerProperties containerProperties = baz.getContainerProperties();
			assertThat(containerProperties.getTopicsPattern()).isEqualTo("persistent://public/default/pattern.*");

			pulsarTemplate.send("persistent://public/default/pattern-1", "hello baz");
			pulsarTemplate.send("persistent://public/default/pattern-2", "hello baz");
			pulsarTemplate.send("persistent://public/default/pattern-3", "hello baz");

			assertThat(latch2.await(10, TimeUnit.SECONDS)).isTrue();
		}

		@Test
		void testPulsarListenerProvidedConsumerProperties(@Autowired PulsarListenerEndpointRegistry registry)
				throws Exception {

			final PulsarContainerProperties pulsarContainerProperties = registry.getListenerContainer("foo")
					.getContainerProperties();
			final Properties pulsarConsumerProperties = pulsarContainerProperties.getPulsarConsumerProperties();
			assertThat(pulsarConsumerProperties.size()).isEqualTo(2);
			assertThat(pulsarConsumerProperties.get("topicNames")).isEqualTo("foo-1");
			assertThat(pulsarConsumerProperties.get("subscriptionName")).isEqualTo("subscription-1");
			pulsarTemplate.send("hello foo");
			assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
		}

		@Test
		void concurrencyOnPulsarListenerWithFailoverSubscription(@Autowired PulsarListenerEndpointRegistry registry)
				throws Exception {
			PulsarProducerFactory<String> pulsarProducerFactory = new DefaultPulsarProducerFactory<>(pulsarClient,
					Map.of("batchingEnabled", false));
			PulsarTemplate<String> customTemplate = new PulsarTemplate<>(pulsarProducerFactory);

			final ConcurrentPulsarMessageListenerContainer<?> bar = (ConcurrentPulsarMessageListenerContainer<?>) registry
					.getListenerContainer("bar");

			assertThat(bar.getConcurrency()).isEqualTo(3);

			customTemplate.sendAsync("concurrency-on-pl", "hello john doe");
			customTemplate.sendAsync("concurrency-on-pl", "hello alice doe");
			customTemplate.sendAsync("concurrency-on-pl", "hello buzz doe");

			assertThat(latch1.await(10, TimeUnit.SECONDS)).isTrue();
		}

		@Test
		void nonDefaultConcurrencySettingNotAllowedOnExclusiveSubscriptions(
				@Autowired PulsarListenerEndpointRegistry registry) throws Exception {
			PulsarProducerFactory<String> pulsarProducerFactory = new DefaultPulsarProducerFactory<>(pulsarClient,
					Map.of("batchingEnabled", false));
			PulsarTemplate<String> customTemplate = new PulsarTemplate<>(pulsarProducerFactory);

			final ConcurrentPulsarMessageListenerContainer<?> bar = (ConcurrentPulsarMessageListenerContainer<?>) registry
					.getListenerContainer("bar");

			assertThat(bar.getConcurrency()).isEqualTo(3);

			customTemplate.sendAsync("concurrency-on-pl", "hello john doe");
			customTemplate.sendAsync("concurrency-on-pl", "hello alice doe");
			customTemplate.sendAsync("concurrency-on-pl", "hello buzz doe");

			assertThat(latch1.await(10, TimeUnit.SECONDS)).isTrue();
		}

		@Test
		void ackModeAppliedToContainerFromListener(@Autowired PulsarListenerEndpointRegistry registry) {
			final PulsarContainerProperties pulsarContainerProperties = registry.getListenerContainer("ackMode-test-id")
					.getContainerProperties();
			assertThat(pulsarContainerProperties.getAckMode()).isEqualTo(AckMode.RECORD);
		}

	}

	@EnablePulsar
	@Configuration
	static class TestPulsarListenersForBasicScenario {

		@PulsarListener(id = "foo", properties = { "subscriptionName=subscription-1", "topicNames=foo-1" })
		void listen1(String message) {
			latch.countDown();
		}

		@PulsarListener(id = "bar", topics = "concurrency-on-pl", subscriptionName = "subscription-2",
				subscriptionType = SubscriptionType.Failover, concurrency = "3")
		void listen2(String message) {
			latch1.countDown();
		}

		@PulsarListener(id = "baz", topicPattern = "persistent://public/default/pattern.*",
				subscriptionName = "subscription-3",
				properties = { "patternAutoDiscoveryPeriod=5", "subscriptionInitialPosition=Earliest" })
		void listen3(String message) {
			latch2.countDown();
		}

		@PulsarListener(id = "ackMode-test-id", subscriptionName = "ackModeTest-sub", topics = "ackModeTest-topic",
				ackMode = AckMode.RECORD)
		void ackModeTestListener(String message) {
		}

	}

	@Nested
	@ContextConfiguration(classes = NegativeAckRedeliveryConfig.class)
	class NegativeAckRedeliveryBackoffTest {

		@Autowired
		private CountDownLatch nackRedeliveryBackoffLatch;

		@Test
		void pulsarListenerWithNackRedeliveryBackoff(@Autowired PulsarListenerEndpointRegistry registry)
				throws Exception {
			pulsarTemplate.send("withNegRedeliveryBackoff-test-topic", "hello john doe");
			assertThat(nackRedeliveryBackoffLatch.await(15, TimeUnit.SECONDS)).isTrue();
		}

	}

	@Nested
	@ContextConfiguration(classes = AckTimeoutRedeliveryConfig.class)
	class AckTimeoutRedeliveryBackoffTest {

		@Autowired
		private CountDownLatch ackTimeoutRedeliveryBackoffLatch;

		@Test
		@Disabled
		void pulsarListenerWithAckTimeoutRedeliveryBackoff(@Autowired PulsarListenerEndpointRegistry registry)
				throws Exception {
			pulsarTemplate.send("withAckTimeoutRedeliveryBackoff-test-topic", "hello john doe");
			assertThat(ackTimeoutRedeliveryBackoffLatch.await(15, TimeUnit.SECONDS)).isTrue();
		}

	}

	@Nested
	@ContextConfiguration(classes = PulsarConsumerErrorHandlerConfig.class)
	class PulsarConsumerErrorHandlerTest {

		@Autowired
		@Qualifier("pulsarConsumerErrorHandlerLatch")
		private CountDownLatch pulsarConsumerErrorHandlerLatch;

		@Autowired
		@Qualifier("dltLatch")
		private CountDownLatch dltLatch;

		@Test
		void pulsarListenerWithNackRedeliveryBackoff(@Autowired PulsarListenerEndpointRegistry registry)
				throws Exception {
			pulsarTemplate.send("pceht-topic", "hello john doe");
			assertThat(pulsarConsumerErrorHandlerLatch.await(10, TimeUnit.SECONDS)).isTrue();
			assertThat(dltLatch.await(10, TimeUnit.SECONDS)).isTrue();
		}

	}

	@Nested
	@ContextConfiguration(classes = DeadLetterPolicyConfig.class)
	class DeadLetterPolicyTest {

		@Autowired
		@Qualifier("latch")
		private CountDownLatch latch;

		@Autowired
		@Qualifier("dlqLatch")
		private CountDownLatch dlqLatch;

		@Test
		@Disabled
		void pulsarListenerWithDeadLetterPolicy() throws Exception {
			pulsarTemplate.send("dlpt-topic-1", "hello");
			assertThat(dlqLatch.await(10, TimeUnit.SECONDS)).isTrue();
			assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		}

		@Nested
		class NegativeConcurrency {

			@Test
			void exclusiveSubscriptionNotAllowedToHaveMultipleConsumers() {
				assertThatThrownBy(
						() -> new AnnotationConfigApplicationContext(TopLevelConfig.class, ConcurrencyConfig.class))
								.rootCause().isInstanceOf(IllegalStateException.class)
								.hasMessage("concurrency > 1 is not allowed on Exclusive subscription type");
			}

		}

		@Nested
		@ContextConfiguration(classes = SchemaTestConfig.class)
		class SchemaTestCases {

			@Autowired
			@Qualifier("jsonLatch")
			private CountDownLatch jsonLatch;

			@Autowired
			@Qualifier("jsonBatchLatch")
			private CountDownLatch jsonBatchLatch;

			@Autowired
			@Qualifier("avroLatch")
			private CountDownLatch avroLatch;

			@Autowired
			@Qualifier("avroBatchLatch")
			private CountDownLatch avroBatchLatch;

			@Autowired
			@Qualifier("keyvalueLatch")
			private CountDownLatch keyvalueLatch;

			@Autowired
			@Qualifier("keyvalueBatchLatch")
			private CountDownLatch keyvalueBatchLatch;

			@Autowired
			@Qualifier("protobufLatch")
			private CountDownLatch protobufLatch;

			@Autowired
			@Qualifier("protobufBatchLatch")
			private CountDownLatch protobufBatchLatch;

			@Test
			void jsonSchema() throws Exception {
				PulsarProducerFactory<User> pulsarProducerFactory = new DefaultPulsarProducerFactory<>(pulsarClient,
						Collections.emptyMap());
				PulsarTemplate<User> template = new PulsarTemplate<>(pulsarProducerFactory);
				template.setSchema(JSONSchema.of(User.class));

				for (int i = 0; i < 3; i++) {
					template.send("json-topic", new User("Jason", i));
				}
				assertThat(jsonLatch.await(10, TimeUnit.SECONDS)).isTrue();
				assertThat(jsonBatchLatch.await(10, TimeUnit.SECONDS)).isTrue();
			}

			@Test
			void avroSchema() throws Exception {
				PulsarProducerFactory<User> pulsarProducerFactory = new DefaultPulsarProducerFactory<>(pulsarClient,
						Collections.emptyMap());
				PulsarTemplate<User> template = new PulsarTemplate<>(pulsarProducerFactory);
				template.setSchema(AvroSchema.of(User.class));

				for (int i = 0; i < 3; i++) {
					template.send("avro-topic", new User("Avi", i));
				}
				assertThat(avroLatch.await(10, TimeUnit.SECONDS)).isTrue();
				assertThat(avroBatchLatch.await(10, TimeUnit.SECONDS)).isTrue();
			}

			@Test
			void keyvalueSchema() throws Exception {
				PulsarProducerFactory<KeyValue<String, Integer>> pulsarProducerFactory = new DefaultPulsarProducerFactory<>(
						pulsarClient, Collections.emptyMap());
				PulsarTemplate<KeyValue<String, Integer>> template = new PulsarTemplate<>(pulsarProducerFactory);
				Schema<KeyValue<String, Integer>> kvSchema = Schema.KeyValue(Schema.STRING, Schema.INT32,
						KeyValueEncodingType.INLINE);
				template.setSchema(kvSchema);

				for (int i = 0; i < 3; i++) {
					template.send("keyvalue-topic", new KeyValue<>("Kevin", i));
				}
				assertThat(keyvalueLatch.await(10, TimeUnit.SECONDS)).isTrue();
				assertThat(keyvalueBatchLatch.await(10, TimeUnit.SECONDS)).isTrue();
			}

			@Test
			void protobufSchema() throws Exception {
				PulsarProducerFactory<Proto.Person> pulsarProducerFactory = new DefaultPulsarProducerFactory<>(
						pulsarClient, Collections.emptyMap());
				PulsarTemplate<Proto.Person> template = new PulsarTemplate<>(pulsarProducerFactory);
				template.setSchema(ProtobufSchema.of(Proto.Person.class));

				for (int i = 0; i < 3; i++) {
					template.send("protobuf-topic", Proto.Person.newBuilder().setId(i).setName("Paul").build());
				}
				assertThat(protobufLatch.await(10, TimeUnit.SECONDS)).isTrue();
				assertThat(protobufBatchLatch.await(10, TimeUnit.SECONDS)).isTrue();
			}

		}

	}

	static class User {

		private String name;

		private int age;

		User() {

		}

		User(String name, int age) {
			this.name = name;
			this.age = age;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			User user = (User) o;
			return age == user.age && Objects.equals(name, user.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, age);
		}

		@Override
		public String toString() {
			return "User{" + "name='" + name + '\'' + ", age=" + age + '}';
		}

	}

	@Nested
	@ContextConfiguration(classes = PulsarListerWithHeadersConfig.class)
	class PulsarHeadersTest {

		@Autowired
		@Qualifier("simpleListenerLatch")
		private CountDownLatch simpleListenerLatch;

		@Autowired
		@Qualifier("pulsarMessageListenerLatch")
		private CountDownLatch pulsarMessageListenerLatch;

		@Autowired
		@Qualifier("springMessagingMessageListenerLatch")
		private CountDownLatch springMessagingMessageListenerLatch;

		@Autowired
		@Qualifier("simpleBatchListenerLatch")
		private CountDownLatch simpleBatchListenerLatch;

		@Autowired
		@Qualifier("pulsarMessageBatchListenerLatch")
		private CountDownLatch pulsarMessageBatchListenerLatch;

		@Autowired
		@Qualifier("springMessagingMessageBatchListenerLatch")
		private CountDownLatch springMessagingMessageBatchListenerLatch;

		@Autowired
		@Qualifier("pulsarMessagesBatchListenerLatch")
		private CountDownLatch pulsarMessagesBatchListenerLatch;

		@Test
		void simpleListenerWithHeaders() throws Exception {
			final MessageId messageId = pulsarTemplate.newMessage("hello-simple-listener")
					.withMessageCustomizer(
							messageBuilder -> messageBuilder.property("foo", "simpleListenerWithHeaders"))
					.withTopic("simpleListenerWithHeaders").send();
			assertThat(simpleListenerLatch.await(10, TimeUnit.SECONDS)).isTrue();
			assertThat(capturedData).isEqualTo("hello-simple-listener");
			assertThat(PulsarListenerTests.messageId).isEqualTo(messageId);
			assertThat(topicName).isEqualTo("persistent://public/default/simpleListenerWithHeaders");
			assertThat(fooValue).isEqualTo("simpleListenerWithHeaders");
			assertThat(rawData).isEqualTo("hello-simple-listener".getBytes(StandardCharsets.UTF_8));
		}

		@Test
		void pulsarMessageListenerWithHeaders() throws Exception {
			final MessageId messageId = pulsarTemplate.newMessage("hello-pulsar-message-listener")
					.withMessageCustomizer(
							messageBuilder -> messageBuilder.property("foo", "pulsarMessageListenerWithHeaders"))
					.withTopic("pulsarMessageListenerWithHeaders").send();
			assertThat(pulsarMessageListenerLatch.await(10, TimeUnit.SECONDS)).isTrue();
			assertThat(capturedData).isEqualTo("hello-pulsar-message-listener");
			assertThat(PulsarListenerTests.messageId).isEqualTo(messageId);
			assertThat(topicName).isEqualTo("persistent://public/default/pulsarMessageListenerWithHeaders");
			assertThat(fooValue).isEqualTo("pulsarMessageListenerWithHeaders");
			assertThat(rawData).isEqualTo("hello-pulsar-message-listener".getBytes(StandardCharsets.UTF_8));
		}

		@Test
		void springMessagingMessageListenerWithHeaders() throws Exception {
			final MessageId messageId = pulsarTemplate.newMessage("hello-spring-messaging-message-listener")
					.withMessageCustomizer(messageBuilder -> messageBuilder.property("foo",
							"springMessagingMessageListenerWithHeaders"))
					.withTopic("springMessagingMessageListenerWithHeaders").send();
			assertThat(springMessagingMessageListenerLatch.await(10, TimeUnit.SECONDS)).isTrue();
			assertThat(capturedData).isEqualTo("hello-spring-messaging-message-listener");
			assertThat(PulsarListenerTests.messageId).isEqualTo(messageId);
			assertThat(topicName).isEqualTo("persistent://public/default/springMessagingMessageListenerWithHeaders");
			assertThat(fooValue).isEqualTo("springMessagingMessageListenerWithHeaders");
			assertThat(rawData).isEqualTo("hello-spring-messaging-message-listener".getBytes(StandardCharsets.UTF_8));
		}

		@Test
		void simpleBatchListenerWithHeaders() throws Exception {
			final MessageId messageId = pulsarTemplate.newMessage("hello-simple-batch-listener")
					.withMessageCustomizer(
							messageBuilder -> messageBuilder.property("foo", "simpleBatchListenerWithHeaders"))
					.withTopic("simpleBatchListenerWithHeaders").send();
			assertThat(simpleBatchListenerLatch.await(10, TimeUnit.SECONDS)).isTrue();
			assertThat(capturedBatchData).containsExactly("hello-simple-batch-listener");
			assertThat(batchMessageIds).containsExactly(messageId);
			assertThat(batchTopicNames).containsExactly("persistent://public/default/simpleBatchListenerWithHeaders");
			assertThat(batchFooValues).containsExactly("simpleBatchListenerWithHeaders");
		}

		@Test
		void pulsarMessageBatchListenerWithHeaders() throws Exception {
			final MessageId messageId = pulsarTemplate.newMessage("hello-pulsar-message-batch-listener")
					.withMessageCustomizer(
							messageBuilder -> messageBuilder.property("foo", "pulsarMessageBatchListenerWithHeaders"))
					.withTopic("pulsarMessageBatchListenerWithHeaders").send();
			assertThat(pulsarMessageBatchListenerLatch.await(10, TimeUnit.SECONDS)).isTrue();
			assertThat(capturedBatchData).containsExactly("hello-pulsar-message-batch-listener");
			assertThat(batchTopicNames)
					.containsExactly("persistent://public/default/pulsarMessageBatchListenerWithHeaders");
			assertThat(batchFooValues).containsExactly("pulsarMessageBatchListenerWithHeaders");
			assertThat(batchMessageIds).containsExactly(messageId);
		}

		@Test
		void springMessagingMessageBatchListenerWithHeaders() throws Exception {
			final MessageId messageId = pulsarTemplate.newMessage("hello-spring-messaging-message-batch-listener")
					.withMessageCustomizer(messageBuilder -> messageBuilder.property("foo",
							"springMessagingMessageBatchListenerWithHeaders"))
					.withTopic("springMessagingMessageBatchListenerWithHeaders").send();
			assertThat(springMessagingMessageBatchListenerLatch.await(10, TimeUnit.SECONDS)).isTrue();
			assertThat(capturedBatchData).containsExactly("hello-spring-messaging-message-batch-listener");
			assertThat(batchTopicNames)
					.containsExactly("persistent://public/default/springMessagingMessageBatchListenerWithHeaders");
			assertThat(batchFooValues).containsExactly("springMessagingMessageBatchListenerWithHeaders");
			assertThat(batchMessageIds).containsExactly(messageId);
		}

		@Test
		void pulsarMessagesBatchListenerWithHeaders() throws Exception {
			final MessageId messageId = pulsarTemplate.newMessage("hello-pulsar-messages-batch-listener")
					.withMessageCustomizer(
							messageBuilder -> messageBuilder.property("foo", "pulsarMessagesBatchListenerWithHeaders"))
					.withTopic("pulsarMessagesBatchListenerWithHeaders").send();
			assertThat(pulsarMessagesBatchListenerLatch.await(10, TimeUnit.SECONDS)).isTrue();
			assertThat(capturedBatchData).containsExactly("hello-pulsar-messages-batch-listener");
			assertThat(batchTopicNames)
					.containsExactly("persistent://public/default/pulsarMessagesBatchListenerWithHeaders");
			assertThat(batchFooValues).containsExactly("pulsarMessagesBatchListenerWithHeaders");
			assertThat(batchMessageIds).containsExactly(messageId);
		}

	}

}

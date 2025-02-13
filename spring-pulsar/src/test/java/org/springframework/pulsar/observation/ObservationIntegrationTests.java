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

package org.springframework.pulsar.observation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.pulsar.annotation.EnablePulsar;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.pulsar.config.ConcurrentPulsarListenerContainerFactory;
import org.springframework.pulsar.config.PulsarClientConfiguration;
import org.springframework.pulsar.config.PulsarClientFactoryBean;
import org.springframework.pulsar.config.PulsarListenerContainerFactory;
import org.springframework.pulsar.core.DefaultPulsarConsumerFactory;
import org.springframework.pulsar.core.DefaultPulsarProducerFactory;
import org.springframework.pulsar.core.PulsarAdministration;
import org.springframework.pulsar.core.PulsarConsumerFactory;
import org.springframework.pulsar.core.PulsarProducerFactory;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.pulsar.core.PulsarTestContainerSupport;

import io.micrometer.common.KeyValues;
import io.micrometer.core.tck.MeterRegistryAssert;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span.Kind;
import io.micrometer.tracing.exporter.FinishedSpan;
import io.micrometer.tracing.test.SampleTestRunner;
import io.micrometer.tracing.test.simple.SpanAssert;
import io.micrometer.tracing.test.simple.SpansAssert;

/**
 * Integration tests for {@link PulsarTemplateObservation send} and
 * {@link PulsarListenerObservation receive} observations in Spring Pulsar against all
 * supported Tracing runtimes.
 *
 * @author Chris Bono
 * @see SampleTestRunner
 */
public class ObservationIntegrationTests extends SampleTestRunner implements PulsarTestContainerSupport {

	@SuppressWarnings("unchecked")
	@Override
	public SampleTestRunnerConsumer yourCode() {
		// template -> listener -> template -> listener
		return (bb, meterRegistry) -> {
			ObservationRegistry observationRegistry = getObservationRegistry();
			try (AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext()) {
				applicationContext.registerBean(ObservationRegistry.class, () -> observationRegistry);
				applicationContext.register(ObservationIntegrationTestAppConfig.class);
				applicationContext.refresh();
				applicationContext.getBean(PulsarTemplate.class).send("obs1-topic", "hello");
				CountDownLatch latch = applicationContext.getBean(ObservationIntegrationTestAppListeners.class).latch;
				assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
			}

			List<FinishedSpan> finishedSpans = bb.getFinishedSpans();
			SpansAssert.assertThat(finishedSpans).haveSameTraceId().hasSize(4);

			List<FinishedSpan> producerSpans = finishedSpans.stream()
					.filter(span -> span.getKind().equals(Kind.PRODUCER)).collect(Collectors.toList());
			SpanAssert.assertThat(producerSpans.get(0)).hasTag("spring.pulsar.template.name", "pulsarTemplate");
			SpanAssert.assertThat(producerSpans.get(1)).hasTag("spring.pulsar.template.name", "pulsarTemplate");

			List<FinishedSpan> consumerSpans = finishedSpans.stream()
					.filter(span -> span.getKind().equals(Kind.CONSUMER)).collect(Collectors.toList());
			SpanAssert.assertThat(consumerSpans.get(0)).hasTagWithKey("spring.pulsar.listener.id");
			assertThat(consumerSpans.get(0).getTags().get("spring.pulsar.listener.id")).isIn("obs1-id-0", "obs2-id-0");
			SpanAssert.assertThat(consumerSpans.get(1)).hasTagWithKey("spring.pulsar.listener.id");
			assertThat(consumerSpans.get(1).getTags().get("spring.pulsar.listener.id")).isIn("obs1-id-0", "obs2-id-0");
			assertThat(consumerSpans.get(0).getTags().get("spring.pulsar.listener.id"))
					.isNotEqualTo(consumerSpans.get(1).getTags().get("spring.pulsar.listener.id"));

			MeterRegistryAssert.assertThat(getMeterRegistry())
					.hasTimerWithNameAndTags("spring.pulsar.template",
							KeyValues.of("spring.pulsar.template.name", "pulsarTemplate"))
					.hasTimerWithNameAndTags("spring.pulsar.template",
							KeyValues.of("spring.pulsar.template.name", "pulsarTemplate"))
					.hasTimerWithNameAndTags("spring.pulsar.listener",
							KeyValues.of("spring.pulsar.listener.id", "obs1-id-0"))
					.hasTimerWithNameAndTags("spring.pulsar.listener",
							KeyValues.of("spring.pulsar.listener.id", "obs2-id-0"));
		};
	}

	@Configuration(proxyBeanMethods = false)
	@EnablePulsar
	static class ObservationIntegrationTestAppConfig {

		@Bean
		public PulsarProducerFactory<String> pulsarProducerFactory(PulsarClient pulsarClient) {
			return new DefaultPulsarProducerFactory<>(pulsarClient, Collections.emptyMap());
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
			PulsarTemplate<String> template = new PulsarTemplate<>(pulsarProducerFactory);
			template.setObservationEnabled(true);
			return template;
		}

		@Bean
		public PulsarConsumerFactory<?> pulsarConsumerFactory(PulsarClient pulsarClient) {
			return new DefaultPulsarConsumerFactory<>(pulsarClient, Collections.emptyMap());
		}

		@Bean
		PulsarListenerContainerFactory<?> pulsarListenerContainerFactory(
				PulsarConsumerFactory<Object> pulsarConsumerFactory) {
			final ConcurrentPulsarListenerContainerFactory<?> pulsarListenerContainerFactory = new ConcurrentPulsarListenerContainerFactory<>();
			pulsarListenerContainerFactory.setPulsarConsumerFactory(pulsarConsumerFactory);
			pulsarListenerContainerFactory.getContainerProperties().setObservationEnabled(true);
			return pulsarListenerContainerFactory;
		}

		@Bean
		PulsarAdministration pulsarAdministration() {
			return new PulsarAdministration(
					PulsarAdmin.builder().serviceHttpUrl(PulsarTestContainerSupport.getHttpServiceUrl()));
		}

		@Bean
		ObservationIntegrationTestAppListeners observationTestAppListeners(PulsarTemplate<String> pulsarTemplate) {
			return new ObservationIntegrationTestAppListeners(pulsarTemplate);
		}

	}

	static class ObservationIntegrationTestAppListeners {

		private PulsarTemplate<String> template;

		CountDownLatch latch = new CountDownLatch(1);

		ObservationIntegrationTestAppListeners(PulsarTemplate<String> template) {
			this.template = template;
		}

		@PulsarListener(id = "obs1-id", properties = { "subscriptionName=obs1-sub", "topicNames=obs1-topic" })
		void listen1(String message) throws PulsarClientException {
			this.template.send("obs2-topic", message);
		}

		@PulsarListener(id = "obs2-id", properties = { "subscriptionName=obs2-sub", "topicNames=obs2-topic" })
		void listen2(String message) {
			latch.countDown();
		}

	}

}

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

package org.springframework.pulsar.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.concurrent.TimeUnit;

import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.interceptor.ProducerInterceptor;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.pulsar.annotation.EnablePulsar;
import org.springframework.pulsar.annotation.PulsarBootstrapConfiguration;
import org.springframework.pulsar.annotation.PulsarListenerAnnotationBeanPostProcessor;
import org.springframework.pulsar.config.ConcurrentPulsarListenerContainerFactory;
import org.springframework.pulsar.config.PulsarClientConfiguration;
import org.springframework.pulsar.config.PulsarClientFactoryBean;
import org.springframework.pulsar.config.PulsarListenerContainerFactory;
import org.springframework.pulsar.config.PulsarListenerEndpointRegistry;
import org.springframework.pulsar.core.CachingPulsarProducerFactory;
import org.springframework.pulsar.core.DefaultPulsarProducerFactory;
import org.springframework.pulsar.core.PulsarAdministration;
import org.springframework.pulsar.core.PulsarConsumerFactory;
import org.springframework.pulsar.core.PulsarProducerFactory;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.pulsar.listener.DefaultPulsarMessageListenerContainer;

/**
 * Autoconfiguration tests for {@link PulsarAutoConfiguration}.
 *
 * @author Chris Bono
 * @author Alexander Preuß
 * @author Soby Chacko
 */
@SuppressWarnings("unchecked")
class PulsarAutoConfigurationTests {

	private ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(PulsarAutoConfiguration.class));

	@Test
	void autoConfigurationSkippedWhenPulsarTemplateNotOnClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(PulsarTemplate.class))
				.run((context) -> assertThat(context).hasNotFailed().doesNotHaveBean(PulsarAutoConfiguration.class));
	}

	@Test
	void annotationDrivenConfigurationSkippedWhenEnablePulsarAnnotationNotOnClasspath() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(EnablePulsar.class))
				.run((context) -> assertThat(context).hasNotFailed()
						.doesNotHaveBean(PulsarAnnotationDrivenConfiguration.class));
	}

	@Test
	void bootstrapConfigurationSkippedWhenCustomPulsarListenerAnnotationProcessorDefined() {
		this.contextRunner
				.withBean("org.springframework.pulsar.config.internalPulsarListenerAnnotationProcessor", String.class,
						() -> "someFauxBean")
				.run((context) -> assertThat(context).hasNotFailed()
						.doesNotHaveBean(PulsarBootstrapConfiguration.class));
	}

	@Test
	void defaultBeansAreAutoConfigured() {
		this.contextRunner
				.run((context) -> assertThat(context).hasNotFailed().hasSingleBean(PulsarClientConfiguration.class)
						.hasSingleBean(PulsarClientFactoryBean.class).hasSingleBean(PulsarProducerFactory.class)
						.hasSingleBean(PulsarTemplate.class).hasSingleBean(PulsarConsumerFactory.class)
						.hasSingleBean(ConcurrentPulsarListenerContainerFactory.class)
						.hasSingleBean(PulsarListenerAnnotationBeanPostProcessor.class)
						.hasSingleBean(PulsarListenerEndpointRegistry.class).hasSingleBean(PulsarAdministration.class));
	}

	@Test
	void customPulsarClientConfigurationIsRespected() {
		PulsarClientConfiguration clientConfig = new PulsarClientConfiguration(
				new PulsarProperties().buildClientProperties());
		this.contextRunner
				.withBean("customPulsarClientConfiguration", PulsarClientConfiguration.class, () -> clientConfig)
				.run((context) -> assertThat(context).hasNotFailed().getBean(PulsarClientConfiguration.class)
						.isSameAs(clientConfig));
	}

	@Test
	void customPulsarClientFactoryBeanIsRespected() {
		PulsarClientConfiguration clientConfig = new PulsarClientConfiguration(
				new PulsarProperties().buildClientProperties());
		PulsarClientFactoryBean clientFactoryBean = new PulsarClientFactoryBean(clientConfig);
		this.contextRunner
				.withBean("customPulsarClientFactoryBean", PulsarClientFactoryBean.class, () -> clientFactoryBean)
				.run((context) -> assertThat(context)
						.getBean("&customPulsarClientFactoryBean", PulsarClientFactoryBean.class)
						.isSameAs(clientFactoryBean));
	}

	@Test
	void customPulsarProducerFactoryIsRespected() {
		PulsarProducerFactory<String> producerFactory = mock(PulsarProducerFactory.class);
		this.contextRunner.withBean("customPulsarProducerFactory", PulsarProducerFactory.class, () -> producerFactory)
				.run((context) -> assertThat(context).hasNotFailed().getBean(PulsarProducerFactory.class)
						.isSameAs(producerFactory));
	}

	@Test
	void customPulsarTemplateIsRespected() {
		PulsarTemplate<String> template = mock(PulsarTemplate.class);
		this.contextRunner.withBean("customPulsarTemplate", PulsarTemplate.class, () -> template)
				.run((context) -> assertThat(context).hasNotFailed().getBean(PulsarTemplate.class).isSameAs(template));
	}

	@Test
	void customPulsarConsumerFactoryIsRespected() {
		PulsarConsumerFactory<String> consumerFactory = mock(PulsarConsumerFactory.class);
		this.contextRunner.withBean("customPulsarConsumerFactory", PulsarConsumerFactory.class, () -> consumerFactory)
				.run((context) -> assertThat(context).hasNotFailed().getBean(PulsarConsumerFactory.class)
						.isSameAs(consumerFactory));
	}

	@Test
	void pulsarConsumerFactoryWithEnumPropertyValue() {
		this.contextRunner.withPropertyValues("spring.pulsar.consumer.subscriptionInitialPosition=Earliest")
				.run((context -> assertThat(context).hasNotFailed().getBean(PulsarConsumerFactory.class)
						.extracting("consumerConfig").hasFieldOrPropertyWithValue("subscriptionInitialPosition",
								SubscriptionInitialPosition.Earliest)));
	}

	@Test
	void customPulsarListenerContainerFactoryIsRespected() {
		PulsarListenerContainerFactory<DefaultPulsarMessageListenerContainer<String>> listenerContainerFactory = mock(
				PulsarListenerContainerFactory.class);
		this.contextRunner
				.withBean("pulsarListenerContainerFactory", PulsarListenerContainerFactory.class,
						() -> listenerContainerFactory)
				.run((context) -> assertThat(context).hasNotFailed().getBean(PulsarListenerContainerFactory.class)
						.isSameAs(listenerContainerFactory));
	}

	@Test
	void customPulsarListenerAnnotationBeanPostProcessorIsRespected() {
		PulsarListenerAnnotationBeanPostProcessor<String> listenerAnnotationBeanPostProcessor = mock(
				PulsarListenerAnnotationBeanPostProcessor.class);
		this.contextRunner
				.withBean("org.springframework.pulsar.config.internalPulsarListenerAnnotationProcessor",
						PulsarListenerAnnotationBeanPostProcessor.class, () -> listenerAnnotationBeanPostProcessor)
				.run((context) -> assertThat(context).hasNotFailed()
						.getBean(PulsarListenerAnnotationBeanPostProcessor.class)
						.isSameAs(listenerAnnotationBeanPostProcessor));
	}

	@Test
	void customPulsarAdministrationIsRespected() {
		PulsarAdministration pulsarAdministration = mock(PulsarAdministration.class);
		this.contextRunner
				.withBean("customPulsarAdministration", PulsarAdministration.class, () -> pulsarAdministration)
				.run((context) -> assertThat(context).hasNotFailed().getBean(PulsarAdministration.class)
						.isSameAs(pulsarAdministration));
	}

	@Test
	void customProducerInterceptorIsUsedInPulsarTemplate() {
		ProducerInterceptor interceptor = mock(ProducerInterceptor.class);
		this.contextRunner.withBean("customProducerInterceptor", ProducerInterceptor.class, () -> interceptor)
				.run((context -> assertThat(context).hasNotFailed().getBean(PulsarTemplate.class)
						.extracting("interceptors")
						.asInstanceOf(InstanceOfAssertFactories.list(ProducerInterceptor.class))
						.contains(interceptor)));
	}

	@Test
	void customProducerInterceptorsOrderedProperly() {
		this.contextRunner.withUserConfiguration(InterceptorTestConfiguration.class)
				.run((context -> assertThat(context).hasNotFailed().getBean(PulsarTemplate.class)
						.extracting("interceptors")
						.asInstanceOf(InstanceOfAssertFactories.list(ProducerInterceptor.class))
						.containsExactly(InterceptorTestConfiguration.interceptorBar,
								InterceptorTestConfiguration.interceptorFoo)));
	}

	@Test
	void consumerBatchPropertiesAreHonored() {
		contextRunner
				.withPropertyValues("spring.pulsar.listener.max-num-messages=10",
						"spring.pulsar.listener.max-num-bytes=101", "spring.pulsar.listener.batch-timeout-millis=50")
				.run((context -> assertThat(context).hasNotFailed()
						.getBean(ConcurrentPulsarListenerContainerFactory.class).extracting("containerProperties")
						.hasFieldOrPropertyWithValue("maxNumMessages", 10)
						.hasFieldOrPropertyWithValue("maxNumBytes", 101)
						.hasFieldOrPropertyWithValue("batchTimeoutMillis", 50)));
	}

	@Nested
	class ClientAutoConfigurationTests {

		@Test
		void authParamMapConvertedToEncodedParamString() {
			contextRunner.withPropertyValues(
					"spring.pulsar.client.auth-plugin-class-name=org.apache.pulsar.client.impl.auth.AuthenticationBasic",
					"spring.pulsar.client.authentication.userId=username",
					"spring.pulsar.client.authentication.password=topsecret")
					.run((context -> assertThat(context).hasNotFailed().getBean(PulsarClientConfiguration.class)
							.extracting("configs", InstanceOfAssertFactories.map(String.class, Object.class))
							.doesNotContainKey("authParamMap").doesNotContainKey("userId").doesNotContainKey("password")
							.containsEntry("authParams", "{\"password\":\"topsecret\",\"userId\":\"username\"}")));
		}

	}

	@Nested
	class ProducerFactoryAutoConfigurationTests {

		@Test
		void cachingProducerFactoryEnabledByDefault() {
			contextRunner.run((context) -> assertHasProducerFactoryOfType(CachingPulsarProducerFactory.class, context));
		}

		@Test
		void nonCachingProducerFactoryCanBeEnabled() {
			contextRunner.withPropertyValues("spring.pulsar.producer.cache.enabled=false")
					.run((context -> assertHasProducerFactoryOfType(DefaultPulsarProducerFactory.class, context)));
		}

		@Test
		void cachingProducerFactoryCanBeEnabled() {
			contextRunner.withPropertyValues("spring.pulsar.producer.cache.enabled=true")
					.run((context -> assertHasProducerFactoryOfType(CachingPulsarProducerFactory.class, context)));
		}

		@Test
		void cachingProducerFactoryCanBeConfigured() {
			contextRunner
					.withPropertyValues("spring.pulsar.producer.cache.expire-after-access=100s",
							"spring.pulsar.producer.cache.maximum-size=5150",
							"spring.pulsar.producer.cache.initial-capacity=200")
					.run((context -> assertThat(context).hasNotFailed().getBean(PulsarProducerFactory.class)
							.extracting("producerCache").extracting("cache")
							.hasFieldOrPropertyWithValue("maximum", 5150L)
							.hasFieldOrPropertyWithValue("expiresAfterAccessNanos", TimeUnit.SECONDS.toNanos(100))));
		}

		private void assertHasProducerFactoryOfType(Class<?> producerFactoryType,
				AssertableApplicationContext context) {
			assertThat(context).hasNotFailed().hasSingleBean(PulsarProducerFactory.class)
					.getBean(PulsarProducerFactory.class).isExactlyInstanceOf(producerFactoryType);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class InterceptorTestConfiguration {

		static ProducerInterceptor interceptorFoo = mock(ProducerInterceptor.class);
		static ProducerInterceptor interceptorBar = mock(ProducerInterceptor.class);

		@Bean
		@Order(200)
		ProducerInterceptor interceptorFoo() {
			return interceptorFoo;
		}

		@Bean
		@Order(100)
		ProducerInterceptor interceptorBar() {
			return interceptorBar;
		}

	}

}

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

import java.util.concurrent.CountDownLatch;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.pulsar.annotation.EnablePulsar;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.pulsar.core.PulsarTemplate;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Test configurations for {@link PulsarListenerTests}.
 *
 * @author Ali Ustek
 */
@EnablePulsar
@Configuration
class PulsarConsumerErrorHandlerConfig {

	private final CountDownLatch pulsarConsumerErrorHandlerLatch = new CountDownLatch(11);

	private final CountDownLatch dltLatch = new CountDownLatch(1);

	@PulsarListener(id = "pceht-id", subscriptionName = "pceht-subscription", topics = "pceht-topic",
			pulsarConsumerErrorHandler = "pulsarConsumerErrorHandler")
	void listen(String msg) {
		pulsarConsumerErrorHandlerLatch.countDown();
		throw new RuntimeException("fail " + msg);
	}

	@PulsarListener(id = "pceh-dltListener", topics = "pceht-topic-pceht-subscription-DLT")
	void listenDlq(String msg) {
		dltLatch.countDown();
	}

	@Bean
	public PulsarConsumerErrorHandler<String> pulsarConsumerErrorHandler(PulsarTemplate<String> pulsarTemplate) {
		return new DefaultPulsarConsumerErrorHandler<>(new PulsarDeadLetterPublishingRecoverer<>(pulsarTemplate),
				new FixedBackOff(100, 10));
	}

	@Bean
	@Qualifier("pulsarConsumerErrorHandlerLatch")
	public CountDownLatch getPulsarConsumerErrorHandlerLatch() {
		return pulsarConsumerErrorHandlerLatch;
	}

	@Bean
	@Qualifier("dltLatch")
	public CountDownLatch getDltLatch() {
		return dltLatch;
	}

}

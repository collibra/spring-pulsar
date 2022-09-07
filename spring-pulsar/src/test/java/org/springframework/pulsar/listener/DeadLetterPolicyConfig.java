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

import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.apache.pulsar.client.api.SubscriptionType;

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
class DeadLetterPolicyConfig {

	private final CountDownLatch latch = new CountDownLatch(2);

	private final CountDownLatch dlqLatch = new CountDownLatch(1);

	@PulsarListener(id = "deadLetterPolicyListener", subscriptionName = "deadLetterPolicySubscription",
			topics = "dlpt-topic-1", deadLetterPolicy = "deadLetterPolicy", subscriptionType = SubscriptionType.Shared,
			properties = { "ackTimeoutMillis=1" })
	void listen(String msg) {
		latch.countDown();
		throw new RuntimeException("fail " + msg);
	}

	@PulsarListener(id = "dlqListener", topics = "dlpt-dlq-topic")
	void listenDlq(String msg) {
		dlqLatch.countDown();
	}

	@Bean
	DeadLetterPolicy deadLetterPolicy() {
		return DeadLetterPolicy.builder().maxRedeliverCount(1).deadLetterTopic("dlpt-dlq-topic").build();
	}

	@Bean
	@Qualifier("latch")
	public CountDownLatch getLatch() {
		return latch;
	}

	@Bean
	@Qualifier("dlqLatch")
	public CountDownLatch getDlqLatch() {
		return dlqLatch;
	}

}

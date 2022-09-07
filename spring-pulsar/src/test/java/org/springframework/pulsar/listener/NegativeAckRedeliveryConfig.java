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

import org.apache.pulsar.client.api.RedeliveryBackoff;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.client.impl.MultiplierRedeliveryBackoff;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.pulsar.annotation.PulsarListener;

/**
 * Test configurations for {@link PulsarListenerTests}.
 *
 * @author Ali Ustek
 */
@Configuration
class NegativeAckRedeliveryConfig {

	private final CountDownLatch nackRedeliveryBackoffLatch = new CountDownLatch(5);

	@PulsarListener(id = "withNegRedeliveryBackoff", subscriptionName = "withNegRedeliveryBackoffSubscription",
			topics = "withNegRedeliveryBackoff-test-topic", negativeAckRedeliveryBackoff = "redeliveryBackoff",
			subscriptionType = SubscriptionType.Shared)
	void listen(String msg) {
		nackRedeliveryBackoffLatch.countDown();
		throw new RuntimeException("fail " + msg);
	}

	@Bean
	public RedeliveryBackoff redeliveryBackoff() {
		return MultiplierRedeliveryBackoff.builder().minDelayMs(1000).maxDelayMs(5 * 1000).multiplier(2).build();
	}

	@Bean
	public CountDownLatch getNackRedeliveryBackoffLatch() {
		return nackRedeliveryBackoffLatch;
	}

}

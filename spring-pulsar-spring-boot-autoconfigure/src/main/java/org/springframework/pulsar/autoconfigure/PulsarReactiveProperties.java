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

import java.time.Duration;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.apache.pulsar.client.api.CompressionType;
import org.apache.pulsar.client.api.ConsumerCryptoFailureAction;
import org.apache.pulsar.client.api.DeadLetterPolicy;
import org.apache.pulsar.client.api.HashingScheme;
import org.apache.pulsar.client.api.KeySharedMode;
import org.apache.pulsar.client.api.KeySharedPolicy;
import org.apache.pulsar.client.api.MessageRoutingMode;
import org.apache.pulsar.client.api.ProducerAccessMode;
import org.apache.pulsar.client.api.ProducerCryptoFailureAction;
import org.apache.pulsar.client.api.RegexSubscriptionMode;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionType;
import org.apache.pulsar.reactive.client.api.ImmutableReactiveMessageConsumerSpec;
import org.apache.pulsar.reactive.client.api.ImmutableReactiveMessageSenderSpec;
import org.apache.pulsar.reactive.client.api.MutableReactiveMessageConsumerSpec;
import org.apache.pulsar.reactive.client.api.MutableReactiveMessageSenderSpec;
import org.apache.pulsar.reactive.client.api.ReactiveMessageConsumerSpec;
import org.apache.pulsar.reactive.client.api.ReactiveMessageSenderSpec;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.PropertyMapper;

import reactor.core.scheduler.Schedulers;

/**
 * Configuration properties for Spring for the Apache Pulsar reactive client.
 * <p>
 * Users should refer to Pulsar reactive client documentation for complete descriptions of
 * these properties.
 *
 * @author Christophe Bornet
 */
@ConfigurationProperties(prefix = "spring.pulsar.reactive")
public class PulsarReactiveProperties {

	private final Sender sender = new Sender();

	private final Consumer consumer = new Consumer();

	public Sender getSender() {
		return this.sender;
	}

	public Consumer getConsumer() {
		return this.consumer;
	}

	public ReactiveMessageSenderSpec buildReactiveMessageSenderSpec() {
		return this.sender.buildReactiveMessageSenderSpec();
	}

	public ReactiveMessageConsumerSpec buildReactiveMessageConsumerSpec() {
		return this.consumer.buildReactiveMessageConsumerSpec();
	}

	public static class Sender {

		/**
		 * Topic the producer will publish to.
		 */
		private String topicName;

		/**
		 * Name for the producer. If not assigned, a unique name is generated.
		 */
		private String producerName;

		/**
		 * Time before a message has to be acknowledged by the broker.
		 */
		private Duration sendTimeout = Duration.ofSeconds(30);

		/**
		 * Maximum number of pending messages for the producer.
		 */
		private Integer maxPendingMessages = 1000;

		/**
		 * Maximum number of pending messages across all the partitions.
		 */
		private Integer maxPendingMessagesAcrossPartitions = 50000;

		/**
		 * Message routing mode for a partitioned producer.
		 */
		private MessageRoutingMode messageRoutingMode = MessageRoutingMode.RoundRobinPartition;

		/**
		 * Message hashing scheme to choose the partition to which the message is
		 * published.
		 */
		private HashingScheme hashingScheme = HashingScheme.JavaStringHash;

		/**
		 * Action the producer will take in case of encryption failure.
		 */
		private ProducerCryptoFailureAction cryptoFailureAction = ProducerCryptoFailureAction.FAIL;

		/**
		 * Time period within which the messages sent will be batched.
		 */
		private Duration batchingMaxPublishDelay = Duration.ofMillis(1);

		/**
		 * Maximum number of messages to be batched.
		 */
		private Integer batchingMaxMessages = 1000;

		/**
		 * Whether to automatically batch messages.
		 */
		private Boolean batchingEnabled = true;

		/**
		 * Whether to split large-size messages into multiple chunks.
		 */
		private Boolean chunkingEnabled = false;

		/**
		 * Message compression type.
		 */
		private CompressionType compressionType;

		/**
		 * Name of the initial subscription of the topic.
		 */
		private String initialSubscriptionName;

		/**
		 * Type of access to the topic the producer requires.
		 */
		private ProducerAccessMode producerAccessMode = ProducerAccessMode.Shared;

		private final Cache cache = new Cache();

		public String getTopicName() {
			return this.topicName;
		}

		public void setTopicName(String topicName) {
			this.topicName = topicName;
		}

		public String getProducerName() {
			return this.producerName;
		}

		public void setProducerName(String producerName) {
			this.producerName = producerName;
		}

		public Duration getSendTimeout() {
			return this.sendTimeout;
		}

		public void setSendTimeout(Duration sendTimeout) {
			this.sendTimeout = sendTimeout;
		}

		public Integer getMaxPendingMessages() {
			return this.maxPendingMessages;
		}

		public void setMaxPendingMessages(Integer maxPendingMessages) {
			this.maxPendingMessages = maxPendingMessages;
		}

		public Integer getMaxPendingMessagesAcrossPartitions() {
			return this.maxPendingMessagesAcrossPartitions;
		}

		public void setMaxPendingMessagesAcrossPartitions(Integer maxPendingMessagesAcrossPartitions) {
			this.maxPendingMessagesAcrossPartitions = maxPendingMessagesAcrossPartitions;
		}

		public MessageRoutingMode getMessageRoutingMode() {
			return this.messageRoutingMode;
		}

		public void setMessageRoutingMode(MessageRoutingMode messageRoutingMode) {
			this.messageRoutingMode = messageRoutingMode;
		}

		public HashingScheme getHashingScheme() {
			return this.hashingScheme;
		}

		public void setHashingScheme(HashingScheme hashingScheme) {
			this.hashingScheme = hashingScheme;
		}

		public ProducerCryptoFailureAction getCryptoFailureAction() {
			return this.cryptoFailureAction;
		}

		public void setCryptoFailureAction(ProducerCryptoFailureAction cryptoFailureAction) {
			this.cryptoFailureAction = cryptoFailureAction;
		}

		public Duration getBatchingMaxPublishDelay() {
			return this.batchingMaxPublishDelay;
		}

		public void setBatchingMaxPublishDelay(Duration batchingMaxPublishDelay) {
			this.batchingMaxPublishDelay = batchingMaxPublishDelay;
		}

		public Integer getBatchingMaxMessages() {
			return this.batchingMaxMessages;
		}

		public void setBatchingMaxMessages(Integer batchingMaxMessages) {
			this.batchingMaxMessages = batchingMaxMessages;
		}

		public Boolean getBatchingEnabled() {
			return this.batchingEnabled;
		}

		public void setBatchingEnabled(Boolean batchingEnabled) {
			this.batchingEnabled = batchingEnabled;
		}

		public Boolean getChunkingEnabled() {
			return this.chunkingEnabled;
		}

		public void setChunkingEnabled(Boolean chunkingEnabled) {
			this.chunkingEnabled = chunkingEnabled;
		}

		public CompressionType getCompressionType() {
			return this.compressionType;
		}

		public void setCompressionType(CompressionType compressionType) {
			this.compressionType = compressionType;
		}

		public String getInitialSubscriptionName() {
			return this.initialSubscriptionName;
		}

		public void setInitialSubscriptionName(String initialSubscriptionName) {
			this.initialSubscriptionName = initialSubscriptionName;
		}

		public ProducerAccessMode getProducerAccessMode() {
			return this.producerAccessMode;
		}

		public void setProducerAccessMode(ProducerAccessMode producerAccessMode) {
			this.producerAccessMode = producerAccessMode;
		}

		public Cache getCache() {
			return this.cache;
		}

		public ReactiveMessageSenderSpec buildReactiveMessageSenderSpec() {
			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();

			MutableReactiveMessageSenderSpec spec = new MutableReactiveMessageSenderSpec();

			map.from(this::getTopicName).to(spec::setTopicName);
			map.from(this::getProducerName).to(spec::setProducerName);
			map.from(this::getSendTimeout).to(spec::setSendTimeout);
			map.from(this::getMaxPendingMessages).to(spec::setMaxPendingMessages);
			map.from(this::getMaxPendingMessagesAcrossPartitions).to(spec::setMaxPendingMessagesAcrossPartitions);
			map.from(this::getMessageRoutingMode).to(spec::setMessageRoutingMode);
			map.from(this::getHashingScheme).to(spec::setHashingScheme);
			map.from(this::getCryptoFailureAction).to(spec::setCryptoFailureAction);
			map.from(this::getBatchingMaxPublishDelay).to(spec::setBatchingMaxPublishDelay);
			map.from(this::getBatchingMaxMessages).to(spec::setBatchingMaxMessages);
			map.from(this::getBatchingEnabled).to(spec::setBatchingEnabled);
			map.from(this::getChunkingEnabled).to(spec::setChunkingEnabled);
			map.from(this::getCompressionType).to(spec::setCompressionType);
			map.from(this::getInitialSubscriptionName).to(spec::setInitialSubscriptionName);
			map.from(this::getProducerAccessMode).to(spec::setAccessMode);

			return new ImmutableReactiveMessageSenderSpec(spec);
		}

	}

	public static class Consumer {

		/**
		 * Comma-separated list of topics the consumer subscribes to.
		 */
		private String[] topics;

		/**
		 * Pattern for topics the consumer subscribes to.
		 */
		private Pattern topicsPattern;

		/**
		 * Subscription name for the consumer.
		 */
		private String subscriptionName;

		/**
		 * Subscription type to be used when subscribing to a topic.
		 */
		private SubscriptionType subscriptionType = SubscriptionType.Exclusive;

		/*
		 * KeyShared mode of KeyShared subscription.
		 */
		private KeySharedMode keySharedMode;

		/**
		 * Map of properties to add to the subscription.
		 */
		private SortedMap<String, String> subscriptionProperties = new TreeMap<>();

		/**
		 * Number of messages that can be accumulated before the consumer calls "receive".
		 */
		private Integer receiverQueueSize = 1000;

		/**
		 * Time to group acknowledgements before sending them to the broker.
		 */
		private Duration acknowledgementsGroupTime = Duration.ofMillis(100);

		/**
		 * When set to true, ignores the acknowledge operation completion and makes it
		 * asynchronous from the message consuming processing to improve performance by
		 * allowing the acknowledges and message processing to interleave. Defaults to
		 * true.
		 */
		private Boolean acknowledgeAsynchronously = true;

		/**
		 * Type of acknowledge scheduler.
		 */
		private SchedulerType acknowledgeSchedulerType;

		/**
		 * Delay before re-delivering messages that have failed to be processed.
		 */
		private Duration negativeAckRedeliveryDelay = Duration.ofMinutes(1);

		/**
		 * Configuration for the dead letter queue.
		 */
		private DeadLetterPolicy deadLetterPolicy;

		/**
		 * Whether the retry letter topic is enabled.
		 */
		private Boolean retryLetterTopicEnable;

		/**
		 * Maximum number of messages that a consumer can be pushed at once from a broker
		 * across all partitions.
		 */
		private Integer maxTotalReceiverQueueSizeAcrossPartitions = 50000;

		/**
		 * Consumer name to identify a particular consumer from the topic stats.
		 */
		private String consumerName;

		/**
		 * Timeout for unacked messages to be redelivered.
		 */
		private Duration ackTimeout = Duration.ZERO;

		/**
		 * Precision for the ack timeout messages tracker.
		 */
		private Duration ackTimeoutTickTime = Duration.ofSeconds(1);

		/**
		 * Priority level for shared subscription consumers.
		 */
		private Integer priorityLevel = 0;

		/**
		 * Action the consumer will take in case of decryption failure.
		 */
		private ConsumerCryptoFailureAction cryptoFailureAction = ConsumerCryptoFailureAction.FAIL;

		/**
		 * Map of properties to add to the consumer.
		 */
		private SortedMap<String, String> properties = new TreeMap<>();

		/**
		 * Whether to read messages from the compacted topic rather than the full message
		 * backlog.
		 */
		private Boolean readCompacted = false;

		/**
		 * Whether batch index acknowledgement is enabled.
		 */
		private Boolean batchIndexAckEnabled = false;

		/**
		 * Position where to initialize a newly created subscription.
		 */
		private SubscriptionInitialPosition subscriptionInitialPosition = SubscriptionInitialPosition.Latest;

		/**
		 * Auto-discovery period for topics when topic pattern is used.
		 */
		private Duration topicsPatternAutoDiscoveryPeriod = Duration.ofMinutes(1);

		/**
		 * Determines which topics the consumer should be subscribed to when using pattern
		 * subscriptions.
		 */
		private RegexSubscriptionMode topicsPatternSubscriptionMode = RegexSubscriptionMode.PersistentOnly;

		/**
		 * Whether the consumer auto-subscribes for partition increase. This is only for
		 * partitioned consumers.
		 */
		private Boolean autoUpdatePartitions = true;

		private Duration autoUpdatePartitionsInterval;

		/**
		 * Whether to replicate subscription state.
		 */
		private Boolean replicateSubscriptionState = false;

		/**
		 * Whether to automatically drop outstanding un-acked messages if the queue is
		 * full.
		 */
		private Boolean autoAckOldestChunkedMessageOnQueueFull = true;

		/**
		 * Maximum number of chunked messages to be kept in memory.
		 */
		private Integer maxPendingChunkedMessage = 10;

		/**
		 * Time to expire incomplete chunks if the consumer won't be able to receive all
		 * chunks before in milliseconds.
		 */
		private Duration expireTimeOfIncompleteChunkedMessage = Duration.ofMinutes(1);

		public String[] getTopics() {
			return this.topics;
		}

		public void setTopics(String[] topics) {
			this.topics = topics;
		}

		public Pattern getTopicsPattern() {
			return this.topicsPattern;
		}

		public void setTopicsPattern(Pattern topicsPattern) {
			this.topicsPattern = topicsPattern;
		}

		public String getSubscriptionName() {
			return this.subscriptionName;
		}

		public void setSubscriptionName(String subscriptionName) {
			this.subscriptionName = subscriptionName;
		}

		public SubscriptionType getSubscriptionType() {
			return this.subscriptionType;
		}

		public void setSubscriptionType(SubscriptionType subscriptionType) {
			this.subscriptionType = subscriptionType;
		}

		public KeySharedMode getKeySharedMode() {
			return this.keySharedMode;
		}

		public void setKeySharedMode(KeySharedMode keySharedMode) {
			this.keySharedMode = keySharedMode;
		}

		public SortedMap<String, String> getSubscriptionProperties() {
			return this.subscriptionProperties;
		}

		public void setSubscriptionProperties(SortedMap<String, String> subscriptionProperties) {
			this.subscriptionProperties = subscriptionProperties;
		}

		public Integer getReceiverQueueSize() {
			return this.receiverQueueSize;
		}

		public void setReceiverQueueSize(Integer receiverQueueSize) {
			this.receiverQueueSize = receiverQueueSize;
		}

		public Duration getAcknowledgementsGroupTime() {
			return this.acknowledgementsGroupTime;
		}

		public void setAcknowledgementsGroupTime(Duration acknowledgementsGroupTime) {
			this.acknowledgementsGroupTime = acknowledgementsGroupTime;
		}

		public Boolean getAcknowledgeAsynchronously() {
			return this.acknowledgeAsynchronously;
		}

		public void setAcknowledgeAsynchronously(Boolean acknowledgeAsynchronously) {
			this.acknowledgeAsynchronously = acknowledgeAsynchronously;
		}

		public SchedulerType getAcknowledgeSchedulerType() {
			return this.acknowledgeSchedulerType;
		}

		public void setAcknowledgeSchedulerType(SchedulerType acknowledgeSchedulerType) {
			this.acknowledgeSchedulerType = acknowledgeSchedulerType;
		}

		public Duration getNegativeAckRedeliveryDelay() {
			return this.negativeAckRedeliveryDelay;
		}

		public void setNegativeAckRedeliveryDelay(Duration negativeAckRedeliveryDelay) {
			this.negativeAckRedeliveryDelay = negativeAckRedeliveryDelay;
		}

		public DeadLetterPolicy getDeadLetterPolicy() {
			return this.deadLetterPolicy;
		}

		public void setDeadLetterPolicy(DeadLetterPolicy deadLetterPolicy) {
			this.deadLetterPolicy = deadLetterPolicy;
		}

		public Boolean getRetryLetterTopicEnable() {
			return this.retryLetterTopicEnable;
		}

		public void setRetryLetterTopicEnable(Boolean retryLetterTopicEnable) {
			this.retryLetterTopicEnable = retryLetterTopicEnable;
		}

		public Integer getMaxTotalReceiverQueueSizeAcrossPartitions() {
			return this.maxTotalReceiverQueueSizeAcrossPartitions;
		}

		public void setMaxTotalReceiverQueueSizeAcrossPartitions(Integer maxTotalReceiverQueueSizeAcrossPartitions) {
			this.maxTotalReceiverQueueSizeAcrossPartitions = maxTotalReceiverQueueSizeAcrossPartitions;
		}

		public String getConsumerName() {
			return this.consumerName;
		}

		public void setConsumerName(String consumerName) {
			this.consumerName = consumerName;
		}

		public Duration getAckTimeout() {
			return this.ackTimeout;
		}

		public void setAckTimeout(Duration ackTimeout) {
			this.ackTimeout = ackTimeout;
		}

		public Duration getAckTimeoutTickTime() {
			return this.ackTimeoutTickTime;
		}

		public void setAckTimeoutTickTime(Duration ackTimeoutTickTime) {
			this.ackTimeoutTickTime = ackTimeoutTickTime;
		}

		public Integer getPriorityLevel() {
			return this.priorityLevel;
		}

		public void setPriorityLevel(Integer priorityLevel) {
			this.priorityLevel = priorityLevel;
		}

		public ConsumerCryptoFailureAction getCryptoFailureAction() {
			return this.cryptoFailureAction;
		}

		public void setCryptoFailureAction(ConsumerCryptoFailureAction cryptoFailureAction) {
			this.cryptoFailureAction = cryptoFailureAction;
		}

		public SortedMap<String, String> getProperties() {
			return this.properties;
		}

		public void setProperties(SortedMap<String, String> properties) {
			this.properties = properties;
		}

		public Boolean getReadCompacted() {
			return this.readCompacted;
		}

		public void setReadCompacted(Boolean readCompacted) {
			this.readCompacted = readCompacted;
		}

		public Boolean getBatchIndexAckEnabled() {
			return this.batchIndexAckEnabled;
		}

		public void setBatchIndexAckEnabled(Boolean batchIndexAckEnabled) {
			this.batchIndexAckEnabled = batchIndexAckEnabled;
		}

		public SubscriptionInitialPosition getSubscriptionInitialPosition() {
			return this.subscriptionInitialPosition;
		}

		public void setSubscriptionInitialPosition(SubscriptionInitialPosition subscriptionInitialPosition) {
			this.subscriptionInitialPosition = subscriptionInitialPosition;
		}

		public Duration getTopicsPatternAutoDiscoveryPeriod() {
			return this.topicsPatternAutoDiscoveryPeriod;
		}

		public void setTopicsPatternAutoDiscoveryPeriod(Duration topicsPatternAutoDiscoveryPeriod) {
			this.topicsPatternAutoDiscoveryPeriod = topicsPatternAutoDiscoveryPeriod;
		}

		public RegexSubscriptionMode getTopicsPatternSubscriptionMode() {
			return this.topicsPatternSubscriptionMode;
		}

		public void setTopicsPatternSubscriptionMode(RegexSubscriptionMode topicsPatternSubscriptionMode) {
			this.topicsPatternSubscriptionMode = topicsPatternSubscriptionMode;
		}

		public Boolean getAutoUpdatePartitions() {
			return this.autoUpdatePartitions;
		}

		public void setAutoUpdatePartitions(Boolean autoUpdatePartitions) {
			this.autoUpdatePartitions = autoUpdatePartitions;
		}

		public Duration getAutoUpdatePartitionsInterval() {
			return this.autoUpdatePartitionsInterval;
		}

		public void setAutoUpdatePartitionsInterval(Duration autoUpdatePartitionsInterval) {
			this.autoUpdatePartitionsInterval = autoUpdatePartitionsInterval;
		}

		public Boolean getReplicateSubscriptionState() {
			return this.replicateSubscriptionState;
		}

		public void setReplicateSubscriptionState(Boolean replicateSubscriptionState) {
			this.replicateSubscriptionState = replicateSubscriptionState;
		}

		public Boolean getAutoAckOldestChunkedMessageOnQueueFull() {
			return this.autoAckOldestChunkedMessageOnQueueFull;
		}

		public void setAutoAckOldestChunkedMessageOnQueueFull(Boolean autoAckOldestChunkedMessageOnQueueFull) {
			this.autoAckOldestChunkedMessageOnQueueFull = autoAckOldestChunkedMessageOnQueueFull;
		}

		public Integer getMaxPendingChunkedMessage() {
			return this.maxPendingChunkedMessage;
		}

		public void setMaxPendingChunkedMessage(Integer maxPendingChunkedMessage) {
			this.maxPendingChunkedMessage = maxPendingChunkedMessage;
		}

		public Duration getExpireTimeOfIncompleteChunkedMessage() {
			return this.expireTimeOfIncompleteChunkedMessage;
		}

		public void setExpireTimeOfIncompleteChunkedMessage(Duration expireTimeOfIncompleteChunkedMessage) {
			this.expireTimeOfIncompleteChunkedMessage = expireTimeOfIncompleteChunkedMessage;
		}

		public ReactiveMessageConsumerSpec buildReactiveMessageConsumerSpec() {

			MutableReactiveMessageConsumerSpec spec = new MutableReactiveMessageConsumerSpec();

			PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();

			map.from(this::getTopics).as(List::of).to(spec::setTopicNames);
			map.from(this::getTopicsPattern).to(spec::setTopicsPattern);
			map.from(this::getSubscriptionName).to(spec::setSubscriptionName);
			map.from(this::getSubscriptionType).to(spec::setSubscriptionType);
			map.from(this::getKeySharedMode).as((mode) -> switch (mode) {
				case STICKY -> KeySharedPolicy.stickyHashRange();
				case AUTO_SPLIT -> KeySharedPolicy.autoSplitHashRange();
			}).to(spec::setKeySharedPolicy);
			map.from(this::getSubscriptionProperties).to(spec::setSubscriptionProperties);
			map.from(this::getReceiverQueueSize).to(spec::setReceiverQueueSize);
			map.from(this::getAcknowledgementsGroupTime).to(spec::setAcknowledgementsGroupTime);
			map.from(this::getAcknowledgeAsynchronously).to(spec::setAcknowledgeAsynchronously);
			map.from(this::getAcknowledgeSchedulerType).as((scheduler) -> switch (scheduler) {
				case boundedElastic -> Schedulers.boundedElastic();
				case parallel -> Schedulers.parallel();
				case single -> Schedulers.single();
				case immediate -> Schedulers.immediate();
			}).to(spec::setAcknowledgeScheduler);
			map.from(this::getNegativeAckRedeliveryDelay).to(spec::setNegativeAckRedeliveryDelay);
			map.from(this::getDeadLetterPolicy).to(spec::setDeadLetterPolicy);
			map.from(this::getRetryLetterTopicEnable).to(spec::setRetryLetterTopicEnable);
			map.from(this::getMaxTotalReceiverQueueSizeAcrossPartitions)
					.to(spec::setMaxTotalReceiverQueueSizeAcrossPartitions);
			map.from(this::getConsumerName).to(spec::setConsumerName);
			map.from(this::getAckTimeout).to(spec::setAckTimeout);
			map.from(this::getAckTimeoutTickTime).to(spec::setAckTimeoutTickTime);
			map.from(this::getPriorityLevel).to(spec::setPriorityLevel);
			map.from(this::getCryptoFailureAction).to(spec::setCryptoFailureAction);
			map.from(this::getProperties).to(spec::setProperties);
			map.from(this::getReadCompacted).to(spec::setReadCompacted);
			map.from(this::getBatchIndexAckEnabled).to(spec::setBatchIndexAckEnabled);
			map.from(this::getTopicsPatternAutoDiscoveryPeriod).to(spec::setTopicsPatternAutoDiscoveryPeriod);
			map.from(this::getTopicsPatternSubscriptionMode).to(spec::setTopicsPatternSubscriptionMode);
			map.from(this::getAutoUpdatePartitions).to(spec::setAutoUpdatePartitions);
			map.from(this::getAutoUpdatePartitionsInterval).to(spec::setAutoUpdatePartitionsInterval);
			map.from(this::getReplicateSubscriptionState).to(spec::setReplicateSubscriptionState);
			map.from(this::getAutoAckOldestChunkedMessageOnQueueFull)
					.to(spec::setAutoAckOldestChunkedMessageOnQueueFull);
			map.from(this::getMaxPendingChunkedMessage).to(spec::setMaxPendingChunkedMessage);
			map.from(this::getExpireTimeOfIncompleteChunkedMessage).to(spec::setExpireTimeOfIncompleteChunkedMessage);
			return new ImmutableReactiveMessageConsumerSpec(spec);
		}

	}

	public enum SchedulerType {

		/**
		 * Reactor's {@link reactor.core.scheduler.BoundedElasticScheduler}.
		 */
		boundedElastic,
		/**
		 * Reactor's Reactor's {@link reactor.core.scheduler.ParallelScheduler}.
		 */
		parallel,
		/**
		 * Reactor's Reactor's {@link reactor.core.scheduler.SingleScheduler}.
		 */
		single,
		/**
		 * Reactor's {@link reactor.core.scheduler.ImmediateScheduler}.
		 */
		immediate

	}

	public static class Cache {

		/** Time period to expire unused entries in the cache. */
		private Duration expireAfterAccess = Duration.ofMinutes(1);

		/** Maximum size of cache (entries). */
		private Long maximumSize = 1000L;

		/** Initial size of cache. */
		private Integer initialCapacity = 50;

		public Duration getExpireAfterAccess() {
			return this.expireAfterAccess;
		}

		public void setExpireAfterAccess(Duration expireAfterAccess) {
			this.expireAfterAccess = expireAfterAccess;
		}

		public Long getMaximumSize() {
			return this.maximumSize;
		}

		public void setMaximumSize(Long maximumSize) {
			this.maximumSize = maximumSize;
		}

		public Integer getInitialCapacity() {
			return this.initialCapacity;
		}

		public void setInitialCapacity(Integer initialCapacity) {
			this.initialCapacity = initialCapacity;
		}

	}

}
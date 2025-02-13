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

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.logging.LogFactory;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.admin.PulsarAdminBuilder;
import org.apache.pulsar.client.admin.PulsarAdminException;
import org.apache.pulsar.client.api.PulsarClientException;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.log.LogAccessor;
import org.springframework.util.CollectionUtils;

/**
 * An administration class that delegates to {@link PulsarAdmin} to create and manage
 * topics defined in the application context.
 *
 * @author Alexander Preuß
 */
public class PulsarAdministration
		implements ApplicationContextAware, SmartInitializingSingleton, PulsarAdministrationOperations {

	private final LogAccessor logger = new LogAccessor(LogFactory.getLog(this.getClass()));

	private final PulsarAdminBuilder adminBuilder;

	private ApplicationContext applicationContext;

	/**
	 * Construct a {@code PulsarAdministration} instance using the given configuration for
	 * the underlying {@link PulsarAdmin}.
	 * @param adminConfig the {@link PulsarAdmin} configuration
	 */
	public PulsarAdministration(Map<String, Object> adminConfig) {
		this.adminBuilder = PulsarAdmin.builder().loadConf(adminConfig);
	}

	/**
	 * Construct a {@code PulsarAdministration} instance using the given builder for the
	 * underlying {@link PulsarAdmin}.
	 * @param adminBuilder the {@link PulsarAdminBuilder}
	 */
	public PulsarAdministration(PulsarAdminBuilder adminBuilder) {
		this.adminBuilder = adminBuilder;
	}

	@Override
	public void afterSingletonsInstantiated() {
		initialize();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	private void initialize() {
		Collection<PulsarTopic> topics = this.applicationContext.getBeansOfType(PulsarTopic.class, false, false)
				.values();
		createOrModifyTopicsIfNeeded(topics);
	}

	private PulsarAdmin createAdminClient() throws PulsarClientException {
		return this.adminBuilder.build();
	}

	@Override
	public void createOrModifyTopics(PulsarTopic... topics) {
		createOrModifyTopicsIfNeeded(Arrays.asList(topics));
	}

	private Map<String, List<PulsarTopic>> getTopicsPerNamespace(Collection<PulsarTopic> topics) {
		return topics.stream().collect(Collectors.groupingBy(this::getTopicNamespaceIdentifier));
	}

	private String getTopicNamespaceIdentifier(PulsarTopic topic) {
		return topic.getComponents().getTenant() + "/" + topic.getComponents().getNamespace();
	}

	private List<String> getMatchingTopicPartitions(PulsarTopic topic, List<String> existingTopics) {
		return existingTopics.stream()
				.filter(existing -> existing.startsWith(topic.getFullyQualifiedTopicName() + "-partition-"))
				.collect(Collectors.toList());
	}

	private void createOrModifyTopicsIfNeeded(Collection<PulsarTopic> topics) {
		if (CollectionUtils.isEmpty(topics)) {
			return;
		}

		try (PulsarAdmin admin = createAdminClient()) {
			doCreateOrModifyTopicsIfNeeded(admin, topics);
		}
		catch (PulsarClientException e) {
			throw new IllegalStateException("Could not create PulsarAdmin", e);
		}
	}

	private void doCreateOrModifyTopicsIfNeeded(PulsarAdmin admin, Collection<PulsarTopic> topics) {
		Map<String, List<PulsarTopic>> topicsPerNamespace = getTopicsPerNamespace(topics);

		Set<PulsarTopic> topicsToCreate = new HashSet<>();
		Set<PulsarTopic> topicsToModify = new HashSet<>();

		topicsPerNamespace.forEach((namespace, requestedTopics) -> {
			try {
				List<String> existingTopicsInNamespace = admin.topics().getList(namespace);

				for (PulsarTopic topic : requestedTopics) {
					if (topic.isPartitioned()) {
						List<String> matchingPartitions = getMatchingTopicPartitions(topic, existingTopicsInNamespace);
						if (matchingPartitions.isEmpty()) {
							this.logger.debug(() -> "Topic " + topic.getFullyQualifiedTopicName() + " does not exist.");
							topicsToCreate.add(topic);
						}
						else {
							int numberOfExistingPartitions = matchingPartitions.size();
							if (numberOfExistingPartitions < topic.getNumberOfPartitions()) {
								this.logger.debug(() -> "Topic " + topic.getFullyQualifiedTopicName() + " found with "
										+ numberOfExistingPartitions + " partitions.");
								topicsToModify.add(topic);
							}
							else if (numberOfExistingPartitions > topic.getNumberOfPartitions()) {
								throw new IllegalStateException("Topic " + topic.getFullyQualifiedTopicName()
										+ " found with " + numberOfExistingPartitions
										+ " partitions. Needs to be deleted first.");
							}
						}
					}
					else {
						if (!existingTopicsInNamespace.contains(topic.getFullyQualifiedTopicName())) {
							this.logger.debug(() -> "Topic " + topic.getFullyQualifiedTopicName() + " does not exist.");
							topicsToCreate.add(topic);
						}
					}
				}

				createTopics(admin, topicsToCreate);
				modifyTopics(admin, topicsToModify);
			}
			catch (PulsarAdminException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private void createTopics(PulsarAdmin admin, Set<PulsarTopic> topicsToCreate) throws PulsarAdminException {
		this.logger.debug(() -> "Creating topics: " + topicsToCreate.stream()
				.map(PulsarTopic::getFullyQualifiedTopicName).collect(Collectors.joining(",")));
		for (PulsarTopic topic : topicsToCreate) {
			if (topic.isPartitioned()) {
				admin.topics().createPartitionedTopic(topic.getTopicName(), topic.getNumberOfPartitions());
			}
			else {
				admin.topics().createNonPartitionedTopic(topic.getTopicName());
			}
		}
	}

	private void modifyTopics(PulsarAdmin admin, Set<PulsarTopic> topicsToModify) throws PulsarAdminException {
		this.logger.debug(() -> "Modifying topics: " + topicsToModify.stream()
				.map(PulsarTopic::getFullyQualifiedTopicName).collect(Collectors.joining(",")));
		for (PulsarTopic topic : topicsToModify) {
			admin.topics().updatePartitionedTopic(topic.getTopicName(), topic.getNumberOfPartitions());
		}
	}

}

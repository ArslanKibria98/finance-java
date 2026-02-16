package com.ksa.islamic.messaging.kafka.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Consumer rebalance listener for logging partition assignments and revocations
 */
@Slf4j
public class LoggingConsumerRebalanceListener implements ConsumerRebalanceListener {

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        if (partitions.isEmpty()) {
            log.info("No partitions revoked");
        } else {
            String partitionInfo = partitions.stream()
                .map(tp -> tp.topic() + "-" + tp.partition())
                .collect(Collectors.joining(", "));

            log.warn("Partitions revoked: [{}]. Total count: {}",
                    partitionInfo, partitions.size());
        }
    }

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        if (partitions.isEmpty()) {
            log.info("No partitions assigned");
        } else {
            String partitionInfo = partitions.stream()
                .map(tp -> tp.topic() + "-" + tp.partition())
                .collect(Collectors.joining(", "));

            log.info("Partitions assigned: [{}]. Total count: {}",
                    partitionInfo, partitions.size());
        }
    }

    @Override
    public void onPartitionsLost(Collection<TopicPartition> partitions) {
        if (!partitions.isEmpty()) {
            String partitionInfo = partitions.stream()
                .map(tp -> tp.topic() + "-" + tp.partition())
                .collect(Collectors.joining(", "));

            log.error("Partitions lost: [{}]. Total count: {}. " +
                     "This may result in duplicate processing.",
                     partitionInfo, partitions.size());
        }
    }
}
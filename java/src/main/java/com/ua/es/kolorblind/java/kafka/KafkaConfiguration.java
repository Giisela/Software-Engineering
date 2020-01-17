package com.ua.es.kolorblind.java.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
//import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaConfiguration {

  //@Value("${kafka.bootstrap-servers}")
  //private String bootstrapServers = "127.0.0.1";

  @Bean
  public Map<String, Object> producerConfigs() {
    Map<String, Object> props = new HashMap<>();
    // list of host:port pairs used for establishing the initial connections to the
    // Kakfa cluster
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.160.80:39092");
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
 

    return props;
  }

  @Bean
  public ProducerFactory<String, KafkaMessage> producerFactory() {
    return new DefaultKafkaProducerFactory<>(producerConfigs());
  }

  @Bean
  public KafkaTemplate<String, KafkaMessage> kafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
  }

  @Bean
    public Map<String, Object> consumerConfigs() {
      Map<String, Object> props = new HashMap<>();
      // list of host:port pairs used for establishing the initial connections to the Kafka cluster
      props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "192.168.160.80:39092");
      props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
      props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
      props.put(ConsumerConfig.GROUP_ID_CONFIG, "json");


      return props;
    }

    @Bean
    public ConsumerFactory<String, KafkaMessage> consumerFactory() {
      return new DefaultKafkaConsumerFactory<>(consumerConfigs(), new StringDeserializer(), new  JsonDeserializer<>(KafkaMessage.class));
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<String, KafkaMessage>> kafkaListenerContainerFactory() {
      ConcurrentKafkaListenerContainerFactory<String, KafkaMessage> factory =
          new ConcurrentKafkaListenerContainerFactory<>();
      factory.setConsumerFactory(consumerFactory());

      return factory;
    }
}
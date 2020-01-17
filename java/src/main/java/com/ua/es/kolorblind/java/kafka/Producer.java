package com.ua.es.kolorblind.java.kafka;


import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Service
public class Producer {

    @Autowired
    private KafkaTemplate<String, KafkaMessage> kafkaTemplate;
    
    private static final Logger logger = LoggerFactory.getLogger(Producer.class);
    private static final String TOPIC = "users";

    public void sendMessage(String fileType, String fileName, String id) throws IOException {
        logger.info(String.format("#### -> Producing message -> %s", fileType));
            
        this.kafkaTemplate.send(TOPIC, new KafkaMessage(fileType, fileName, id));
    }
}
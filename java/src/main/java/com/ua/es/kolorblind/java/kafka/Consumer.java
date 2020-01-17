package com.ua.es.kolorblind.java.kafka;

import com.ua.es.kolorblind.java.Repository.ImageRepository;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;

import org.springframework.beans.factory.annotation.Autowired;

import com.ua.es.kolorblind.java.Entity.Image;
import com.ua.es.kolorblind.java.Entity.MediaStatus;
import com.ua.es.kolorblind.java.Entity.Video;
import com.ua.es.kolorblind.java.Repository.VideoRepository;

@Service
public class Consumer {

    
    private final Logger logger = LoggerFactory.getLogger(Consumer.class);
    
    @Autowired
    ImageRepository imageRepository;
    @Autowired
    VideoRepository videoRepository;
    
    @KafkaListener(topics = "outroTopico", groupId = "group_id")
    public void consume(@Payload KafkaMessage msg, @Headers MessageHeaders headers) throws IOException {
        System.out.println("Antes do if");
        System.out.println("MSG: "+msg);

        if(msg.getFileType().equals("image"))
        {           
            System.out.println("Dentro do if");

            Image img = imageRepository.findById(msg.getId()).get();
            System.out.println("Depois do img");

            img.setProcessTime(msg.getProcessTime());
            System.out.println("Depois do process");

            img.setStatus(MediaStatus.READY);
            System.out.println("Depois do status");

            imageRepository.save(img);
             System.out.println("Depois do save");

            System.out.println(img);
            System.out.println("fim if");

        }
        
        else
        {
            System.out.println("Dentro do else");

            Video video = videoRepository.findById(msg.getId()).get();
            System.out.println("Depois do video");

            video.setProcessTime(msg.getProcessTime());
            System.out.println("Depois do processVideo");

            video.setStatus(MediaStatus.READY);
            System.out.println("Depois do statusVideo");

            videoRepository.save(video);
            
            System.out.println("Depois do save Video");

            System.out.println(video);
            
            System.out.println("FIM ELSE");

        
        }
        
        System.out.println("fim programa");


    }
}
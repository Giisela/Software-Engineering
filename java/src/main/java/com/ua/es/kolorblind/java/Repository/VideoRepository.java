package com.ua.es.kolorblind.java.Repository;

import com.ua.es.kolorblind.java.Entity.Video;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface VideoRepository extends MongoRepository<Video, String> {
}

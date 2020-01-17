package com.ua.es.kolorblind.java.Repository;

import com.ua.es.kolorblind.java.Entity.Image;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ImageRepository extends MongoRepository<Image,String> {
}

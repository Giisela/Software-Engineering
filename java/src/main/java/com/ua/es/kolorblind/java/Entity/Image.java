package com.ua.es.kolorblind.java.Entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document
public class Image extends MediaFile{


    @Id
    private String id;

    public Image(){
        super();
    }

    public String getId() {
        return id;
    }
}


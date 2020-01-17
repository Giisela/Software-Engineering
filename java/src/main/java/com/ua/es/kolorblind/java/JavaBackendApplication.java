package com.ua.es.kolorblind.java;

import com.ua.es.kolorblind.java.Controller.ImageController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;

@SpringBootApplication
@RestController
public class JavaBackendApplication {

	public static void main(String[] args) {

		new File(ImageController.uploadDirectory).mkdir();
		new File(ImageController.DownloadDirectory).mkdir();
		SpringApplication.run(JavaBackendApplication.class, args);
	}
}

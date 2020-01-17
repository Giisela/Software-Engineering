package com.ua.es.kolorblind.java.Controller;

import com.ua.es.kolorblind.java.Entity.Image;
import com.ua.es.kolorblind.java.Entity.MediaStatus;
import com.ua.es.kolorblind.java.Repository.ImageRepository;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.google.common.io.ByteStreams;
import com.ua.es.kolorblind.java.kafka.KafkaMessage;
import com.ua.es.kolorblind.java.kafka.Producer;

@RestController
@RequestMapping("/api/image")
public class ImageController {

    public static String uploadDirectory = System.getProperty("user.dir")+"/uploads";

    public static String DownloadDirectory = System.getProperty("user.dir")+"/ready2Download";

    private final Logger LOG = LogManager.getLogger(ImageController.class);

    @Autowired
    ImageRepository imageRepository;
    @Autowired
    Producer producer;

    @PostMapping("/upload")
    public ResponseEntity uploadImage(@RequestParam("file") MultipartFile file) {
        
        LOG.info("p3g2 - POST REQUEST /api/image/upload");

        String fileType = file.getContentType().split("/")[0];

        if (!fileType.equals("image")) {
            return new ResponseEntity<String>("Must be an Image Format", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }


        Image imageDb = new Image();

        try {

            imageDb.setFileSize((double) file.getSize());
            imageRepository.save(imageDb);
            imageDb.setProcessTime(0.0);
            imageDb.setFullName(imageDb.getId() + "." + file.getOriginalFilename().split("\\.")[1]);

            imageRepository.save(imageDb);
            Path fileNameAndPath = Paths.get(uploadDirectory, imageDb.getFullName());
            Files.write(fileNameAndPath, file.getBytes());
           
            producer.sendMessage("image", imageDb.getFullName(), imageDb.getId());

        } catch (IOException e) {
            imageRepository.delete(imageDb);
            LOG.error("p3g2", "ERROR - /api/image/upload");
            return new ResponseEntity<String>("Server Error", HttpStatus.INSUFFICIENT_STORAGE);
        }


        return new ResponseEntity<Image>(imageDb, HttpStatus.OK);
    }

    @PostMapping("/uploadPython")
    public ResponseEntity uploadPythonImage(@RequestParam("file") MultipartFile file, @RequestParam("id") String id) {

        String fileType = file.getContentType().split("/")[0];

        if (!fileType.equals("image")) {
            return new ResponseEntity<String>("Must be an Image Format", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        Image imageDb;

        try {
            imageDb = imageRepository.findById(id).get();
        } catch (NoSuchElementException e) {
            return new ResponseEntity<String>("Image with id " + id + " not found", HttpStatus.NOT_FOUND);
        }

        try {
            Path fileNameAndPath = Paths.get(DownloadDirectory, imageDb.getFullName());
            Files.write(fileNameAndPath, file.getBytes());

        } catch (IOException e) {
            imageRepository.delete(imageDb);
            return new ResponseEntity<String>("Server Error", HttpStatus.INSUFFICIENT_STORAGE);
        }

        imageDb.setStatus(MediaStatus.READY);

        imageRepository.save(imageDb);


        return new ResponseEntity<Image>(imageDb, HttpStatus.OK);
    }

    @GetMapping("/info")
    public ResponseEntity ImageState(@RequestParam("id") String id) {
        Image imageDocument;

        try {
            imageDocument = imageRepository.findById(id).get();
        } catch (NoSuchElementException e) {
            return new ResponseEntity<String>("Image with id " + id + " not found", HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<Image>(imageDocument, HttpStatus.ACCEPTED);
    }

    @GetMapping("/infoAll")
    public ResponseEntity ImageAllState() {
        List<Image> imageDocumentList;

        try {
            imageDocumentList = imageRepository.findAll();
        } catch (NoSuchElementException e) {
            return new ResponseEntity<String>("No images found", HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<List<Image>>(imageDocumentList, HttpStatus.ACCEPTED);
    }

    @PostMapping("/infoList")
    public ResponseEntity ImageListState(@RequestBody Map<String, String[]> json) {
        ArrayList<Image> imageDocumentList = new ArrayList<>();
        String[] list = json.get("list");

        try {
            for(String id : list) {
                Image img = imageRepository.findById(id).get();
                System.out.println(img.getStatus());
                imageDocumentList.add(img);
            }
        } catch (NoSuchElementException e) {
            return new ResponseEntity<String>("No images found", HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<List<Image>>(imageDocumentList, HttpStatus.ACCEPTED);
    }

    @GetMapping("/download")
    public ResponseEntity ImageDownload(@RequestParam("id") String id){

        Image imageDocument;

        try {
            imageDocument = imageRepository.findById(id).get();
        } catch (NoSuchElementException e) {
            return new ResponseEntity<String>("Image with id " + id + " not found", HttpStatus.NOT_FOUND);
        }

        if(imageDocument.getStatus() != MediaStatus.READY){
            return new ResponseEntity<String>("Image with id " + id + " is not ready", HttpStatus.NOT_FOUND);
        }

        Path fileNameAndPath = Paths.get(DownloadDirectory, imageDocument.getFullName());
    
        
        byte[] image = null;

		try {
			image = Files.readAllBytes(fileNameAndPath);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.setContentLength(image.length);

        return new ResponseEntity<byte[]>(image, headers, HttpStatus.OK);
    }

    @GetMapping("/downloadPython")
    public ResponseEntity ImagePythonDownload(@RequestParam("id") String id){

        Image imageDocument;

        try {
            imageDocument = imageRepository.findById(id).get();
        } catch (NoSuchElementException e) {
            return new ResponseEntity<String>("Image with id " + id + " not found", HttpStatus.NOT_FOUND);
        }

        if(imageDocument.getStatus() != MediaStatus.RECEIVE){
            return new ResponseEntity<String>("Image with id " + id + " is not ready", HttpStatus.NOT_FOUND);
        }

        Path fileNameAndPath = Paths.get(uploadDirectory, imageDocument.getFullName());

        byte[] image = null;

		try {
			image = Files.readAllBytes(fileNameAndPath);
		} catch (IOException e) {
			e.printStackTrace();
		}


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        headers.setContentLength(image.length);

        return new ResponseEntity<byte[]>(image, headers, HttpStatus.OK);
    }

}




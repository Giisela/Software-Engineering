package com.ua.es.kolorblind.java.Controller;

import com.ua.es.kolorblind.java.Entity.Image;
import com.ua.es.kolorblind.java.Entity.MediaStatus;
import com.ua.es.kolorblind.java.Entity.Video;
import com.ua.es.kolorblind.java.Repository.ImageRepository;
import com.ua.es.kolorblind.java.Repository.VideoRepository;
import com.ua.es.kolorblind.java.kafka.Producer;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.MediaTypeNotSupportedStatusException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    public static String uploadDirectory = System.getProperty("user.dir")+"/uploads";

    public static String DownloadDirectory = System.getProperty("user.dir")+"/ready2Download";

    @Autowired
    VideoRepository videoRepository;
    @Autowired
    Producer producer;

    @PostMapping("/upload")
    public ResponseEntity uploadVideo(@RequestParam("file") MultipartFile file) {

        String fileType = file.getContentType().split("/")[0];

        System.out.println(file.getContentType());
        if (!fileType.equals("video")){
            return new ResponseEntity<String>("Must be a Video Format", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        Video videoDb = new Video();

        try {

            videoDb.setFileSize((double) file.getSize());

            videoRepository.save(videoDb);

            videoDb.setFullName(videoDb.getId() + "." +file.getOriginalFilename().split("\\.")[1]);

            videoDb.setProcessTime(0.0);

            videoRepository.save(videoDb);

            Path fileNameAndPath = Paths.get(uploadDirectory, videoDb.getFullName());

            Files.write(fileNameAndPath, file.getBytes());
            
            System.out.println("Producer is : " +producer);
            System.out.println("File is : " +fileNameAndPath);
            producer.sendMessage("video",videoDb.getFullName(), videoDb.getId());
            

        } catch (IOException e) {
            videoRepository.delete(videoDb);
            return new ResponseEntity<String>("Server Error", HttpStatus.INSUFFICIENT_STORAGE);

        }


        return new ResponseEntity<Video>(videoDb, HttpStatus.OK);
    }

    @PostMapping("/pythonUpload")
    public ResponseEntity uploadPythonVideo(@RequestParam("file") MultipartFile file, @RequestParam("id") String id) {

        String fileType = file.getContentType().split("/")[0];

        Video videoDb;

        try {
            videoDb = videoRepository.findById(id).get();
        } catch (NoSuchElementException e) {
            return new ResponseEntity<String>("Video with id " + id + " not found", HttpStatus.NOT_FOUND);
        }

        System.out.println(file.getContentType());
        if (!fileType.equals("video")){
            return new ResponseEntity<String>("Must be a Video Format", HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }

        try {

            videoDb.setStatus(MediaStatus.READY);
            videoRepository.save(videoDb);

            Path fileNameAndPath = Paths.get(DownloadDirectory, videoDb.getFullName());

            Files.write(fileNameAndPath, file.getBytes());

        } catch (IOException e) {
            videoRepository.delete(videoDb);
            return new ResponseEntity<String>("Server Error", HttpStatus.INSUFFICIENT_STORAGE);

        }


        return new ResponseEntity<Video>(videoDb, HttpStatus.OK);
    }

    @GetMapping("/info")
    public ResponseEntity VideoState(@RequestParam("id") String id){
        Video videoDocument;

        try{
            videoDocument = videoRepository.findById(id).get();
        }catch (NoSuchElementException e){
            return new ResponseEntity<String>("Video with id " + id + " not found", HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<Video>(videoDocument, HttpStatus.ACCEPTED);
    }

    @GetMapping("/infoAll")
    public ResponseEntity VideoAllState() {
        List<Video> videoDocumentList;

        try {
            videoDocumentList = videoRepository.findAll();
        } catch (NoSuchElementException e) {
            return new ResponseEntity<String>("No videos found", HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<List<Video>>(videoDocumentList, HttpStatus.ACCEPTED);
    }

    @PostMapping("/infoList")
    public ResponseEntity ImageListState(@RequestBody Map<String, String[]> json) {
        ArrayList<Video> videoDocumentList = new ArrayList<>();
        String[] list = json.get("list");

        try {
            for(String id : list) {
                Video img = videoRepository.findById(id).get();
                System.out.println(img.getStatus());
                videoDocumentList.add(img);
            }
        } catch (NoSuchElementException e) {
            return new ResponseEntity<String>("No videos found", HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<List<Video>>(videoDocumentList, HttpStatus.ACCEPTED);
    }

    @GetMapping("/download")
    public ResponseEntity VideoDownload(@RequestParam("id") String id) {

        Video videoDocument;

        try {
            videoDocument = videoRepository.findById(id).get();
        } catch (NoSuchElementException e) {
            return new ResponseEntity<String>("Video with id " + id + " not found", HttpStatus.NOT_FOUND);
        }

        if (videoDocument.getStatus() != MediaStatus.READY) {
            return new ResponseEntity<String>("Video with id " + id + " not ready", HttpStatus.NOT_FOUND);
        }
        
        Path fileNameAndPath = Paths.get(DownloadDirectory, videoDocument.getFullName());

        byte[] video = null;

		try {
			video = Files.readAllBytes(fileNameAndPath);
		} catch (IOException e) {
			e.printStackTrace();
		}

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(video.length);

        return new ResponseEntity<byte[]>(video, headers, HttpStatus.OK);
    }

    @GetMapping("/pythonDownload")
    public ResponseEntity VideoPythonDownload(@RequestParam("id") String id) {

        Video videoDocument;

        try {
            videoDocument = videoRepository.findById(id).get();
        } catch (NoSuchElementException e) {
            return new ResponseEntity<String>("Video with id " + id + " not found", HttpStatus.NOT_FOUND);
        }
        
        videoDocument.setStatus(MediaStatus.PROCESSING);
        
        videoRepository.save(videoDocument);
        
        Path fileNameAndPath = Paths.get(uploadDirectory, videoDocument.getFullName());

        byte[] video = null;

        try {
			video = Files.readAllBytes(fileNameAndPath);
		} catch (IOException e) {
			e.printStackTrace();
		}

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentLength(video.length);

        return new ResponseEntity<byte[]>(video, headers, HttpStatus.OK);
    }
}




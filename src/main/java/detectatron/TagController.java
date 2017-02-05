package detectatron;

import com.amazonaws.services.rekognition.model.InvalidImageFormatException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;
import java.util.logging.Level;


import org.springframework.web.multipart.MultipartFile;

/**
 * Accepts an image via POST and returns a JSON object with scoring information.
 */
@RestController
@EnableAutoConfiguration

public class TagController {

    private static final Logger logger = Logger.getLogger("TagController");

    @Autowired
    VideoTagService myVideoTagService;

    @Autowired
    ImageTagService myImageTagService;

    @Autowired
    ArmingService myArmingService;



    @RequestMapping(value = "/tag", method = RequestMethod.GET)
    public String tag() {
        return "This endpoint must be invoked via POST with an image file\n";
    }

    @RequestMapping(value = "/tag/image", method = RequestMethod.POST)
    public ResponseEntity<String> tagImage(
            @RequestParam("file") MultipartFile imageFile
        ) {

        logger.log(Level.INFO, "Received binary image for processing");

        if (myArmingService.armed == false) {
            logger.log(Level.INFO, "Discarding request, Detectatron is currently disarmed.");
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("Detectatron is disarmed, not accepting images for tagging");
        }

        // Convert the image from MultiPart form to actual binary data (and make sure we actually got a damn image).
        byte[] imageBinary;

        try {
            logger.log(Level.INFO, "Processing file: " + imageFile.getOriginalFilename());
            imageBinary = imageFile.getBytes();
        } catch (java.io.IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("A binary image must be POSTed to this endpoint.\n");
        }


        // Process result
        try {

            ObjectMapper objectMapper = new ObjectMapper();
            String tagsAsJSON = objectMapper.writeValueAsString(myImageTagService.process(imageBinary));

            return ResponseEntity.ok(tagsAsJSON);

        } catch (InvalidImageFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("An invalid image format was supplied - use JPG or PNG only");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("A failure occurred when categorising the image");
        }

    }

    @RequestMapping(value = "/tag/video", method = RequestMethod.POST)
    public ResponseEntity<String> tagVideo(
            @RequestParam("file") MultipartFile videoFile
    ) {
        logger.log(Level.INFO, "Received binary video for processing");

        if (myArmingService.armed == false) {
            logger.log(Level.INFO, "Discarding request, Detectatron is currently disarmed.");
            return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body("Detectatron is disarmed, not accepting videos for tagging");
        }

        // Convert the image from MultiPart form to actual binary data (and make sure we actually got a damn image).
        byte[] videoBinary;

        try {
            logger.log(Level.INFO, "Processing file: " + videoFile.getOriginalFilename());
            videoBinary = videoFile.getBytes();
        } catch (java.io.IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("A binary video must be POSTed to this endpoint.\n");
        }

        // Process result
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            String tagsAsJSON = objectMapper.writeValueAsString(myVideoTagService.process(videoBinary));

            return ResponseEntity.ok(tagsAsJSON);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("A failure occurred when categorising the video");
        }
    }

}

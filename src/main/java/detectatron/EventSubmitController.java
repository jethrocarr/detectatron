package detectatron;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.StorageClass;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This controller performs tagging and also S3 storage of any submitted video. It's specifically targeted for use by
 * the connectors for uploading, storing and tagging videos where they need a simple API which handles the lot for them.
 */
@RestController
@EnableAutoConfiguration
public class EventSubmitController {

    private static final Logger logger = Logger.getLogger("EventSubmitController");
    private static final String s3Bucket = System.getenv("S3_BUCKET"); // todo we should be using spring configuration?

    private static AmazonS3 s3Client = new AmazonS3Client();


    @Autowired
    VideoTagService myVideoTagService;

    @Autowired
    ArmingService myArmingService;


    @RequestMapping(value = "/event", method = RequestMethod.POST)
    public ResponseEntity<String> submitEvent(
            @RequestParam("file") MultipartFile videoFile
    ) {
        logger.log(Level.INFO, "Received video event for processing");

        byte[] videoBinary;
        TagModel videoTags = new TagModel();
        String videoKeyTags = "{}";


        /**
         * Convert the video from MultiPart form to actual binary data (and make sure we actually got a damn image).
         */
        try {
            logger.log(Level.INFO, "Processing file: " + videoFile.getOriginalFilename());
            videoBinary = videoFile.getBytes();
        } catch (java.io.IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("A binary video must be POSTed to this endpoint.\n");
        }

        /**
         * Perform tagging of the video. This step is dependent on the state of Detectatron - if unarmed, we do not
         * tag the video in order to keep our running costs low.
         */
        if (!myArmingService.armed) {
            // Disarmed, so we cannot tag the video.
            logger.log(Level.INFO, "Skipping video tagging as Detectatron disarmed.");
        } else {
            // Tag baby, tag!

            try {
                videoTags = myVideoTagService.process(videoBinary);

                ObjectMapper objectMapper = new ObjectMapper();
                videoKeyTags = objectMapper.writeValueAsString(videoTags.keyTags);

            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("A failure occurred when categorising the video");
            }
        }


        /**
         * Upload the file into the S3 bucket. This would be a bit dodgy if we were doing it for other people since we
         * trust the original filename of the video, but as Detectatron is a backend service we don't need to worry
         * about the trust worthyness of the data and can trust the filenames to be unique and sensible.
         */
        if (s3Bucket == null) {
            logger.log(Level.WARNING, "No S3 bucket configured, video will not be retained post-processing.");
        } else {
            logger.log(Level.INFO, "Uploading video to S3 bucket ("+ s3Bucket +")...");

            try {
                InputStream videoBinaryStream = new ByteArrayInputStream(videoBinary);

                ObjectMetadata s3Meta = new ObjectMetadata();
                s3Meta.setContentLength(videoBinary.length);
                s3Meta.setContentType(videoFile.getContentType());
                s3Meta.setHeader("x-amz-meta-detectatron", videoKeyTags); // include key tags on videos

                PutObjectRequest s3Request = new PutObjectRequest(s3Bucket, videoFile.getOriginalFilename(), videoBinaryStream, s3Meta);
                s3Request.setStorageClass(StorageClass.StandardInfrequentAccess); // Save money - most of this stuff is store & forget.

                s3Client.putObject(s3Request);

                logger.log(Level.INFO, "Upload completed (s3://"+ s3Bucket +"/"+ videoFile.getOriginalFilename() +")");

            } catch (RuntimeException e) {
                e.printStackTrace();
                logger.log(Level.SEVERE, "An unexpected error occurred whilst uploading the video to S3");
            }
        }


        /**
         * We have two different success codes:
         * 200 - Video submitted and uploaded successfully.
         * 201 - Video submitted, uploaded successfully and key tags were found.
         *
         * Either way, we also return the key tags JSON string.
         */
        if (videoTags.keyTags.size() > 0) {
            return ResponseEntity.status(HttpStatus.CREATED).body(videoKeyTags);
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(videoKeyTags);
        }


    }



}

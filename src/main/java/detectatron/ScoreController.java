package detectatron;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.S3Object;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.annotation.MultipartConfig;

/**
 * Accepts an image via POST and returns a JSON object with scoring information.
 */
@RestController
@EnableAutoConfiguration

public class ScoreController {

    private static final Logger logger = Logger.getLogger("ScoreController");


    @RequestMapping(value = "/score", method = RequestMethod.GET)
    public String scoreGet() {
        return "This endpoint must be invoked via POST with an image file\n";
    }

    @RequestMapping(value = "/score", method = RequestMethod.POST)
    public static ResponseEntity<String> receiverPost(
            @RequestParam("file") MultipartFile imageFile
        ) {

        logger.log(Level.INFO, "Received binary image for processing");

        // Convert the image from MultiPart form to actual binary data.
        byte[] imageBinary;

        try {
            logger.log(Level.INFO, "Processing file: " + imageFile.getOriginalFilename());
            imageBinary = imageFile.getBytes();
        } catch (java.io.IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("A binary image must be POSTed to this endpoint.\n");
        }


        // Take the bytes collected by the HTTP server and create an image object that we can pass directly
        // into AWS. Note that this is limited to 5MB maximum size - anything larger needs to be stored in S3
        // first, however that will increase the price somewhat since then we'll be paying for an S3 transaction
        // as well.
        ByteBuffer imageByteBuffer = ByteBuffer.wrap(imageBinary);
        Image imageObject = new Image();
        imageObject.setBytes(imageByteBuffer);


        /*
        // debug - write the buffer to a file
        try {
            File file = new File("/tmp/debug.jpg");
            FileChannel wChannel = new FileOutputStream(file, false).getChannel();
            wChannel.write(imageByteBuffer);
            wChannel.close();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Debug termination");

        } catch (Exception e) {
            e.printStackTrace();
        }
        */


        // Send AWS a request for label detection for our image. We only care about the top 10 over 75% score.
        logger.log(Level.INFO, "Sending image label request to AWS Rekognition");
        DetectLabelsRequest request = new DetectLabelsRequest()
                .withImage(imageObject)
                .withMaxLabels(10)
                .withMinConfidence(75F);

        AmazonRekognitionClient rekognitionClient = new AmazonRekognitionClient()
                .withEndpoint("rekognition.us-east-1.amazonaws.com");
        rekognitionClient.setSignerRegionOverride("us-east-1");


        // Process result
        try {
            DetectLabelsResult result = rekognitionClient.detectLabels(request);

            ObjectMapper objectMapper = new ObjectMapper();
            return ResponseEntity.ok(objectMapper.writeValueAsString(result.getLabels()));

        } catch (AmazonRekognitionException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("A failure occurred on the Amazon Rekognition backend");

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            e.printStackTrace();
        }



        return ResponseEntity.ok("Scoring processed successfully\n");
    }

}

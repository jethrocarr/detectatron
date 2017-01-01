package detectatron;

import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.tools.internal.ws.wsdl.framework.ValidationException;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Takes an image and performs categorisation tasks on it.
 */
public class ImageCategorisation {

    private static final Logger logger = Logger.getLogger("ImageCategorisation");

    public static String process (
            byte[] imageBinary
    ) {

        // Take the byte array and create an image object that we can pass directly into AWS.
        //
        // Note that this is limited to 5MB maximum size - anything larger needs to be stored in S3 first, however that
        // will increase the price somewhat since then we'll be paying for an S3 transaction as well - so if it's under
        // 5 MB, we really should just post it up directly.

        // TODO: We need to properly detect and be able to handle > 5MB images.

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


        // Send AWS a request for label detection for our image. We only care about the top 20 over 50% score.
        logger.log(Level.INFO, "Sending image label request to AWS Rekognition");
        DetectLabelsRequest request = new DetectLabelsRequest()
                .withImage(imageObject)
                .withMaxLabels(20)
                .withMinConfidence(50F);

        // TODO: Currently hard coded to us-east-1 and with no graceful handling of credentials and validation.
        AmazonRekognitionClient rekognitionClient = new AmazonRekognitionClient()
                .withEndpoint("rekognition.us-east-1.amazonaws.com");
        rekognitionClient.setSignerRegionOverride("us-east-1");


        // Process result
        try {
            DetectLabelsResult result = rekognitionClient.detectLabels(request);

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(result.getLabels());

        } catch (AmazonRekognitionException e) {
            e.printStackTrace();
            throw new ValidationException("An unexpected fault occured when interacting with AWS Rekognition");
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            e.printStackTrace();
            throw new ValidationException("An unexpected fault occured with JSON processing");
        }
    }
}

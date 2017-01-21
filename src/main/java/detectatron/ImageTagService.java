package detectatron;

import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.tools.internal.ws.wsdl.framework.ValidationException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Takes an image and performs categorisation tasks on it.
 */
@Service
@EnableAsync
public class ImageTagService {

    private static final Logger logger = Logger.getLogger("ImageTagService");

    /**
     * Process the supplied image.
     * @param imageBinary
     * @return
     */
    public TagModel process (
            byte[] imageBinary
    ) {

        // Take the byte array and create an image object that we can pass directly into AWS.
        //
        // Note that this is limited to 5MB maximum size - anything larger needs to be stored in S3 first, however that
        // will increase the price somewhat since then we'll be paying for an S3 transaction as well - so if it's under
        // 5 MB, we really should just post it up directly.

        // TODO: We need to properly detect and be able to handle > 5MB images. Interestingly, Rekognition doesn't
        // actually appear to enforce the 5MB upload limit currently...

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


        // Send request to AWS. This command can take a while, so we time and report for future use.
        // TODO: send duration metrics into stats/datadog
        try {

            long startTime = System.currentTimeMillis();

            DetectLabelsResult result = rekognitionClient.detectLabels(request);
            TagModel imageTags = new TagModel(result.getLabels());

            long stopTime = System.currentTimeMillis();
            int elapsedTime = (int) (stopTime - startTime) / 1000;

            logger.log(Level.INFO, "Request processed in: " + elapsedTime + " seconds.");
            logger.log(Level.INFO, "All tags: " + imageTags.allTags);
            return imageTags;

        } catch (InvalidImageFormatException e) {
            e.printStackTrace();
            throw e;
        } catch (AmazonRekognitionException e) {
            e.printStackTrace();
            throw new ValidationException("An unexpected fault occurred when interacting with AWS Rekognition");
        }

    }

    /**
     * Process the image in a background asynchronous thread. This is used by the video categorisation service
     * in order to get the data for multiple frames quickly. Note that threading performance can be limited by the
     * external services we use - for example, Rekognition has a max transactions per second limit.
     *
     * @param imageBinary
     * @return
     */
    @Async
    public Future<TagModel> processAsync(
            byte[] imageBinary
    )
            throws InterruptedException
    {
        TagModel results = process(imageBinary);
        return new AsyncResult<>(results);
    }

}

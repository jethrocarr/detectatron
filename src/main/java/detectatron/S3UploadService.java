package detectatron;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.StorageClass;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Uploads a file into S3 with some handling around the requirements of Detectatron specifically.
 */
@Service
public class S3UploadService {
    private static final Logger logger = Logger.getLogger("S3UploadService");

    private static final String s3Bucket = System.getenv("S3_BUCKET"); // todo we should be using spring configuration?
    private static AmazonS3 s3Client = new AmazonS3Client();


    public void uploader(String fileName, byte[] fileData, String customMetadata) {

        if (s3Bucket == null) {
            logger.log(Level.WARNING, "No S3 bucket configured, unable to upload "+ fileName);
        } else {
            logger.log(Level.INFO, "Uploading file to S3 bucket (s3://" + s3Bucket + "/" + fileName + ")...");

            try {
                InputStream fileBinaryStream = new ByteArrayInputStream(fileData);

                ObjectMetadata s3Meta = new ObjectMetadata();
                s3Meta.setContentLength(fileData.length);

                if (!customMetadata.equals("")) {
                    s3Meta.setHeader("x-amz-meta-detectatron", customMetadata); // Optional tag
                }

                PutObjectRequest s3Request = new PutObjectRequest(s3Bucket, fileName, fileBinaryStream, s3Meta);
                s3Request.setStorageClass(StorageClass.StandardInfrequentAccess); // Save money - most of this stuff is store & forget.

                s3Client.putObject(s3Request);

                logger.log(Level.INFO, "Upload completed");

            } catch (RuntimeException e) {
                e.printStackTrace();
                logger.log(Level.SEVERE, "An unexpected error occurred whilst uploading the file to S3");
            }
        }
    }

}

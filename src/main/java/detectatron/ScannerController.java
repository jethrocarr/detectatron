package detectatron;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;
import java.util.logging.Level;


import org.springframework.web.multipart.MultipartFile;

import javax.servlet.annotation.MultipartConfig;

/**
 * Accepts an image via POST and returns a JSON object with scoring information.
 */
@RestController
@EnableAutoConfiguration

public class ScannerController {

    private static final Logger logger = Logger.getLogger("ScannerController");


    @RequestMapping(value = "/scanner", method = RequestMethod.GET)
    public String scanner() {
        return "This endpoint must be invoked via POST with an image file\n";
    }

    @RequestMapping(value = "/scanner/image", method = RequestMethod.POST)
    public static ResponseEntity<String> scannerImage(
            @RequestParam("file") MultipartFile imageFile
        ) {

        logger.log(Level.INFO, "Received binary image for processing");

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
            String results = ImageCategorisation.process(imageBinary);
            return ResponseEntity.ok(results);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("A failure occurred when categorising the image");
        }

    }

}

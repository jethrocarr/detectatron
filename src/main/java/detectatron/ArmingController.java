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
 * Allow Detectatron to be armed/disarmed.
 */
@RestController
@EnableAutoConfiguration

public class ArmingController {

    private static final Logger logger = Logger.getLogger("ArmingController");

    @Autowired
    ArmingService myArmingService;

    @RequestMapping(value = "/arming", method = RequestMethod.GET)
    public ResponseEntity<String> armingStatus() {

        logger.log(Level.INFO, "System arming status queried.");

        if (myArmingService.armed == true) {
            return ResponseEntity.ok("System Armed");
        } else {
            return ResponseEntity.ok("System Disarmed\n");
        }
    }

    @RequestMapping(value = "/arming/armed", method = RequestMethod.GET)
    public ResponseEntity<String> arm() {

        myArmingService.armed = true;

        logger.log(Level.INFO, "System Armed!");
        return ResponseEntity.ok("System Armed\n");
    }

    @RequestMapping(value = "/arming/disarmed", method = RequestMethod.GET)
    public ResponseEntity<String> disarm() {

        myArmingService.armed = false;

        logger.log(Level.INFO, "System Disarmed!");
        return ResponseEntity.ok("System Disarmed\n");
    }

}

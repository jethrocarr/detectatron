package detectatron;

import org.springframework.stereotype.Service;

/**
 * Support service used to switch the application modes (armed->unarmed and vice-versa).
 */

@Service
public class ArmingService {

    // Flag whether or not the service is currently armed.
    public static boolean armed = true;

}

package detectatron;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Default controller for expected URL endpoints
 */
@RestController
public class DefaultController {
    @RequestMapping("/")
    public String root() {
        return "You have reached Detectatron. This page does nothing useful.";
    }
}

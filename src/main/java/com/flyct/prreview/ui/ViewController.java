package com.flyct.prreview.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to serve the manual PR review UI.
 */
@Controller
public class ViewController {

    @GetMapping("/ui")
    public String index() {
        // Forward to the static index.html
        return "forward:/index.html";
    }
}

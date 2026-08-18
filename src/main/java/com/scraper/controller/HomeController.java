package com.scraper.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    /**
     * Forwards the root path "/" directly to the static "index.html" page.
     * This keeps the URL in the address bar clean as "http://localhost:8081/".
     */
    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }
}

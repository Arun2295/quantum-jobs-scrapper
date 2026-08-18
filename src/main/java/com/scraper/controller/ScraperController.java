package com.scraper.controller;

import com.scraper.service.GenericScraperService;
import com.scraper.service.HybridScraperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ScraperController {

    private final GenericScraperService genericScraperService;
    private final HybridScraperService hybridScraperService;

    @Autowired
    public ScraperController(GenericScraperService genericScraperService,
                             HybridScraperService hybridScraperService) {
        this.genericScraperService = genericScraperService;
        this.hybridScraperService = hybridScraperService;
    }

    /**
     * Legacy Jsoup Live Quotes scraper
     */
    @GetMapping("/scrape-live")
    public ResponseEntity<?> scrapeQuotes(@RequestParam(value = "query", required = false, defaultValue = "") String query) {
        try {
            List<Map<String, String>> quotes = genericScraperService.scrapeQuotes(query);
            return ResponseEntity.ok(quotes);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error executing quotes scraping: " + e.getMessage());
        }
    }

    /**
     * Playwright + Jsoup Hybrid Scraper
     * This endpoint runs a browser to load the target page, extracts the container HTML,
     * and uses Jsoup in-memory to parse the individual job nodes extremely fast.
     */
    @GetMapping("/scrape")
    public ResponseEntity<?> scrapeJobs(
            @RequestParam(value = "query", required = false, defaultValue = "developer") String query,
            @RequestParam(value = "location", required = false, defaultValue = "United States") String location,
            @RequestParam(value = "target", required = false, defaultValue = "") String target,
            @RequestParam(value = "sources", required = false) List<String> sources
    ) {
        try {
            List<Map<String, Object>> jobs = hybridScraperService.scrapeJobs(query, location, target, sources);
            return ResponseEntity.ok(jobs);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error executing hybrid scraping pipeline: " + e.getMessage());
        }
    }
}

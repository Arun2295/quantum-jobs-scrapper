package com.scraper.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GenericScraperService {

    public List<Map<String, String>> scrapeJobs(String targetUrl) throws IOException {
        List<Map<String, String>> jobList = new ArrayList<>();

        // Setup generic browser-like headers to avoid generic User-Agent blocks
        Document doc = Jsoup.connect(targetUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("Referer", "https://www.google.com/")
                .timeout(10000)
                .get();

        // Parse individual job cards based on class/structure
        Elements cards = doc.select(".job-card");
        for (Element card : cards) {
            Map<String, String> job = new HashMap<>();
            
            String title = card.select(".job-title").text();
            String company = card.select(".job-company").text();
            String location = card.select(".job-location").text();
            String description = card.select(".job-desc").text();

            job.put("title", title);
            job.put("company", company);
            job.put("location", location);
            job.put("description", description);

            jobList.add(job);
        }

        return jobList;
    }

    public List<Map<String, String>> scrapeQuotes(String query) throws IOException {
        List<Map<String, String>> quoteList = new ArrayList<>();
        String targetUrl = "https://quotes.toscrape.com/";
        
        // If a specific tag query is provided, query that tag
        if (query != null && !query.isEmpty()) {
            targetUrl = "https://quotes.toscrape.com/tag/" + query.toLowerCase().trim() + "/";
        }

        Document doc = Jsoup.connect(targetUrl)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .timeout(10000)
                .get();

        Elements quoteElements = doc.select(".quote");
        for (Element element : quoteElements) {
            Map<String, String> item = new HashMap<>();
            
            String text = element.select(".text").text();
            String author = element.select(".author").text();
            
            // Clean up double quotes from target
            if (text.startsWith("“") && text.endsWith("”")) {
                text = text.substring(1, text.length() - 1);
            }

            item.put("quote", text);
            item.put("author", author);

            quoteList.add(item);
        }

        return quoteList;
    }
}

package com.scraper.service;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class HybridScraperService {

    private static final Logger log = LoggerFactory.getLogger(HybridScraperService.class);
    private final Browser browser;

    public HybridScraperService(Browser browser) {
        this.browser = browser;
    }

    public List<Map<String, Object>> scrapeJobs(String query, String location, String targetUrl) {
        return scrapeJobs(query, location, targetUrl, null);
    }

    public List<Map<String, Object>> scrapeJobs(String query, String location, String targetUrl, List<String> selectedSources) {
        List<Map<String, Object>> jobs = new ArrayList<>();

        log.info("Launching multi-source hybrid scrape execution for query: '{}', selected sources: {}", query, selectedSources);

        try (BrowserContext context = browser.newContext(
                new Browser.NewContextOptions()
                        .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        )) {
            // Define list of sources to scrape
            List<Map<String, String>> sources = new ArrayList<>();
            
            // 1. If targetUrl is explicitly provided, only scrape that URL
            if (targetUrl != null && !targetUrl.isEmpty()) {
                Map<String, String> customSource = new HashMap<>();
                customSource.put("name", "custom");
                customSource.put("url", targetUrl);
                customSource.put("selector", "body");
                sources.add(customSource);
            } else {
                // 2. Determine which real job boards the user selected
                boolean selectAll = (selectedSources == null || selectedSources.isEmpty());

                // Source A: Arbeitnow (Unprotected)
                if (selectAll || selectedSources.stream().anyMatch(s -> s.equalsIgnoreCase("Arbeitnow"))) {
                    Map<String, String> s1 = new HashMap<>();
                    s1.put("name", "Arbeitnow");
                    s1.put("url", "https://www.arbeitnow.com/?search=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
                    s1.put("selector", "#body");
                    sources.add(s1);
                }

                // Source B: We Work Remotely (Unprotected)
                if (selectAll || selectedSources.stream().anyMatch(s -> s.equalsIgnoreCase("WeWorkRemotely"))) {
                    Map<String, String> s2 = new HashMap<>();
                    s2.put("name", "WeWorkRemotely");
                    s2.put("url", "https://weworkremotely.com/remote-jobs?search_term=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
                    s2.put("selector", ".jobs-container, body");
                    sources.add(s2);
                }

                // Source C: LinkedIn Jobs
                if (selectAll || selectedSources.stream().anyMatch(s -> s.equalsIgnoreCase("LinkedIn"))) {
                    Map<String, String> s3 = new HashMap<>();
                    s3.put("name", "LinkedIn");
                    s3.put("url", "https://www.linkedin.com/jobs/search/?keywords=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
                    s3.put("selector", ".jobs-search__results-list, body");
                    sources.add(s3);
                }
            }

            for (Map<String, String> src : sources) {
                String sourceName = src.get("name");
                String url = src.get("url");
                String selector = src.get("selector");

                log.info("Scraping source '{}' from URL: {}", sourceName, url);
                
                try {
                    Page page = context.newPage();
                    // Set short navigation timeout to avoid hanging the entire request on one blocked site
                    page.navigate(url, new Page.NavigateOptions().setTimeout(12000));
                    
                    // Wait briefly for container
                    String[] containerSelectors = selector.split(",\\s*");
                    String activeSelector = "body";
                    for (String sel : containerSelectors) {
                        try {
                            page.locator(sel).waitFor(new Locator.WaitForOptions()
                                    .setState(com.microsoft.playwright.options.WaitForSelectorState.ATTACHED)
                                    .setTimeout(2500));
                            activeSelector = sel;
                            break;
                        } catch (Exception ignored) {}
                    }

                    String innerHtml = page.locator(activeSelector).first().innerHTML();
                    Document doc = Jsoup.parse(innerHtml);
                    
                    // Extract from standard listing elements
                    Elements cards = doc.select("li, .job-card, .job-card-container, div.job-card, article ul li");
                    int parsedCount = 0;
                    
                    for (Element card : cards) {
                        Map<String, Object> job = parseCard(card, sourceName);
                        if (job != null) {
                            jobs.add(job);
                            parsedCount++;
                        }
                    }
                    log.info("Successfully extracted {} jobs from '{}'", parsedCount, sourceName);
                    page.close();

                } catch (Exception e) {
                    log.warn("Failed to scrape source '{}': {}. Continuing with other sources.", sourceName, e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Fatal error during browser context creation: {}", e.getMessage(), e);
            throw new RuntimeException("Scraping context execution failed: " + e.getMessage(), e);
        }

        return jobs;
    }

    private Map<String, Object> parseCard(Element card, String sourceName) {
        // Look for title element (supports itemprop="title" for Arbeitnow, span.title for We Work Remotely, and standard h3/h2)
        String title = card.select("h3[itemprop='title'], span.title, .base-search-card__title, .job-title, .job-card-title, h3, h2").text().trim();

        // Look for company element (supports itemprop="hiringOrganization", span.company for We Work Remotely, and standard company classes)
        String company = card.select("a[itemprop='hiringOrganization'], span.company, .base-search-card__subtitle, .job-company, .company-name, .job-card-company, .text-primary-700, h4").text().trim();

        // Look for location element (supports span.text-gray-600, span.region for We Work Remotely, and standard location classes)
        String location = card.select(".job-search-card__location, .job-location, .job-card-location, span.text-gray-600, span.region, span[class*='location']").text().trim();

        // Look for link (supports itemprop="url", remote-jobs patterns, and standard job link selectors)
        String link = card.select("a[itemprop='url'], a[href*='/remote-jobs/'], a[href*='/jobs/'], a[href*='/view/'], a.job-card-title-link, a").attr("abs:href");
        if (link.isEmpty()) {
            link = card.select("a[itemprop='url'], a[href*='/remote-jobs/'], a[href*='/jobs/'], a[href*='/view/'], a").attr("href");
        }

        // Clean up or skip if empty essential fields
        if (title.isEmpty() || company.isEmpty()) {
            return null;
        }

        Map<String, Object> job = new HashMap<>();
        job.put("title", title);
        job.put("company", company);
        job.put("location", location.isEmpty() ? "Remote / Not specified" : location);
        job.put("applyUrl", link);
        job.put("source", sourceName);
        return job;
    }
}

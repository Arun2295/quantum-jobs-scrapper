package com.scraper.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class PlaywrightConfig {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightConfig.class);

    private Playwright playwright;
    private Browser browser;

    @Bean
    public Playwright playwright() {
        log.info("Initializing Playwright runtime bean");
        this.playwright = Playwright.create();
        return this.playwright;
    }

    @Bean
    public Browser browser(Playwright playwright) {
        log.info("Launching headless Chromium browser instance");
        this.browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                        .setHeadless(true)
                        .setArgs(List.of(
                                "--disable-blink-features=AutomationControlled",
                                "--no-sandbox",
                                "--disable-dev-shm-usage"
                        ))
        );
        return this.browser;
    }

    @PreDestroy
    public void destroy() {
        if (browser != null) {
            log.info("Closing Playwright Browser");
            browser.close();
        }
        if (playwright != null) {
            log.info("Closing Playwright Runtime");
            playwright.close();
        }
    }
}

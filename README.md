# Job Scraper API

A Spring Boot application that scrapes job listings from multiple job portals and exposes them through REST APIs. The project uses **Playwright** for browser automation and **Jsoup** for HTML parsing to collect job information from dynamic and static websites.

## Features

- Scrape jobs from multiple job portals
- REST API to fetch scraped job listings
- Browser automation using Playwright
- HTML parsing with Jsoup
- Clean and modular Spring Boot architecture
- Easy to extend by adding new scraper implementations

## Tech Stack

- Java 21
- Spring Boot
- Playwright
- Jsoup
- Maven
- REST API
- SLF4J Logging

## Project Structure

```text
src/main/java
├── config
├── controller
├── service
└── JobScraperApplication.java
```

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- Playwright Browsers

### Installation

```bash
git clone https://github.com/<your-username>/job-scraper.git
cd job-scraper
mvn clean install
```

Install Playwright browsers:

```bash
mvn exec:java -e -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install"
```

Run the application:

```bash
mvn spring-boot:run
```

The application will start at:

```
http://localhost:8080
```

## API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/jobs` | Returns scraped job listings |

## How It Works

1. The client sends a request to the REST API.
2. Spring Boot invokes the appropriate scraper service.
3. Playwright launches a browser and loads the target website.
4. Jsoup parses the page and extracts job details.
5. The scraped data is returned as a JSON response.
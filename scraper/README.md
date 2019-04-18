#Scraper Service
TODO

## API
###Start scraping the site

Starts a new crawl job and generates a correlation id.

    curl -H "Content-Type: application/json" -X POST http://localhost:8201/scrape/5cb7562bde7e6369754b3e8b

Returns:

    "Started scraping http://url-to-scrape.com"

Produces Event:

    kac.scraper.event::ScapeFinished

###Get Game details from previous crawl

Gets all games that were scraped from a given crawl job

    curl -H "Content-Type: application/json" -X GET http://localhost:8201/scrape/games/5cb7562bde7e6369754b3e8b

Returns:

    [
        ...
        
        TODO: List<OpenPlanGame>
        
        ...
    ]

## Building and Running
In top level project /volleyball run to build and package all services/dependencies:

    mvn clean install

If using Maven you can run with the command in the service folder (/volleyball/scraper):

    mvn exec:java -Dexec.mainClass="kac.crawl.ScraperApplicationKt"

For use in a JVM, run the jar file in the service folder(/volleyball/crawl):

    java -jar target/scraper-1.0-SNAPSHOT-jar-with-dependencies.jar"







mvn package
mvn exec:java -Dexec.mainClass="kac.scraper.ScraperApplicationKt"

java -jar target/scraper-1.0-SNAPSHOT-jar-with-dependencies.jar
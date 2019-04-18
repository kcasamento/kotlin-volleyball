# Controller Service
The controller service acts as the main consumer of all application events being streamed to kafka.  It subscribes to all topics and handles each event by its respective business logic requirements.

##Consumes (Topic::Event)
- kac.crawl.event::CrawlJobStarted
- kac.scraper.event::ScrapeFinished
- kac.crawl.event::CrawlJobCompleted

## Building and Running
In top level project /volleyball run to build and package all services/dependencies:

    `mvn clean install`

If using Maven you can run with the command in the service folder (/volleyball/controller):

    `mvn exec:java -Dexec.mainClass="kac.controller.MainKt"`

For use in a JVM, run the jar file in the service folder(/volleyball/crawl):

    `java -jar target/scraper-1.0-SNAPSHOT-jar-with-dependencies.jar`
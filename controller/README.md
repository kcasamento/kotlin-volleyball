# Controller
The controller service acts as the main consumer of all application events being streamed to kafka.  It subscribes to all topics and handles each event by its respective business logic requirements.

Topics:
 - *kac.crawl.events* - all events emitted from the Crawl service
 - *kac.scraper.events* - all events emitted from the Scraper service


## Building and Running
In top level project /volleyball run:
`mvn clean install`

To package all services run:
`mvn package`


If using Maven you can run with the command in the service folder (/volleyball/crawl):
`mvn exec:java -Dexec.mainClass="kac.controller.MainKt"`

For use in a JVM, run the jar file in the service folder(/volleyball/crawl):
`java -jar target/scraper-1.0-SNAPSHOT-jar-with-dependencies.jar`
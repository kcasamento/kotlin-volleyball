# Volleyball App
Sample app built with the primary intention of learning the ins and outs of a distributed microservice architecture.  The purpose of the application is to crawl an NYC based volleyball league web site for updates on new open play games and sending an email notification to subscribed users.

## Technology Stack
- [Kotlin](https://kotlinlang.org/): Language (Java wrapper)
- [Ktor](https://github.com/ktorio/ktor): Connected web application framework
- [Consul](https://www.consul.io/): Service discovery server/client
- [MongoDb](https://www.mongodb.com/): Distributed document database
- [Kafka](https://kafka.apache.org/): Message broker for pub/sub
- [Docker](https://www.docker.com/): Infrastructure containerization platform
- [Docker Compose](https://docs.docker.com/compose/): Feature of Docker that manages the orchestration of building and connecting all the different containers

## Application Topology
- __Crawl Module__: The crawl module is responsible for managing the crawl process end-to-end.  The generated "correlation id" is used in all other services to map back to the originating crawl job.
*Uses: Ktor for API web server, Kafka as a producer, MongoDb for data storage*
- __Scraper Module__: The scraper module is responsible for physically crawling the NY Urban open play site to look for all available open play spots.
*Uses: Ktor for API, Kafka as a producer, MongoDb for data storage, JSoup for Html parsing*
- __Transform Module__: The transform module is a console app set to consume events from the scraper app and transforms the resulting data to determine which of the open play games are new since the last time the crawl job ran to avoid spamming end users with duplicate notifications.
*Uses: Kafka as a producer and consumer, MondgoDb for data storage*
- __Controller Module__: The controller module is a console app that runs as the master controller of the majority of Kafka events produced by the system.  It handles each event accordingly to facilitate asynchronous communication between all the services.
*Uses: Kafka as a producer and a consumer*

### Data Architecture
<pre>

Crawl --[Kafka::Crawl Started]--> Controller --[Http: /scrape]-->

Scrape --[Kafka::New Games]--> Controller --[Http: /crawl/complete]-->
                               Transform --[Event:: New Open Games]-->

TODO:
[Event::New Open Games]--> Email Service --> SMTP

</pre>



## Running
`mvn clean install`

`docker-compose -f docker-compose-infra.yml up`

`docker-compose -f docker-compose-service.yml up`

## TODO
- Add TTL index to scrape-games in mongo db to avoid very large table
- Email service to consume transforma::NewSpotsOpened event
- Refactor kafka broker settings to be more reusable
- Setup a longer message expiration so consumers have enough time to finish
- Finish README.md's in each module
- Dependency Inject/IoC
- Error Handling
- Dead Letter Queue and retry logic for consumer apps (https://eng.uber.com/reliable-reprocessing/)

## Future TODO
- Add front end to give users a settings portal (subscribe/unsubscibe/etc)
- Text message notifications (Twilio)

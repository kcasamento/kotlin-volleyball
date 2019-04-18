#Crawl Service
The purpose of the crawl module is be the master record of all crawl jobs and record the start and end timestamps and status.  The generated id of a crawl job is used throughout the rest of the application to map back to the originating job.

## API
###Start crawl job

Starts a new crawl job and generates a correlation id.

    curl -H "Content-Type: application/json" -X POST http://localhost:8301/crawl/start

Returns:

    {
        "id":"5cb7562bde7e6369754b3e8b"
        "startTimestamp":"2019-04-17T12:36:59-0400",
        "status":"RUNNING",
        "message":""
    }

Produces Event:

    kac.crawl.event::CrawlJobStarted

###Get Crawl Jobs

Gets all crawl jobs from the database.

    curl -H "Content-Type: application/json" -X GET http://localhost:8301/crawl

Returns:

    [
        ...
        
        {
             "id":"5cb7562bde7e6369754b3e8b",
             "startTimestamp":"2019-04-17T12:36:59-0400",
             "status":"RUNNING",
             "message":""
        },
        {
            "id":"5cb7562bde7e6369753d64e6",
            "startTimestamp":"2019-04-16T12:36:59-0400",
            "status":"COMPLETED",
            "message":"API triggered completion"
        }
        
        ...
    ]
    
###Get Recent Crawl Jobs

Gets the top most recently completed crawl jobs

    curl -H "Content-Type: application/json" -X GET http://localhost:8301/crawl/recent/2

Returns:

    [
        {
             "id":"5cb7562bde7e6369754b3e8b",
             "startTimestamp":"2019-04-17T12:36:59-0400",
             "status":"COMPLETED",
             "message":"API triggered completion"
        },
        {
            "id":"5cb7562bde7e6369753d64e6",
            "startTimestamp":"2019-04-16T12:36:59-0400",
            "status":"COMPLETED",
            "message":"API triggered completion"
        }
    ]

###Complete Crawl Job

Sets the status of a running crawl job to COMPLETE.

    curl -d "Finshing from from curl test" -H "Content-Type: application/json" -X POST http://localhost:8301/crawl/5cb7562bde7e6369754b3e8b/complete

Returns:

    {
         "id":"5cb7562bde7e6369754b3e8b",
         "startTimestamp":"2019-04-17T12:36:59-0400",
         "completedTimestame":"2019-04-17T12:37:59-0400"
         "status":"COMPLETED",
         "message":"Finshing from from curl test"
    }

Produces Event:

    kac.crawl.event::CrawlJobCompleted

## Building and Running
In top level project /volleyball run to build and package all services/dependencies:

    mvn clean install

If using Maven you can run with the command in the service folder (/volleyball/crawl):

    mvn exec:java -Dexec.mainClass="kac.crawl.CrawlApplicationKt"

For use in a JVM, run the jar file in the service folder(/volleyball/crawl):

    java -jar target/crawl-1.0-SNAPSHOT-jar-with-dependencies.jar"


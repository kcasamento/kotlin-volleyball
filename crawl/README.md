#Crawl Module
The purpose of the crawl module is be the master record of all crawl jobs and record the start and end timestamps and status.  The generated id of a crawl job is used throughout the rest of the application to map back to the originating job.

## API
**Start crawl job**

Starts a new crawl job and generates a correlation id

`curl -H "Content-Type: application/json" -X POST http://localhost:8301/crawl/start`

Returns:

`{
    "id":"5cb7562bde7e6369754b3e8b"
    "startTimestamp":"2019-04-17T12:36:59-0400",
    "status":"RUNNING",
    "message":""
}`

**Get Crawl Jobs**



mvn publish
java -jar target/crawl-1.0-SNAPSHOT-jar-with-dependencies.jar



#Test Cases:

## Start crawl job
curl -H "Content-Type: application/json" -X POST http://localhost:8301/crawl/start

## Get crawl jobs
curl -H "Content-Type: application/json" -X GET http://localhost:8301/crawl

## Finish crawl job
curl -d "Finshing from from curl test" -H "Content-Type: application/json" -X POST http://localhost:8301/crawl/[ID of job]/complete


curl -d '{"name":"John Smith"}' -H "Content-Type: application/json" -X POST http://localhost:8095/customers
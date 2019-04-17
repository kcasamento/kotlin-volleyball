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
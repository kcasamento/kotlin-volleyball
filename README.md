# Volleyball App

## Running
`mvn clean install`

`mvn package`

`docker-compose up`

## TODO
- Add TTL index to scrape-games in mongo db to avoid very large table
- Update code and dockerfile to be able to connect to kafka servers (outside of localhost)
- Email service to consume transforma::NewSpotsOpened event
- Refactor kafka broker settings to be more reusable
- Setup a longer message expiration so consumers have enough time to finish
- Finish README.md's in each module
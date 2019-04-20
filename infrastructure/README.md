# Infrastructure
Uses Docker and Docker Compose to bootstrap the infrastructure needed to run the project.  There are 2 docker compose files that need to be run:

docker-compose-infra.yml - handles all external infrastructure containers such as kafka, consul, and mongo

docker-compose-service.yml - handles all service containers for the app.

## Running
1. First run the external containers to ensure all dependencies are running
    `docker-compose -f docker-compose-infra.yml up`
2. Run the service containers:
    `docker-compose -f docker-compose-service.yml up`
    
## Containers

### Consul Cluster (Service Discovery)
Consul is a service discovery agent for the volleyball app and is running here as a 3 cluster instance to ensure a leader can be elected.  Each of the custom services will ping Consul before their TTL expires notifying the health of the service.  When a node goes down, consul is able to redirect traffic to the next healthy node.

- agent 1 (internal)
- agent 2 (internal)
- agent 3 (internal)
- server 1 (internal)
- server 2 (internal)
- bootstrap server (external, hosts the Consul UI)

### Kafka cluster and Zookeeper cluster
Zookeeper is the service discovery agent required to run Kafka and is setup here to run as a 3 server cluster.  Kafka as well is setup to run as a 3 server cluster.  When initialized the zookeeper and kafka clusters will elect a leader upon an election process.

- Zookeeper 1 (kafka service discovery)
- Zookeeper 2 (kafka service discovery)
- Zookeeper 3 (kafka service discovery)
- Kafka 1 (kafka cluster)
- Kafka 2 (kafka cluster)
- Kafka 3 (kafka cluster)

### MongoDb
Simple 1 instance setup of a MongoDb server used to store all application data


### Services (JVMs)
Each of the application services are packaged in a JVM container and bootstrapped to run the target JAR.  More information about each service can be found in its module's README.
- crawl
- scrape
- transform
- controller
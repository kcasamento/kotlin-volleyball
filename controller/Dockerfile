FROM openjdk:8-jre-alpine

ENV APPLICATION_USER controller
RUN adduser -D -g '' $APPLICATION_USER

RUN mkdir /app
RUN chown -R $APPLICATION_USER /app

USER $APPLICATION_USER

COPY ./target/controller-1.0-SNAPSHOT.jar /app/controller-1.0-SNAPSHOT.jar
WORKDIR /app

CMD ["java", "-server", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCGroupMemoryLimitForHeap", "-XX:InitialRAMFraction=2", "-XX:MinRAMFraction=2", "-XX:MaxRAMFraction=2", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=100", "-XX:+UseStringDeduplication", "-jar", "controller-1.0-SNAPSHOT.jar"]
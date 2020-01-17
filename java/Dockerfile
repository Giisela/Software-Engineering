FROM java:8-jdk-alpine

COPY ./target/java-0.0.1-SNAPSHOT.jar /usr/app/

WORKDIR /usr/app

RUN sh -c 'touch java-0.0.1-SNAPSHOT.jar'

ENTRYPOINT ["java","-jar","java-0.0.1-SNAPSHOT.jar"]
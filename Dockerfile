FROM java:8-jdk-alpine
COPY ./target/poker-0.1.0-SNAPSHOT-standalone.jar /usr/app/
WORKDIR /usr/app
EXPOSE 9190
ENTRYPOINT java -jar poker-0.1.0-SNAPSHOT-standalone.jar

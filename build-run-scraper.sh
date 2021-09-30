#!/bin/bash

export PGUSER=postgres
export PGPASSWORD=rooster
export PGDATABASE=postgres
export PGHOST=localhost
export PGPORT=5432

mvn clean package -pl scraper \
  && java -Xmx128M -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -jar scraper/target/leaderboard-scraper-1.0.0-SNAPSHOT-fat.jar

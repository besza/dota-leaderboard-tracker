#!/bin/bash

export PGUSER=postgres
export PGPASSWORD=postgres
export PGDATABASE=postgres
export PGHOST=localhost
export PGPORT=5432

mvn clean package -pl server \
  && java -Xmx128M -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -jar server/target/leaderboard-server-1.0.0-SNAPSHOT-fat.jar

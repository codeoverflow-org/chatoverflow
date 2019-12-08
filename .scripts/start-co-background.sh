#!/usr/bin/env bash
set -e

# GUI needs to be compiled and the api needs to be already fetched before calling this script!
sbt run &
SBT_PID=$!

trap 'kill -9 $SBT_PID' EXIT # Stops sbt once done starting

attempt_counter=0
max_attempts=12 # 60 seconds

until curl --output /dev/null --silent --head --fail http://localhost:2400; do
  if [ ${attempt_counter} -eq ${max_attempts} ]; then
    echo "Max attempts (60 seconds) reached"
    exit 1
  fi

  echo "Waiting for ChatOverflow server to be ready: waited $((attempt_counter * 5))s"
  attempt_counter=$((attempt_counter + 1))
  sleep 5
done

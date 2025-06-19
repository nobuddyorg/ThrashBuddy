#!/bin/bash

go install go.k6.io/xk6/cmd/xk6@latest
xk6 build --with github.com/grafana/xk6-output-influxdb

. ../../../infrastructure/helm/.env

export K6_INFLUXDB_ORGANIZATION="thrash-buddy"
export K6_INFLUXDB_BUCKET="metrics"
export K6_INFLUXDB_TOKEN=$INFLUXDB_API_TOKEN
export K6_INFLUXDB_ADDR="http://influxdb.localhost" 
export K6_INSTANCE_ID="k6-instance-1"

./k6.exe run test.js -o xk6-influxdb

#!/bin/bash

go install go.k6.io/xk6/cmd/xk6@latest
xk6 build --with github.com/grafana/xk6-output-influxdb
. ../../../../infrastructure/helm/.env
export K6_INFLUXDB_ORGANIZATION="cloud-thrash"
export K6_INFLUXDB_BUCKET="metrics"
export K6_INFLUXDB_TOKEN=$INFLUXDB_API_TOKEN
export K6_INFLUXDB_ADDR="http://localhost:32002" 
./k6.exe run test.js -o xk6-influxdb

#!/bin/bash

go install go.k6.io/xk6/cmd/xk6@latest
xk6 build

. ../../../configs/.env

export K6_INSTANCE_ID="k6-instance-1"
./k6.exe run test.js

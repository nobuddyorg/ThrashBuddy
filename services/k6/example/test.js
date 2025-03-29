import http from "k6/http";
import { sleep } from "k6";
import { Gauge, Counter } from "k6/metrics";

export let options = {
    vus: 2,
    duration: "2m",
    tags: {
        host: __ENV.K6_INSTANCE_ID || "unknown-host"
    },
    summaryTrendStats: ["avg", "min", "max", "p(90)", "p(95)"],
};

const vuGauge = new Gauge("vus");
const iterationCounter = new Counter("iterations_count");

export default function () {
    if (__VU === 1 && __ITER === 0) {
        console.log("K6 Environment Variables:");
        console.log(`K6_INFLUXDB_ORGANIZATION: ${__ENV.K6_INFLUXDB_ORGANIZATION || "Not Set"}`);
        console.log(`K6_INFLUXDB_BUCKET: ${__ENV.K6_INFLUXDB_BUCKET || "Not Set"}`);
        console.log(`K6_INFLUXDB_ADDR: ${__ENV.K6_INFLUXDB_ADDR || "Not Set"}`);
        console.log(`K6_INFLUXDB_TOKEN: ${__ENV.K6_INFLUXDB_TOKEN || "Not Set"}`);
        console.log(`DUMMY_1: ${__ENV.DUMMY_1 || "Not Set"}`);
        console.log(`DUMMY_2: ${__ENV.DUMMY_2 || "Not Set"}`);
    }

    vuGauge.add(__VU);
    iterationCounter.add(1);

    http.get("https://test.k6.io");
    sleep(1);
}

import http from "k6/http";
import { check, sleep } from "k6";
import { Gauge, Counter } from "k6/metrics";

export let options = {
    vus: __ENV.TEST_VUS || 2,
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
        console.log(`K6_INSTANCE_ID: ${__ENV.K6_INSTANCE_ID || "Not Set"}`);
        console.log(`PROMETHEUS_ADDR: ${__ENV.PROMETHEUS_ADDR || "Not Set"}`);
        console.log(`DUMMY_1: ${__ENV.DUMMY_1 || "Not Set"}`);
        console.log(`DUMMY_2: ${__ENV.DUMMY_2 || "Not Set"}`);
    }

    vuGauge.add(__VU);
    iterationCounter.add(1);

    const baseUrl = __ENV.TEST_URL || 'https://test.k6.io';
    const res = http.get(baseUrl);
    check(res, {
        'status is 200': (r) => r.status === 200,
    });


    sleep(1);
}

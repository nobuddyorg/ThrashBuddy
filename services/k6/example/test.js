import http from "k6/http";
import { sleep } from "k6";

export let options = {
    vus: 2,  
    duration: "2m",
    summaryTrendStats: ["avg", "min", "max", "p(90)", "p(95)"],
};

export default function () {
    if (__VU === 1 && __ITER === 0) {
        console.log("K6 Environment Variables:");
        console.log(`K6_INFLUXDB_ORGANIZATION: ${__ENV.K6_INFLUXDB_ORGANIZATION || "Not Set"}`);
        console.log(`K6_INFLUXDB_BUCKET: ${__ENV.K6_INFLUXDB_BUCKET || "Not Set"}`);
        console.log(`K6_INFLUXDB_ADDR: ${__ENV.K6_INFLUXDB_ADDR || "Not Set"}`);
        console.log(`K6_INFLUXDB_TOKEN: ${__ENV.K6_INFLUXDB_TOKEN || "Not Set"}`);
    }

    http.get("https://test.k6.io");
    sleep(1);
}

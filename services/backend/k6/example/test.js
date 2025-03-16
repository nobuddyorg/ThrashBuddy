import http from "k6/http";
import { sleep } from "k6";

export let options = {
    vus: 5,  
    duration: "5m",
    summaryTrendStats: ["avg", "min", "max", "p(90)", "p(95)"],
};

export default function () {
    http.get("https://test.k6.io");
    sleep(1);
}

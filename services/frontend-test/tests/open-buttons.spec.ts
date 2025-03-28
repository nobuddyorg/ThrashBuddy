import { test, expect } from "@playwright/test";
import * as dotenv from "dotenv";
import path from "path";

dotenv.config({ path: path.resolve(__dirname, "../../../infrastructure/helm/.env") });

test("open minio and login", async ({ page }) => {
    await page.goto("http://localhost");

    const [newPage] = await Promise.all([page.waitForEvent("popup"), page.click("text=Files")]);

    await newPage.waitForLoadState();

    expect(newPage.url()).not.toBe("");

    const usernameInput = newPage.locator("#accessKey");
    const passwordInput = newPage.locator("#secretKey");
    await usernameInput.fill(process.env.USERNAME_TOOLS || "");
    await passwordInput.fill(process.env.PASSWORD_TOOLS || "");
    await newPage.locator("button:has-text('Login')").click();

    await expect(newPage.locator("text=cloud-thrash")).toBeVisible();
});

test("open influxdb and login", async ({ page }) => {
    await page.goto("http://localhost");

    const [newPage] = await Promise.all([page.waitForEvent("popup"), page.click("text=Data")]);

    await newPage.waitForLoadState();

    expect(newPage.url()).not.toBe("");

    const usernameInput = newPage.locator("#login");
    const passwordInput = newPage.locator("#password");
    await usernameInput.fill(process.env.USERNAME_TOOLS || "");
    await passwordInput.fill(process.env.PASSWORD_TOOLS || "");
    await newPage.locator("button:has-text('SIGN IN')").click();

    await newPage.getByTestId("tree-nav-toggle").click();
    await newPage.getByTestId("nav-item-load-data").click();
    await newPage.locator("text=Buckets").click();

    await expect(newPage.locator("text=metrics")).toBeVisible();
    await expect(newPage.locator("text=_monitoring")).toBeVisible();
});

test("open grafana and login", async ({ page }) => {
    await page.goto("http://localhost");

    const [newPage] = await Promise.all([page.waitForEvent("popup"), page.click("text=Monitoring")]);

    await newPage.waitForLoadState();

    expect(newPage.url()).not.toBe("");

    const usernameInput = newPage.getByTestId("data-testid Username input field");
    const passwordInput = newPage.getByTestId("data-testid Password input field");
    await usernameInput.fill(process.env.USERNAME_TOOLS || "");
    await passwordInput.fill(process.env.PASSWORD_TOOLS || "");
    await newPage.locator("button:has-text('Log in')").click();

    await newPage.locator("text=Dashboards").first().click();

    await expect(newPage.locator("text=Cluster metrics")).toBeVisible();
    await expect(newPage.locator("text=k6 metrics")).toBeVisible();
});

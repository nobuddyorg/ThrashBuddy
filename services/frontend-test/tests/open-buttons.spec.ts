import { test, expect, Page, Locator } from "@playwright/test";
import * as dotenv from "dotenv";
import path from "path";
import { BASE_CONFIG } from "./config";

dotenv.config({ path: path.resolve(__dirname, "../../../infrastructure/helm/.env") });

async function loginToPage({
    usernameInput,
    passwordInput,
    loginButton,
}: {
    usernameInput: Locator;
    passwordInput: Locator;
    loginButton: Locator;
}) {
    await usernameInput.fill(process.env.USERNAME_TOOLS || "");
    await passwordInput.fill(process.env.PASSWORD_TOOLS || "");
    await loginButton.click();
}

async function openPopup(page: Page, triggerText: string): Promise<Page> {
    const [popup] = await Promise.all([page.waitForEvent("popup"), page.click(`text=${triggerText}`)]);
    await popup.waitForLoadState();
    expect(popup.url()).not.toBe("");
    return popup;
}

test.describe("Tool dashboards login", () => {
    test("open Minio and login", async ({ page }) => {
        await page.goto(BASE_CONFIG.BASE_URL);

        const newPage = await openPopup(page, "Files");

        await loginToPage({
            usernameInput: newPage.locator("#accessKey"),
            passwordInput: newPage.locator("#secretKey"),
            loginButton: newPage.locator("button:has-text('Login')"),
        });

        await expect(newPage.locator("text=cloud-thrash")).toBeVisible();
    });

    test("open InfluxDB and login", async ({ page }) => {
        await page.goto(BASE_CONFIG.BASE_URL);

        const newPage = await openPopup(page, "Data");

        await loginToPage({
            usernameInput: newPage.locator("#login"),
            passwordInput: newPage.locator("#password"),
            loginButton: newPage.locator("button:has-text('SIGN IN')"),
        });

        await expect(newPage.locator("text=Get Started").first()).toBeVisible();
        await newPage.goto(`${newPage.url()}/load-data/buckets`);

        await expect(newPage.locator("text=metrics")).toBeVisible();
        await expect(newPage.locator("text=_monitoring")).toBeVisible();
    });

    test("open Grafana and login", async ({ page }) => {
        await page.goto(BASE_CONFIG.BASE_URL);

        const newPage = await openPopup(page, "Monitoring");

        await loginToPage({
            usernameInput: newPage.getByTestId("data-testid Username input field"),
            passwordInput: newPage.getByTestId("data-testid Password input field"),
            loginButton: newPage.locator("button:has-text('Log in')"),
        });

         
        await expect(newPage.locator("text=Welcome to Grafana")).toBeVisible();
        await newPage.waitForTimeout(3000);
        await newPage.locator("text=Home").first().click();
        await newPage.waitForTimeout(3000);
        await newPage.locator("text=Dashboards").first().click();

        await expect(newPage.locator("text=Cluster metrics")).toBeVisible();
        await expect(newPage.locator("text=k6 metrics")).toBeVisible();

        await newPage.locator("text=Cluster metrics").click();
        await expect(newPage.locator("text=No data").first()).toBeVisible();

        await newPage.goBack();

        await newPage.locator("text=k6 metrics").click();
        await expect(newPage.locator("text=No data").first()).toBeVisible();
    });
});

import { test, expect, Page, Locator } from "@playwright/test";
import * as dotenv from "dotenv";
import path from "path";
import { BASE_CONFIG } from "./config";

dotenv.config({ path: path.resolve(__dirname, "../../../configs/.env") });

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
  const [popup] = await Promise.all([
    page.waitForEvent("popup"),
    page.getByAltText(triggerText).click(),
  ]);
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

    await expect(newPage.locator("text=buddy-bucket")).toBeVisible();
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

    await expect(newPage.locator("text=k6 Prometheus")).toBeVisible();
    await newPage.locator("text=k6 Prometheus").click();
    await expect(newPage.locator("text=No data").first()).toBeVisible();
  });
});

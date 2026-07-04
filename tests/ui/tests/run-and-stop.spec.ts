import { test, expect, Page } from "@playwright/test";
import * as dotenv from "dotenv";
import path from "path";
import fs from "fs";
import { BASE_CONFIG } from "./config";

dotenv.config({ path: path.resolve(__dirname, "../../../configs/.env") });

const TEST_FILE_NAME = "test.js";
const TEST_FILE_PATH = path.resolve(
  __dirname,
  "../../../apps/k6/example",
  TEST_FILE_NAME
);

const checkFileExists = (filePath: string) => {
  if (!fs.existsSync(filePath)) {
    throw new Error(`File not found: ${filePath}`);
  }
};

const uploadTestFile = async (page: Page) => {
  checkFileExists(TEST_FILE_PATH);
  const fileInput = page.locator('ngx-dropzone input[type="file"]');
  await fileInput.setInputFiles(TEST_FILE_PATH);
};

test("upload files", async ({ page }) => {
  await page.goto(BASE_CONFIG.BASE_URL);

  await expect(page.getByTestId("power-button")).toBeDisabled();
  await expect(page.locator("text=Uploaded Files")).toBeVisible();
  await expect(page.getByTestId("status-text")).toContainText("Status: INIT");

  await page.getByTestId("Settings").click();

  await expect(
    page.locator(`text=${TEST_FILE_NAME}`).first()
  ).not.toBeVisible();

  await uploadTestFile(page);

  await page.getByTestId("upload-file").click();
  await expect(page.locator("text=Uploaded Files")).toBeVisible();
  await expect(page.locator(`text=${TEST_FILE_NAME}`).first()).toBeVisible();
  await page.getByTestId("back-button").click();

  await expect(page.getByTestId("power-button")).toBeEnabled();
});

test("run test", async ({ page }) => {
  test.setTimeout(240_000);
  await page.goto(BASE_CONFIG.BASE_URL);

  await expect(page.getByTestId("power-button")).toBeEnabled();
  await expect(page.getByTestId("status-text")).toContainText("Status: IDLE");

  const sliderInput = page.locator('input[type="range"]');
  await sliderInput.fill("1");

  await page.getByTestId("power-button").click();
  await expect(page.getByTestId("status-text")).toContainText(
    "Status: RUNNING"
  );

  await page.waitForTimeout(15_000);

  await expect(page.getByTestId("status-text")).toContainText("Status: IDLE", {
    timeout: 180_000,
  });
});

test("stop test", async ({ page }) => {
  test.setTimeout(120_000);
  await page.goto(BASE_CONFIG.BASE_URL);

  await expect(page.getByTestId("power-button")).toBeEnabled();
  await expect(page.getByTestId("status-text")).toContainText("Status: IDLE");

  const sliderInput = page.locator('input[type="range"]');
  await sliderInput.fill("1");

  await page.getByTestId("power-button").click();
  await expect(page.getByTestId("status-text")).toContainText(
    "Status: RUNNING"
  );

  await page.waitForTimeout(15_000);

  await page.getByTestId("power-button").click();
  await expect(page.getByTestId("status-text")).toContainText(
    "Status: STOPPING"
  );

  await expect(page.getByTestId("status-text")).toContainText("Status: IDLE", {
    timeout: 60_000,
  });
});

test("delete file", async ({ page }) => {
  await page.goto(BASE_CONFIG.BASE_URL);

  await expect(page.getByTestId("power-button")).toBeEnabled();
  await page.getByTestId("Settings").click();
  await expect(page.locator(`text=${TEST_FILE_NAME}`).first()).toBeVisible();

  const deleteButton = page.locator('button mat-icon:text("delete")').first();
  await deleteButton.click();

  await expect(page.locator("text=Uploaded Files")).toBeVisible();
  await expect(
    page.locator(`text=${TEST_FILE_NAME}`).first()
  ).not.toBeVisible();
  await page.getByTestId("back-button").click();
  await expect(page.getByTestId("power-button")).toBeDisabled();
});

test("open Grafana and login", async ({ page }) => {
  await page.goto(BASE_CONFIG.BASE_URL);

  const [newPage] = await Promise.all([
    page.waitForEvent("popup"),
    page.getByTestId("Monitoring").click(),
  ]);
  await newPage.waitForLoadState();
  expect(newPage.url()).not.toBe("");

  await newPage
    .getByTestId("data-testid Username input field")
    .fill(process.env.USERNAME_TOOLS || "");
  await newPage
    .getByTestId("data-testid Password input field")
    .fill(process.env.PASSWORD_TOOLS || "");
  await newPage.locator("button:has-text('Log in')").click();

  await expect(newPage.locator("text=Welcome to Grafana")).toBeVisible();
  await newPage.waitForTimeout(3000);
  await newPage.locator("text=Home").first().click();
  await newPage.waitForTimeout(3000);
  await newPage.locator("text=Dashboards").first().click();

  await expect(newPage.locator("text=k6 Prometheus")).toBeVisible();
  await newPage.locator("text=k6 Prometheus").click();
  await expect(newPage.locator("text=No data")).not.toBeVisible();
});

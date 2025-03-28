import { test, expect } from "@playwright/test";
import * as dotenv from "dotenv";
import path from "path";
import fs from "fs";

dotenv.config({ path: path.resolve(__dirname, "../../../infrastructure/helm/.env") });

test("upload files", async ({ page }) => {
    await page.goto("http://localhost");

    await expect(page.locator("text=Run Test")).toBeDisabled();
    await expect(page.locator("text=Stop Test")).toBeDisabled();
    await expect(page.locator("text=Uploaded Files")).not.toBeVisible();
    await expect(page.locator("text=test.js").first()).not.toBeVisible();

    const filePath = path.resolve(__dirname, "../../k6/example/test.js");

    if (!fs.existsSync(filePath)) {
        throw new Error(`File not found: ${filePath}`);
    }

    const fileInput = page.locator('ngx-dropzone input[type="file"]');
    await fileInput.setInputFiles(filePath);

    await expect(page.locator("text=test.js")).toBeVisible();

    await page.locator("text=Upload All").click();
    await expect(page.locator("text=Uploaded Files")).toBeVisible();
    await expect(page.locator("text=test.js").first()).toBeVisible();

    await expect(page.locator("text=Run Test")).toBeEnabled();
    await expect(page.locator("text=Stop Test")).toBeDisabled();
});

test("run test", async ({ page }) => {
    test.setTimeout(240_000);
    await page.goto("http://localhost");

    await expect(page.locator("text=Run Test")).toBeEnabled();
    await expect(page.locator("text=Stop Test")).toBeDisabled();

    const sliderInput = page.locator('input[type="range"]');
    await sliderInput.fill("1");
    await page.locator("text=Run Test").click();

    await expect(page.locator("text=Run Test")).toBeDisabled();
    await expect(page.locator("text=Stop Test")).toBeEnabled();

    const startTime = Date.now();
    const checkDuration = 60_000;

    while (Date.now() - startTime < checkDuration) {
        try {
            await expect(page.locator("text=Run Test")).toBeDisabled({ timeout: 1000 });
            await page.waitForTimeout(5000);
        } catch (e) {
            throw new Error(
                `Run Test button became enabled too early after ${Date.now() - startTime}ms! Error: ${e.message}`
            );
        }
    }

    await expect(page.locator("text=Run Test")).toBeEnabled({ timeout: 120_000 });
});

test("stop test", async ({ page }) => {
    test.setTimeout(120_000);
    await page.goto("http://localhost");

    await expect(page.locator("text=Run Test")).toBeEnabled();
    await expect(page.locator("text=Stop Test")).toBeDisabled();

    const sliderInput = page.locator('input[type="range"]');
    await sliderInput.fill("1");
    await page.locator("text=Run Test").click();
    await expect(page.locator("text=Run Test")).toBeDisabled();
    await expect(page.locator("text=Stop Test")).toBeEnabled();
    await page.waitForTimeout(10_000);

    await page.locator("text=Stop Test").click();
    await expect(page.locator("text=Run Test")).toBeDisabled();
    await expect(page.locator("text=Stop Test")).toBeDisabled();

    await expect(page.locator("text=Run Test")).toBeEnabled({ timeout: 30_000 });
});

test("delete file", async ({ page }) => {
    await page.goto("http://localhost");

    await expect(page.locator("text=Run Test")).toBeEnabled();
    await expect(page.locator("text=Stop Test")).toBeDisabled();
    await expect(page.locator("text=Uploaded Files")).toBeVisible();

    const deleteButton = page.locator('button mat-icon:text("delete")').first();
    await deleteButton.click();

    await expect(page.locator("Uploaded Files")).not.toBeVisible();
});

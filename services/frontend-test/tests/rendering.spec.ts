import { test, expect } from "@playwright/test";
import { BASE_CONFIG } from "./config";

test.describe("Homepage", () => {
    test("should have the correct title", async ({ page }) => {
        await page.goto(BASE_CONFIG.BASE_URL);
        await expect(page).toHaveTitle(/ThrashBuddy/);
    });
});

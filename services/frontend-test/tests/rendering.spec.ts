import { test, expect } from '@playwright/test';

test('has title', async ({ page }) => {
  await page.goto('http://localhost');
  await expect(page).toHaveTitle(/CloudThrash/);
});

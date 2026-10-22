// Abre Chromium contra la aplicación que ya está corriendo en BASE_URL.
const { chromium } = require('playwright');

const BASE_URL = process.env.BASE_URL || 'http://localhost:8080';

async function withPage(run) {
  const browser = await chromium.launch();
  try {
    const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
    await run(page);
  } finally {
    await browser.close();
  }
}

module.exports = { BASE_URL, withPage };

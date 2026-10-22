// Una sugerencia de Inicio busca con los ajustes que la persona eligió en la página.
const test = require('node:test');
const assert = require('node:assert/strict');
const { BASE_URL, withPage } = require('./page');

test('en Híbrida, la sugerencia conserva k=50, plot_1024 y multi_match', async () => {
  await withPage(async (page) => {
    // /buscar se intercepta: sin Elasticsearch, la página real terminaría en EFX-ES-001 y no importa aquí.
    await page.route('**/buscar?**', (route) =>
      route.fulfill({ status: 200, contentType: 'text/html', body: '<!DOCTYPE html><title>Resultados</title>' }));
    await page.goto(BASE_URL + '/');
    await page.click('[data-set="modo"][data-value="hibrida"]');
    await page.click('[data-set="tipo"][data-value="multi_match"]');
    await page.click('[data-set="vector"][data-value="plot_1024"]');
    await page.fill('#knn-k', '50');
    const chip = page.locator('.chip-set:not([hidden]) .chip').first();
    const expectedQuery = new URL(await chip.getAttribute('href'), BASE_URL).searchParams.get('q');

    await Promise.all([page.waitForURL(/\/buscar\?/), chip.click()]);

    const params = new URL(page.url()).searchParams;
    assert.equal(params.get('modo'), 'hibrida');
    assert.equal(params.get('k'), '50');
    assert.equal(params.get('vector'), 'plot_1024');
    assert.equal(params.get('tipo'), 'multi_match');
    assert.equal(params.get('q'), expectedQuery);
  });
});

test('sin JavaScript, el enlace del servidor sigue llevando a /buscar con su modo', async () => {
  const { chromium } = require('playwright');
  const browser = await chromium.launch();
  try {
    const page = await browser.newPage({ javaScriptEnabled: false });
    await page.goto(BASE_URL + '/');
    const href = await page.locator('.chip-set:not([hidden]) .chip').first().getAttribute('href');
    assert.match(href, /^\/buscar\?q=.+&modo=fulltext/);
  } finally {
    await browser.close();
  }
});

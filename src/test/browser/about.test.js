// El índice de «Acerca de». En escritorio queda fijo al desplazarse y marca la sección;
// en el teléfono empieza plegado y se cierra al elegir una sección.
const test = require('node:test');
const assert = require('node:assert/strict');
const { chromium } = require('playwright');
const { BASE_URL, withPage } = require('./page');

const ACTIVE = '.toc a[aria-current="location"]';

test('en escritorio el índice sigue a la vista al bajar y marca la sección elegida', async () => {
  await withPage(async (page) => {
    await page.goto(BASE_URL + '/acerca?lang=es');
    assert.equal(await page.getAttribute('#toc-details', 'open'), '');
    assert.equal(await page.getAttribute(ACTIVE, 'href'), '#intro');

    await page.click('.toc a[href="#catalogo"]');
    await page.waitForFunction(() => window.scrollY > 1000);
    await page.waitForFunction((selector) => {
      const link = document.querySelector(selector);
      return link && link.getAttribute('href') === '#catalogo';
    }, ACTIVE);
    const box = await page.locator('.toc').boundingBox();
    assert.ok(box.y >= 0 && box.y < 100, 'el índice quedó fuera de la vista: ' + box.y);

    await page.evaluate(() => document.getElementById('vectorial').scrollIntoView());
    await page.waitForFunction((selector) => {
      const link = document.querySelector(selector);
      return link && link.getAttribute('href') === '#vectorial';
    }, ACTIVE);
  });
});

test('en el teléfono el índice empieza plegado y se cierra al elegir una sección', async () => {
  const browser = await chromium.launch();
  try {
    const page = await browser.newPage({ viewport: { width: 375, height: 812 } });
    await page.goto(BASE_URL + '/acerca?lang=en');
    assert.equal(await page.getAttribute('#toc-details', 'open'), null);
    assert.equal(await page.isVisible('.toc a[href="#carga"]'), false);

    await page.click('.toc summary');
    assert.equal(await page.isVisible('.toc a[href="#carga"]'), true);
    await page.click('.toc a[href="#carga"]');
    assert.equal(await page.getAttribute('#toc-details', 'open'), null);
    await page.waitForFunction(() => window.location.hash === '#carga');
    const width = await page.evaluate(() => document.documentElement.scrollWidth);
    assert.ok(width <= 375, 'la página se desborda a lo ancho: ' + width);
  } finally {
    await browser.close();
  }
});

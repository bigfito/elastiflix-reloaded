// El navegador acepta un archivo de 25 MB exactos y rechaza uno de 25 MB + 1 byte antes de subirlo.
// El tope llega de la página (data-max-bytes), que lo toma de FileChecker.MAX_FILE_SIZE.
const test = require('node:test');
const assert = require('node:assert/strict');
const { BASE_URL, withPage } = require('./page');

const LIMIT = 25 * 1024 * 1024;

async function choose(page, lang, bytes) {
  await page.goto(BASE_URL + '/cargar?lang=' + lang);
  await page.click('#tab-json');
  await page.setInputFiles('#archivo', { name: 'catalogo.json', mimeType: 'application/json', buffer: bytes });
  await page.waitForFunction(() => !document.querySelector('#steps li[data-step="1"]').classList.contains('is-current'));
}

test('la página entrega el tope de 25 MB en bytes', async () => {
  await withPage(async (page) => {
    await page.goto(BASE_URL + '/cargar?lang=es');
    assert.equal(await page.getAttribute('[data-panel="file"]', 'data-max-bytes'), String(LIMIT));
    assert.equal(await page.getAttribute('[data-panel="file"]', 'data-max-label'), '25 MB');
  });
});

test('un archivo de 25 MB exactos pasa la comprobación del navegador', async () => {
  await withPage(async (page) => {
    await choose(page, 'es', Buffer.alloc(LIMIT, 0x20));
    assert.equal(await page.isHidden('#file-block'), true, await page.textContent('#file-reasons'));
    assert.equal(await page.getAttribute('#steps li[data-step="1"]', 'class'), 'is-done');
    assert.equal(await page.isEnabled('#file-submit'), true);
  });
});

test('un archivo de 25 MB + 1 byte se rechaza antes de subirlo, en español', async () => {
  await withPage(async (page) => {
    let uploads = 0;
    await page.route('**/cargar/archivo', (route) => {
      uploads++;
      return route.abort();
    });
    await choose(page, 'es', Buffer.alloc(LIMIT + 1, 0x20));
    assert.equal(await page.isVisible('#file-block'), true);
    assert.match(await page.textContent('#file-reasons'),
      /El archivo pesa 25\.0 MB y el máximo permitido es 25 MB\./);
    assert.equal(await page.isEnabled('#file-submit'), false);
    assert.equal(uploads, 0);
  });
});

test('un archivo de 25 MB + 1 byte se rechaza antes de subirlo, en inglés', async () => {
  await withPage(async (page) => {
    await choose(page, 'en', Buffer.alloc(LIMIT + 1, 0x20));
    assert.equal(await page.isVisible('#file-block'), true);
    assert.match(await page.textContent('#file-reasons'),
      /The file is 25\.0 MB and the maximum allowed is 25 MB\./);
  });
});

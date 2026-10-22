// La comprobación UTF-8 del navegador lee 64 KiB; un carácter cortado en el borde no es un error.
const test = require('node:test');
const assert = require('node:assert/strict');
const { BASE_URL, withPage } = require('./page');

const LIMIT = 65536;

async function choose(page, name, bytes) {
  await page.goto(BASE_URL + '/cargar');
  await page.click('#tab-json');
  await page.setInputFiles('#archivo', { name, mimeType: 'application/json', buffer: bytes });
  await page.waitForFunction(() => !document.querySelector('#steps li[data-step="1"]').classList.contains('is-current'));
}

test('un carácter de 2 bytes que empieza en el byte 65 535 se acepta', async () => {
  // «é» es C3 A9: el corte de 64 KiB deja solo C3 dentro del trozo leído.
  const bytes = Buffer.concat([Buffer.alloc(LIMIT - 1, 'a'), Buffer.from('é', 'utf8'), Buffer.from('b')]);
  assert.equal(bytes[LIMIT - 1], 0xc3);
  await withPage(async (page) => {
    await choose(page, 'borde.json', bytes);
    assert.equal(await page.isHidden('#file-block'), true, await page.textContent('#file-reasons'));
    assert.equal(await page.getAttribute('#steps li[data-step="1"]', 'class'), 'is-done');
    assert.equal(await page.isEnabled('#file-submit'), true);
  });
});

test('un byte que no es UTF-8 dentro del trozo se sigue rechazando', async () => {
  const bytes = Buffer.concat([Buffer.from('{"a":"'), Buffer.from([0xff]), Buffer.from('"}')]);
  await withPage(async (page) => {
    await choose(page, 'malo.json', bytes);
    assert.equal(await page.isVisible('#file-block'), true);
    assert.match(await page.textContent('#file-reasons'), /El contenido no es texto UTF-8\./);
  });
});

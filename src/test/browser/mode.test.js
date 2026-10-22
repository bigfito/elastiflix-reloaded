// La tarjeta «Modo de carga» y el modo de cada trabajo, con las respuestas del servidor
// simuladas por page.route(); no hace falta Elasticsearch.
const test = require('node:test');
const assert = require('node:assert/strict');
const { BASE_URL, withPage } = require('./page');

const JOB = '11111111-2222-4333-8444-555555555555';
const VALID_JSON = Buffer.from('[{"id":"1","title":"A","plot":"B"}]');

function screen(status, step, mode, written) {
  return {
    job: {
      status: status, step: step, mode: mode, kind: 'json', fileCount: 1, written: written || 0, errorTotal: 0,
      errors: [], columns: [], checks: ['1 registros leídos, con id, title y plot.']
    },
    texts: {
      fileMeta: '0.0 MB · 1 documentos', read: '1', valid: '1', replace: mode === 'bulk' ? '6 899' : '0',
      replaceLabel: mode === 'bulk' ? 'Catálogo actual que se reemplaza' : 'Reemplazarán existentes',
      errors: '0', errorSummary: '0 errores', errorRange: 'Mostrando 0 de 0 errores.', locationLabel: 'Registro',
      mappingNote: '', failure: '', warnings: [],
      importLabel: mode === 'bulk' ? 'Reemplazar el catálogo con 1 registros' : 'Agregar 1 registros',
      importText: 'texto', progressText: '0 de 1'
    }
  };
}

function json(route, status, body) {
  return route.fulfill({ status: status, contentType: 'application/json', body: JSON.stringify(body) });
}

async function serve(page, state) {
  const uploads = [];
  await page.route('**/cargar/archivo', async (route) => {
    uploads.push(route.request().postDataBuffer().toString('utf8'));
    await json(route, 200, { jobId: JOB });
  });
  await page.route('**/cargar/trabajos/' + JOB, (route) => json(route, 200, state));
  return uploads;
}

async function validate(page) {
  await page.click('#tab-json');
  await page.setInputFiles('#archivo', { name: 'peliculas.json', mimeType: 'application/json', buffer: VALID_JSON });
  await page.waitForFunction(() => !document.querySelector('#file-submit').disabled);
  await page.click('#file-submit');
}

function modeField(body) {
  const match = body.match(/name="modo"\r\n\r\n([a-z]+)\r\n/);
  return match ? match[1] : null;
}

test('append viene marcado: sin aviso, paso 4 «Escritura en el catálogo» y sin paso 5; bulk los cambia', async () => {
  await withPage(async (page) => {
    await page.goto(BASE_URL + '/cargar?lang=es');
    await page.click('#tab-json');
    assert.equal(await page.isChecked('input[name="modo"][value="append"]'), true);
    assert.equal(await page.isHidden('#bulk-warning'), true);
    assert.equal(await page.textContent('#step4-title'), 'Escritura en el catálogo');
    assert.equal(await page.isHidden('#steps li[data-step="5"]'), true);

    await page.check('input[name="modo"][value="bulk"]');
    assert.equal(await page.isVisible('#bulk-warning'), true);
    assert.match(await page.textContent('#bulk-warning'), /Con bulk, el catálogo actual \(.+\) se borra al publicar/);
    assert.equal(await page.textContent('#step4-title'), 'Importación en un índice temporal');
    assert.equal(await page.isVisible('#steps li[data-step="5"]'), true);
  });
});

test('la subida manda el modo elegido', async () => {
  await withPage(async (page) => {
    const uploads = await serve(page, screen('valido', 4, 'bulk'));
    await page.goto(BASE_URL + '/cargar?lang=es');
    await page.click('#tab-json');
    await page.check('input[name="modo"][value="bulk"]');
    await validate(page);
    await page.waitForFunction(() => !document.querySelector('#job-import').disabled);
    assert.equal(modeField(uploads[0]), 'bulk');
    assert.equal(await page.textContent('#count-replace-label'), 'Catálogo actual que se reemplaza');
    assert.equal(await page.textContent('#job-import-label'), 'Reemplazar el catálogo con 1 registros');
  });
});

test('cambiar el modo después de validar bloquea «Importar» y pide subir otra vez', async () => {
  await withPage(async (page) => {
    const uploads = await serve(page, screen('valido', 4, 'append'));
    await page.goto(BASE_URL + '/cargar?lang=es');
    await validate(page);
    await page.waitForFunction(() => !document.querySelector('#job-import').disabled);
    assert.equal(modeField(uploads[0]), 'append');

    await page.check('input[name="modo"][value="bulk"]');
    assert.equal(await page.isDisabled('#job-import'), true);
    assert.equal(await page.isEnabled('#file-submit'), true);

    await page.check('input[name="modo"][value="append"]');
    assert.equal(await page.isEnabled('#job-import'), true);
  });
});

test('al volver con ?job= de un trabajo bulk, la tarjeta marca bulk', async () => {
  await withPage(async (page) => {
    await serve(page, screen('valido', 4, 'bulk'));
    await page.goto(BASE_URL + '/cargar?lang=es&job=' + JOB);
    await page.waitForFunction(() => document.querySelector('input[name="modo"][value="bulk"]').checked);
    assert.equal(await page.isVisible('#bulk-warning'), true);
    assert.equal(await page.isVisible('#steps li[data-step="5"]'), true);
    assert.equal(await page.isEnabled('#job-import'), true);
  });
});

test('un append «agregado» deja los cuatro pasos completados y sin paso 5', async () => {
  await withPage(async (page) => {
    await serve(page, screen('agregado', 4, 'append', 1));
    await page.goto(BASE_URL + '/cargar?lang=es&job=' + JOB);
    await page.waitForFunction(() => document.querySelector('#steps li[data-step="4"]').className === 'is-done');
    for (const step of [1, 2, 3, 4]) {
      assert.equal(await page.getAttribute('#steps li[data-step="' + step + '"]', 'class'), 'is-done');
    }
    assert.equal(await page.isHidden('#steps li[data-step="5"]'), true);
    assert.equal(await page.isDisabled('#job-import'), true);
    assert.equal(await page.isDisabled('#job-cancel'), true);
  });
});

// Sin catálogo, la página la sirve otra instancia de la aplicación con un alias
// `elastiflix-it-…` que no existe (NO_CATALOG_URL). Sin esa variable, la prueba se omite.
const NO_CATALOG_URL = process.env.NO_CATALOG_URL;

test('sin catálogo: aviso, append deshabilitado, bulk marcado sin aviso de borrado y paso 5 visible',
  { skip: !NO_CATALOG_URL && 'falta NO_CATALOG_URL' }, async () => {
    await withPage(async (page) => {
      await page.goto(NO_CATALOG_URL + '/cargar?lang=es');
      assert.match(await page.textContent('#catalog-missing'), /todavía no existe\. Se crea con la primera carga/);
      assert.match(await page.textContent('.connection'), /Conectado · el catálogo todavía no existe/);
      await page.click('#tab-json');
      assert.equal(await page.isDisabled('input[name="modo"][value="append"]'), true);
      assert.equal(await page.isChecked('input[name="modo"][value="bulk"]'), true);
      assert.equal(await page.isHidden('#bulk-warning'), true);
      assert.equal(await page.textContent('#step4-title'), 'Importación en un índice temporal');
      assert.equal(await page.isVisible('#steps li[data-step="5"]'), true);
    });
  });

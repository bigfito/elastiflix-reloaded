// Revisión 3 (T-04, T-05 y T-11): estados de «Cargar datos» con las respuestas del servidor simuladas.
// page.route() contesta /cargar/archivo y /cargar/trabajos/*, así que no hace falta Elasticsearch.
const test = require('node:test');
const assert = require('node:assert/strict');
const { BASE_URL, withPage } = require('./page');

const JOB = 'job-1';
const VALID_JSON = Buffer.from('[{"id":"1","title":"A","plot":"B"}]');
const CSV = Buffer.from('id;title;plot\n1;A;B\n');

// La misma forma que JobScreen (job + texts), con lo que pinta ingest.js.
function screen(status, step) {
  return {
    job: {
      status: status, step: step, kind: 'json', fileCount: 1, written: 0, errorTotal: 0,
      errors: [], columns: [], checks: status === 'valido' ? ['1 registros leídos, con id, title y plot.'] : []
    },
    texts: {
      fileMeta: '0.0 MB · 1 documentos', read: '1', valid: '1', replace: '0', errors: '0',
      errorSummary: '0 errores', errorRange: 'Mostrando 0 de 0 errores.', locationLabel: 'Registro',
      mappingNote: '', failure: '', warnings: [], importLabel: 'Importar 1 registros',
      importText: 'Se importará 1 película.', progressText: '0 de 1 importadas'
    }
  };
}

function json(route, status, body) {
  return route.fulfill({ status: status, contentType: 'application/json', body: JSON.stringify(body) });
}

// Simula la subida y el estado del trabajo. Devuelve los contadores de peticiones.
async function serveJob(page, states, jobId = JOB) {
  const calls = { uploads: [], polls: 0 };
  let next = 0;
  await page.route('**/cargar/archivo', async (route) => {
    calls.uploads.push(route.request().postDataBuffer().toString('latin1'));
    await json(route, 200, { jobId: jobId });
  });
  await page.route('**/cargar/trabajos/' + jobId, async (route) => {
    calls.polls++;
    const state = states[Math.min(next, states.length - 1)];
    next++;
    await json(route, 200, state);
  });
  return calls;
}

async function chooseFile(page, tab, name, bytes) {
  await page.goto(BASE_URL + '/cargar');
  await page.click('#tab-' + tab);
  await page.setInputFiles('#archivo', { name: name, mimeType: 'application/octet-stream', buffer: bytes });
  await page.waitForFunction(() => !document.querySelector('#file-submit').disabled);
}

async function validJob(page) {
  const calls = await serveJob(page, [screen('valido', 4)]);
  await chooseFile(page, 'json', 'peliculas.json', VALID_JSON);
  await page.click('#file-submit');
  await page.waitForFunction(() => !document.querySelector('#job-import').disabled);
  return calls;
}

// Espera con condición (sin un tiempo fijo) a que la página haga `count` sondeos en total.
async function waitForPolls(page, calls, count) {
  const deadline = Date.now() + 5000;
  while (calls.polls < count && Date.now() < deadline) {
    await page.waitForTimeout(100);
  }
  assert.ok(calls.polls >= count, 'sondeos: ' + calls.polls);
}

test('T-04: un 409 al importar deja el aviso visible después del siguiente sondeo', async () => {
  await withPage(async (page) => {
    const calls = await validJob(page);
    await page.route('**/cargar/trabajos/' + JOB + '/importar', (route) =>
      json(route, 409, { code: 'EFX-IMP-003', message: 'El trabajo no está listo para importar' }));
    const before = calls.polls;

    await page.click('#job-import');
    await waitForPolls(page, calls, before + 1);
    await page.waitForTimeout(300);

    assert.equal(await page.isVisible('#server-error'), true);
    assert.equal(await page.textContent('#server-error'),
      'EFX-IMP-003 · El trabajo no está listo para importar.');
  });
});

// El cuerpo multipart trae cada campo como «name="separador"» seguido de una línea vacía y su valor.
function separatorOf(body) {
  const match = /name="separador"\r\n\r\n([^\r]*)\r\n/.exec(body);
  return match ? match[1] : null;
}

test('T-05: validar un CSV, cambiar el separador y validar el mismo archivo hace un segundo POST', async () => {
  await withPage(async (page) => {
    const calls = await serveJob(page, [screen('invalido', 3)]);
    await chooseFile(page, 'csv', 'peliculas.csv', CSV);
    assert.equal(await page.inputValue('#archivo'), '', 'el input se vacía para poder elegir el mismo archivo');

    await page.click('#file-submit');
    await page.waitForFunction(() => document.querySelector('#steps li[data-step="3"]').className === 'is-failed');
    assert.equal(await page.isDisabled('#file-submit'), true);
    await page.selectOption('[name="separador"]', ';');
    assert.equal(await page.isEnabled('#file-submit'), true);
    await page.click('#file-submit');
    await page.waitForFunction(() => document.querySelector('#steps li[data-step="3"]').className === 'is-failed');

    assert.equal(calls.uploads.length, 2);
    assert.equal(separatorOf(calls.uploads[0]), ',');
    assert.equal(separatorOf(calls.uploads[1]), ';');
  });
});

test('T-11: un trabajo cancelado deja el paso 4 detenido', async () => {
  await withPage(async (page) => {
    await serveJob(page, [screen('cancelado', 4)]);
    await chooseFile(page, 'json', 'peliculas.json', VALID_JSON);

    await page.click('#file-submit');
    await page.waitForFunction(() => document.querySelector('#steps li[data-step="1"]').className === 'is-done'
      && document.querySelector('#steps li[data-step="4"]').className !== '');

    const classes = await page.$$eval('#steps li', (items) => items.map((item) => item.className));
    assert.deepEqual(classes, ['is-done', 'is-done', 'is-done', 'is-failed', '']);
    assert.equal(await page.textContent('#steps li[data-step="4"] .state'), '· detenido');
  });
});

test('T-11: un doble clic en Importar hace un solo POST', async () => {
  await withPage(async (page) => {
    await validJob(page);
    let imports = 0;
    await page.route('**/cargar/trabajos/' + JOB + '/importar', async (route) => {
      imports++;
      await new Promise((resolve) => setTimeout(resolve, 500));
      await json(route, 200, screen('publicado', 5));
    });

    await page.dblclick('#job-import');
    await page.waitForFunction(() => document.querySelector('#steps li[data-step="5"]').className === 'is-done');

    assert.equal(imports, 1);
  });
});

test('T-11: una caída de red al importar muestra el aviso de red', async () => {
  await withPage(async (page) => {
    const calls = await validJob(page);
    await page.route('**/cargar/trabajos/' + JOB + '/importar', (route) => route.abort('failed'));
    const before = calls.polls;

    await page.click('#job-import');
    await waitForPolls(page, calls, before + 1);
    await page.waitForTimeout(300);

    assert.equal(await page.isVisible('#server-error'), true);
    assert.equal(await page.textContent('#server-error'), 'No se pudo conectar con el servidor de la demo. '
      + 'Revisa que siga en marcha y vuelve a intentarlo.');
  });
});

test('T-11: una caída de red al subir detiene el paso 2 con el aviso de red', async () => {
  await withPage(async (page) => {
    await page.route('**/cargar/archivo', (route) => route.abort('failed'));
    await chooseFile(page, 'json', 'peliculas.json', VALID_JSON);

    await page.click('#file-submit');
    await page.waitForSelector('#server-error:not([hidden])');

    assert.equal(await page.getAttribute('#steps li[data-step="2"]', 'class'), 'is-failed');
    assert.match(await page.textContent('#server-error'), /^No se pudo conectar con el servidor de la demo\./);
    assert.equal(await page.isEnabled('#file-submit'), true);
  });
});

async function tabState(page) {
  return page.$$eval('[role="tab"]', (items) => items.map((item) => ({
    id: item.id, selected: item.getAttribute('aria-selected'), tabIndex: item.tabIndex,
    focused: document.activeElement === item
  })));
}

function selectedOnly(state, id) {
  return state.every((item) => (item.id === id
    ? item.selected === 'true' && item.tabIndex === 0 && item.focused
    : item.selected === 'false' && item.tabIndex === -1 && !item.focused));
}

test('T-26: las pestañas usan tabindex itinerante y se recorren con flechas, Inicio y Fin', async () => {
  await withPage(async (page) => {
    await page.goto(BASE_URL + '/cargar');
    assert.deepEqual((await tabState(page)).map((item) => item.tabIndex), [0, -1, -1]);
    await page.focus('#tab-single');

    await page.keyboard.press('ArrowRight');
    assert.ok(selectedOnly(await tabState(page), 'tab-json'), JSON.stringify(await tabState(page)));
    assert.equal(await page.isVisible('#panel-file'), true);
    assert.equal(await page.textContent('#file-title'), 'Archivo JSON');

    await page.keyboard.press('ArrowRight');
    assert.ok(selectedOnly(await tabState(page), 'tab-csv'));
    assert.equal(await page.textContent('#file-title'), 'Archivo CSV');

    await page.keyboard.press('ArrowRight');
    assert.ok(selectedOnly(await tabState(page), 'tab-single'), 'la flecha da la vuelta');
    assert.equal(await page.isVisible('#panel-single'), true);

    await page.keyboard.press('ArrowLeft');
    assert.ok(selectedOnly(await tabState(page), 'tab-csv'));
    await page.keyboard.press('Home');
    assert.ok(selectedOnly(await tabState(page), 'tab-single'));
    await page.keyboard.press('End');
    assert.ok(selectedOnly(await tabState(page), 'tab-csv'));
  });
});

// Revisión 4: el id de un trabajo es un UUID, como los que da el servidor (?job= solo acepta ese formato).
const CSV_JOB = '3f2b8c1e-5d4a-4e6f-9a7b-1c2d3e4f5a6b';

function csvScreen(status, step) {
  const state = screen(status, step);
  state.job.kind = 'csv';
  state.job.columns = [{ column: 'id', field: 'id', content: 'Texto', status: 'Asignada' }];
  state.texts.mappingNote = '1 de 1 columnas asignadas.';
  state.texts.columnTones = ['ok'];
  return state;
}

test('U-20: al cambiar de idioma con un trabajo CSV activo, sigue la pestaña CSV con su separador', async () => {
  await withPage(async (page) => {
    await serveJob(page, [csvScreen('valido', 4)], CSV_JOB);
    await chooseFile(page, 'csv', 'peliculas.csv', CSV);
    await page.click('#file-submit');
    await page.waitForFunction(() => !document.querySelector('#job-import').disabled);

    await page.click('.lang-switch a[hreflang="en"]');
    await page.waitForURL((url) => url.searchParams.get('job') === CSV_JOB && url.searchParams.get('lang') === 'en');
    await page.waitForFunction(() => document.querySelector('#tab-csv').getAttribute('aria-selected') === 'true');

    assert.equal(await page.getAttribute('#tab-json', 'aria-selected'), 'false');
    assert.equal(await page.textContent('#file-title'), 'CSV file');
    assert.equal(await page.isVisible('[name="separador"]'), true);
    assert.equal(await page.isVisible('#columns-card'), true);
    assert.equal(await page.isVisible('#job'), true);
  });
});

test('U-20: al volver con ?job= de un trabajo JSON, la pestaña es la JSON', async () => {
  await withPage(async (page) => {
    await serveJob(page, [screen('valido', 4)], CSV_JOB);

    await page.goto(BASE_URL + '/cargar?job=' + CSV_JOB);
    await page.waitForFunction(() => !document.querySelector('#job-import').disabled);

    assert.equal(await page.getAttribute('#tab-json', 'aria-selected'), 'true');
    assert.equal(await page.isVisible('[name="separador"]'), false);
  });
});

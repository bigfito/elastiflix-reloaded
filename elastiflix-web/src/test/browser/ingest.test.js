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
async function serveJob(page, states) {
  const calls = { uploads: [], polls: 0 };
  let next = 0;
  await page.route('**/cargar/archivo', async (route) => {
    calls.uploads.push(route.request().postDataBuffer().toString('latin1'));
    await json(route, 200, { jobId: JOB });
  });
  await page.route('**/cargar/trabajos/' + JOB, async (route) => {
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

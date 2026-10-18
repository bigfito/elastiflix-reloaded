(function () {
  var filePanel = document.querySelector('[data-panel="file"]');
  var single = document.querySelector('[data-panel="single"]');
  var form = document.getElementById("file-form");
  if (!filePanel || !form) {
    return;
  }
  var input = document.getElementById("archivo");
  var drop = document.getElementById("drop");
  var submit = document.getElementById("file-submit");
  var importButton = document.getElementById("job-import");
  var cancelButton = document.getElementById("job-cancel");
  var maxBytes = Number(filePanel.getAttribute("data-max-bytes")) || 150 * 1024 * 1024;
  var maxLabel = filePanel.getAttribute("data-max-label") || "150 MB";
  var ACTIVE = ["recibido", "validando", "importando"];
  var STATE_LABELS = { done: "completado", current: "en curso", failed: "detenido", ready: "listo para empezar", pending: "pendiente" };
  var STATE_MARKS = { done: "✓", failed: "✕", current: "…" };
  var NETWORK_ERROR = "No se pudo conectar con el servidor de la demo. Revisa que siga en marcha y vuelve a intentarlo.";
  var POLL_RETRY = "No se pudo consultar el estado del trabajo. Se vuelve a intentar en unos segundos.";

  var tab = "json";
  var chosen = null;
  var jobId = "";
  var timer = 0;
  var busy = false;

  function byId(id) {
    return document.getElementById(id);
  }

  function show(node, visible) {
    if (node) {
      node.hidden = !visible;
    }
  }

  function megabytes(bytes) {
    return (bytes / (1024 * 1024)).toFixed(1) + " MB";
  }

  function setText(id, text) {
    byId(id).textContent = text || "";
  }

  function fill(list, lines) {
    list.replaceChildren();
    (lines || []).forEach(function (line) {
      var item = document.createElement("li");
      item.textContent = line;
      list.appendChild(item);
    });
  }

  function setStep(index, state) {
    var item = document.querySelector('#steps li[data-step="' + index + '"]');
    item.className = state === "pending" ? "" : "is-" + state;
    item.querySelector(".mark").textContent = STATE_MARKS[state] || String(index);
    item.querySelector(".state").textContent = "· " + STATE_LABELS[state];
  }

  function setSteps(states) {
    states.forEach(function (state, position) {
      setStep(position + 1, state);
    });
  }

  function stopPolling() {
    window.clearTimeout(timer);
    timer = 0;
  }

  // Un archivo nuevo, una pestaña nueva o un error nuevo empiezan de cero: no queda nada del trabajo anterior.
  function resetJob() {
    stopPolling();
    jobId = "";
    busy = false;
    ["file-block", "columns-card", "job", "job-import-card", "server-error"].forEach(function (id) {
      show(byId(id), false);
    });
    setSteps(["pending", "pending", "pending", "pending", "pending"]);
  }

  function resetFile() {
    chosen = null;
    input.value = "";
    show(byId("file-chip"), false);
    submit.disabled = true;
    resetJob();
  }

  document.querySelectorAll("[data-tab]").forEach(function (button) {
    button.addEventListener("click", function () {
      tab = button.getAttribute("data-tab");
      document.querySelectorAll("[data-tab]").forEach(function (other) {
        var on = other === button;
        other.classList.toggle("is-on", on);
        other.setAttribute("aria-selected", on ? "true" : "false");
      });
      show(single, tab === "single");
      show(filePanel, tab !== "single");
      document.querySelectorAll("[data-csv]").forEach(function (node) {
        show(node, tab === "csv");
      });
      filePanel.setAttribute("aria-labelledby", button.id);
      paintFileCopy();
      resetFile();
    });
  });

  function paintFileCopy() {
    var csv = tab === "csv";
    setText("file-title", csv ? "Archivo CSV" : "Archivo JSON");
    setText("drop-title", csv
      ? "Arrastra tu archivo .csv aquí o haz clic para elegirlo"
      : "Arrastra tu archivo .json aquí o haz clic para elegirlo");
    setText("drop-only", csv ? "Solo .csv" : "Solo .json");
    setText("file-hint", csv
      ? "Texto UTF-8 (con o sin BOM), una película por fila y la primera fila con los nombres de columna. Las listas y los objetos anidados van como JSON dentro de la celda; los textos pueden tener saltos de línea entre comillas."
      : "El archivo de exportación del índice (objeto con la lista documents y, opcionalmente, mapping y document_count), un arreglo de películas o NDJSON. Los campos usan los nombres del índice.");
    input.setAttribute("accept", csv ? ".csv,text/csv" : ".json,.ndjson,application/json");
  }

  window.addEventListener("dragover", function (event) { event.preventDefault(); });
  window.addEventListener("drop", function (event) { event.preventDefault(); });
  drop.addEventListener("dragover", function (event) {
    event.preventDefault();
    drop.classList.add("is-over");
  });
  drop.addEventListener("dragleave", function () { drop.classList.remove("is-over"); });
  drop.addEventListener("drop", function (event) {
    event.preventDefault();
    drop.classList.remove("is-over");
    choose(event.dataTransfer.files[0] || null);
  });
  input.addEventListener("change", function () {
    choose(input.files[0] || null);
  });

  // Paso 1: la comprobación del navegador corre apenas se elige el archivo.
  function choose(file) {
    resetJob();
    chosen = file;
    submit.disabled = true;
    if (!file) {
      show(byId("file-chip"), false);
      return;
    }
    setText("file-name", file.name);
    setText("file-meta", megabytes(file.size));
    byId("file-chip").classList.remove("is-rejected");
    show(byId("file-chip"), true);
    setStep(1, "current");
    browserReasons(file).then(function (reasons) {
      if (chosen !== file) {
        return;
      }
      if (reasons.length > 0) {
        rejectInBrowser(reasons);
        return;
      }
      setStep(1, "done");
      submit.disabled = false;
    });
  }

  function browserReasons(file) {
    var reason = nameOrSizeReason(file);
    if (reason) {
      return Promise.resolve([reason]);
    }
    return readUtf8(file).then(function (ok) {
      return ok ? [] : ["El contenido no es texto UTF-8."];
    });
  }

  function nameOrSizeReason(file) {
    var expected = tab === "csv" ? ".csv" : ".json";
    var name = file.name.toLowerCase();
    if (!name.endsWith(expected)) {
      return "La extensión no es " + expected + ".";
    }
    if (name.slice(0, -expected.length).indexOf(".") >= 0) {
      return "El archivo tiene más de una extensión. Solo se acepta " + expected + ".";
    }
    if (file.size > maxBytes) {
      return "El archivo pesa " + megabytes(file.size) + " y el máximo permitido es " + maxLabel + ".";
    }
    return "";
  }

  // Solo se leen 64 KiB: el corte puede partir un carácter de varios bytes. Con stream: true, el
  // decodificador guarda esos bytes para un trozo siguiente en lugar de tratarlos como un error.
  function readUtf8(file) {
    return file.slice(0, 65536).arrayBuffer().then(function (buffer) {
      new TextDecoder("utf-8", { fatal: true }).decode(buffer, { stream: true });
      return true;
    }).catch(function () {
      return false;
    });
  }

  function rejectInBrowser(reasons) {
    resetJob();
    submit.disabled = true;
    setStep(1, "failed");
    setText("file-meta", megabytes(chosen ? chosen.size : 0) + " · rechazado");
    byId("file-chip").classList.add("is-rejected");
    fill(byId("file-reasons"), reasons);
    show(byId("file-block"), true);
  }

  function serverError(text) {
    setText("server-error", text);
    show(byId("server-error"), true);
  }

  function errorText(data) {
    if (data && data.code) {
      return data.code + " · " + (data.message || "") + ".";
    }
    return NETWORK_ERROR;
  }

  function readJson(response) {
    var type = response.headers.get("content-type") || "";
    if (type.indexOf("application/json") < 0) {
      return Promise.resolve({ ok: false, data: null });
    }
    return response.json().then(function (data) {
      return { ok: response.ok, data: data };
    });
  }

  form.addEventListener("submit", function (event) {
    event.preventDefault();
    if (!chosen || submit.disabled) {
      return;
    }
    var header = form.querySelector('[name="encabezado"]');
    if (tab === "csv" && header && !header.checked) {
      rejectInBrowser(["EFX-ING-005 · El CSV no confirma que la primera fila sea el encabezado."]);
      return;
    }
    upload(chosen);
  });

  function upload(file) {
    resetJob();
    setStep(1, "done");
    setStep(2, "current");
    submit.disabled = true;
    var body = new FormData();
    var separator = form.querySelector('[name="separador"]');
    var header = form.querySelector('[name="encabezado"]');
    body.append("archivo", file, file.name);
    body.set("separador", separator ? separator.value : ",");
    body.set("encabezado", tab !== "csv" || (header && header.checked) ? "true" : "false");
    fetch("/cargar/archivo", { method: "POST", body: body })
      .then(readJson)
      .then(function (result) {
        if (!result.ok) {
          setStep(2, "failed");
          serverError(errorText(result.data));
          submit.disabled = false;
          return;
        }
        jobId = result.data.jobId;
        byId("job-report").href = "/cargar/trabajos/" + jobId + "/informe.csv";
        show(byId("job"), true);
        show(byId("job-import-card"), true);
        poll();
      })
      .catch(function () {
        setStep(2, "failed");
        serverError(NETWORK_ERROR);
        submit.disabled = false;
      });
  }

  // Una sola cadena de consultas: cada respuesta programa la siguiente y ninguna otra la duplica.
  function poll() {
    stopPolling();
    var current = jobId;
    if (!current) {
      return;
    }
    fetch("/cargar/trabajos/" + current)
      .then(readJson)
      .then(function (result) {
        if (current !== jobId) {
          return;
        }
        if (!result.ok) {
          serverError(errorText(result.data));
          return;
        }
        paint(result.data);
      })
      .catch(function () {
        if (current !== jobId) {
          return;
        }
        serverError(POLL_RETRY);
        timer = window.setTimeout(poll, 3000);
      });
  }

  function act(path) {
    busy = true;
    importButton.disabled = true;
    cancelButton.disabled = true;
    stopPolling();
    var current = jobId;
    fetch("/cargar/trabajos/" + current + "/" + path, { method: "POST" })
      .then(readJson)
      .then(function (result) {
        busy = false;
        if (current !== jobId) {
          return;
        }
        if (!result.ok) {
          serverError(errorText(result.data));
          poll();
          return;
        }
        paint(result.data);
      })
      .catch(function () {
        busy = false;
        serverError(NETWORK_ERROR);
        poll();
      });
  }

  importButton.addEventListener("click", function () { act("importar"); });
  cancelButton.addEventListener("click", function () { act("cancelar"); });

  function paint(screen) {
    var job = screen.job;
    var texts = screen.texts;
    show(byId("server-error"), false);
    paintSteps(job);
    if (job.fileCount > 0) {
      setText("file-meta", texts.fileMeta);
    }
    setText("count-read", texts.read);
    setText("count-valid", texts.valid);
    setText("count-replace", texts.replace);
    setText("count-errors", texts.errors);
    paintColumns(job, texts);
    paintErrors(job, texts);
    paintOk(job, texts);
    setText("job-failure", texts.failure);
    show(byId("job-failure"), !!texts.failure);
    paintImport(job, texts);
    if (ACTIVE.indexOf(job.status) >= 0) {
      timer = window.setTimeout(poll, 1000);
    }
  }

  function paintSteps(job) {
    for (var index = 1; index <= 5; index++) {
      setStep(index, stepState(index, job));
    }
  }

  function stepState(index, job) {
    var step = job.step || 1;
    if (job.status === "publicado") {
      return "done";
    }
    if (job.status === "valido") {
      if (index < 4) {
        return "done";
      }
      return index === 4 ? "ready" : "pending";
    }
    if (job.status === "invalido" || job.status === "fallido" || job.status === "cancelado") {
      if (index < step) {
        return "done";
      }
      return index === step ? "failed" : "pending";
    }
    if (index < step) {
      return "done";
    }
    return index === step ? "current" : "pending";
  }

  function paintColumns(job, texts) {
    var columns = job.columns || [];
    show(byId("columns-card"), job.kind === "csv" && columns.length > 0);
    setText("mapping-note", texts.mappingNote);
    var body = byId("column-rows");
    body.replaceChildren();
    columns.forEach(function (column) {
      var row = document.createElement("tr");
      row.appendChild(cell(column.column));
      var fieldCell = document.createElement("td");
      var select = document.createElement("select");
      select.className = "inp";
      select.setAttribute("aria-label", "Campo para " + column.column);
      var option = document.createElement("option");
      option.textContent = column.field;
      select.appendChild(option);
      fieldCell.appendChild(select);
      row.appendChild(fieldCell);
      row.appendChild(cell(column.content));
      var status = cell(column.status);
      status.className = "status-" + statusTone(column.status);
      row.appendChild(status);
      body.appendChild(row);
    });
  }

  function statusTone(status) {
    if (status === "Asignada") {
      return "ok";
    }
    return status === "No existe en el mapping" ? "error" : "warn";
  }

  function cell(text) {
    var td = document.createElement("td");
    td.textContent = text || "";
    return td;
  }

  function paintErrors(job, texts) {
    var errors = job.errors || [];
    show(byId("job-stopped"), job.errorTotal > 0);
    setText("error-summary", texts.errorSummary);
    setText("location-label", texts.locationLabel);
    var body = byId("job-errors");
    body.replaceChildren();
    errors.forEach(function (issue) {
      var row = document.createElement("tr");
      [issue.recordLabel, issue.id, issue.field, issue.received, issue.expected].forEach(function (value) {
        row.appendChild(cell(value));
      });
      body.appendChild(row);
    });
    setText("error-range", texts.errorRange);
  }

  function paintOk(job, texts) {
    var checks = job.checks || [];
    show(byId("job-ok"), checks.length > 0);
    fill(byId("job-checks"), checks);
    fill(byId("job-warnings"), texts.warnings);
    show(byId("warnings-title"), texts.warnings.length > 0);
    show(byId("job-warnings"), texts.warnings.length > 0);
  }

  function paintImport(job, texts) {
    importButton.disabled = busy || job.status !== "valido";
    cancelButton.disabled = busy || job.status !== "importando";
    setText("job-import-label", texts.importLabel);
    setText("import-text", texts.importText);
    setText("progress-text", texts.progressText);
    var max = Math.max(job.fileCount || 0, 1);
    var done = Math.min(job.written || 0, max);
    var bar = byId("progress");
    bar.setAttribute("aria-valuemax", String(max));
    bar.setAttribute("aria-valuenow", String(done));
    bar.querySelector("span").style.width = Math.round(done * 100 / max) + "%";
  }
}());

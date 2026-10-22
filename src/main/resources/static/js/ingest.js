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
  // El tope (25 MB fijos, FileChecker.MAX_FILE_SIZE) llega solo de la página. Sin él, el navegador deja pasar
  // el archivo y el servidor lo rechaza igual con EFX-ING-003.
  var maxBytes = Number(filePanel.getAttribute("data-max-bytes")) || Infinity;
  var maxLabel = filePanel.getAttribute("data-max-label") || "";
  var ACTIVE = ["recibido", "validando", "importando"];
  var STATE_MARKS = { done: "✓", failed: "✕", current: "…" };
  // Forma del id que da el servidor (UUID.randomUUID()); un ?job= distinto no se usa en ninguna ruta.
  var JOB_ID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

  var messages = {
    networkError: filePanel.getAttribute("data-msg-network-error"),
    pollRetry: filePanel.getAttribute("data-msg-poll-retry"),
    rejected: filePanel.getAttribute("data-msg-rejected"),
    utf8Error: filePanel.getAttribute("data-msg-utf8-error"),
    extError: filePanel.getAttribute("data-msg-ext-error"),
    multiExtError: filePanel.getAttribute("data-msg-multi-ext-error"),
    sizeError: filePanel.getAttribute("data-msg-size-error"),
    headerError: filePanel.getAttribute("data-msg-header-error"),
    fieldFor: filePanel.getAttribute("data-msg-field-for")
  };

  var stateLabels = {
    done: filePanel.getAttribute("data-state-done"),
    current: filePanel.getAttribute("data-state-current"),
    failed: filePanel.getAttribute("data-state-failed"),
    ready: filePanel.getAttribute("data-state-ready"),
    pending: filePanel.getAttribute("data-state-pending")
  };

  var tab = "json";
  var chosen = null;
  var chosenOk = false;
  var jobId = "";
  var timer = 0;
  var busy = false;
  var actionError = false;
  // Último estado pintado: al cambiar el modo se vuelve a decidir si «Importar» se puede usar.
  var lastScreen = null;
  // true mientras se espera el primer estado de un trabajo reanudado con ?job=: ese estado elige la pestaña.
  var resuming = false;

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
    var element = byId(id);
    if (element) {
      element.textContent = text || "";
    }
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
    if (!item) {
      return;
    }
    item.className = state === "pending" ? "" : "is-" + state;
    item.querySelector(".mark").textContent = STATE_MARKS[state] || String(index);
    item.querySelector(".state").textContent = "· " + (stateLabels[state] || state);
  }

  function setSteps(states) {
    states.forEach(function (state, position) {
      setStep(position + 1, state);
    });
  }

  // Ruta de un trabajo. El id va codificado para que nunca cambie la ruta (por ejemplo, con «../»).
  function jobUrl(id, suffix) {
    return "/cargar/trabajos/" + encodeURIComponent(id) + (suffix || "");
  }

  function stopPolling() {
    window.clearTimeout(timer);
    timer = 0;
  }

  function updateLangSwitch(currentJob) {
    document.querySelectorAll(".lang-switch a[hreflang]").forEach(function (link) {
      var href = link.getAttribute("href") || "";
      var parsed;
      try {
        parsed = new URL(href, window.location.origin);
      } catch (err) {
        return;
      }
      if (currentJob) {
        parsed.searchParams.set("job", currentJob);
      } else {
        parsed.searchParams.delete("job");
      }
      link.setAttribute("href", parsed.pathname + parsed.search);
    });
    try {
      var url = new URL(window.location.href);
      if (currentJob) {
        url.searchParams.set("job", currentJob);
      } else {
        url.searchParams.delete("job");
      }
      window.history.replaceState({}, "", url.pathname + (url.search ? url.search : ""));
    } catch (err) {
      /* omit in non-browser testing */
    }
  }

  function resetJob() {
    stopPolling();
    jobId = "";
    lastScreen = null;
    busy = false;
    actionError = false;
    resuming = false;
    updateLangSwitch("");
    ["file-block", "columns-card", "job", "job-import-card", "server-error"].forEach(function (id) {
      show(byId(id), false);
    });
    setSteps(["pending", "pending", "pending", "pending", "pending"]);
  }

  function resetFile() {
    chosen = null;
    chosenOk = false;
    input.value = "";
    show(byId("file-chip"), false);
    submit.disabled = true;
    resetJob();
  }

  var tabs = Array.prototype.slice.call(document.querySelectorAll("[data-tab]"));
  var modeInputs = Array.prototype.slice.call(document.querySelectorAll('input[name="modo"]'));
  // Sin catálogo (Requerimiento 7), append viene deshabilitado y bulk marcado; no hay catálogo que bulk borre.
  var noCatalog = (byId("mode-card") || document.body).getAttribute("data-no-catalog") === "true";

  // Modo de la tarjeta «Modo de carga»: append (por defecto) o bulk.
  function selectedMode() {
    var checked = document.querySelector('input[name="modo"]:checked');
    return checked && checked.value === "bulk" ? "bulk" : "append";
  }

  // Lo que depende del modo: la opción marcada, el aviso de bulk, el texto del paso 4 y si hay paso 5.
  function paintMode(mode) {
    var bulk = mode === "bulk";
    modeInputs.forEach(function (option) {
      option.checked = option.value === mode;
      option.closest(".mode").classList.toggle("is-on", option.checked);
    });
    show(byId("bulk-warning"), bulk && !noCatalog);
    var step4 = document.querySelector('#steps li[data-step="4"]');
    if (step4) {
      setText("step4-title", step4.getAttribute(bulk ? "data-title-bulk" : "data-title-append"));
      setText("step4-detail", step4.getAttribute(bulk ? "data-detail-bulk" : "data-detail-append"));
    }
    show(document.querySelector('#steps li[data-step="5"]'), bulk);
  }

  modeInputs.forEach(function (option) {
    option.addEventListener("change", function () {
      paintMode(selectedMode());
      // El modo es del trabajo: con otro modo hay que volver a subir el archivo, como al cambiar el separador.
      if (chosen && chosenOk) {
        submit.disabled = false;
      }
      if (lastScreen) {
        paintImport(lastScreen.job, lastScreen.texts);
      }
    });
  });

  // La tarjeta llega con el modo del servidor: bulk si el catálogo todavía no existe.
  paintMode(selectedMode());

  // Muestra la pestaña y su panel sin tocar el archivo elegido ni el trabajo en curso.
  function showTab(button) {
    tab = button.getAttribute("data-tab");
    tabs.forEach(function (other) {
      var on = other === button;
      other.classList.toggle("is-on", on);
      other.setAttribute("aria-selected", on ? "true" : "false");
      other.tabIndex = on ? 0 : -1;
    });
    show(single, tab === "single");
    show(filePanel, tab !== "single");
    document.querySelectorAll("[data-csv]").forEach(function (node) {
      show(node, tab === "csv");
    });
    filePanel.setAttribute("aria-labelledby", button.id);
    paintFileCopy();
  }

  function selectTab(button) {
    showTab(button);
    resetFile();
  }

  var TAB_KEYS = { ArrowRight: 1, ArrowLeft: -1, Home: "first", End: "last" };

  function tabAfterKey(button, key) {
    var move = TAB_KEYS[key];
    if (move === "first") {
      return tabs[0];
    }
    if (move === "last") {
      return tabs[tabs.length - 1];
    }
    return tabs[(tabs.indexOf(button) + move + tabs.length) % tabs.length];
  }

  tabs.forEach(function (button) {
    button.addEventListener("click", function () { selectTab(button); });
    button.addEventListener("keydown", function (event) {
      if (!(event.key in TAB_KEYS)) {
        return;
      }
      event.preventDefault();
      var next = tabAfterKey(button, event.key);
      next.focus();
      selectTab(next);
    });
  });

  function paintFileCopy() {
    var csv = tab === "csv";
    setText("file-title", filePanel.getAttribute(csv ? "data-title-csv" : "data-title-json"));
    setText("drop-title", filePanel.getAttribute(csv ? "data-drop-csv" : "data-drop-json"));
    setText("drop-only", filePanel.getAttribute(csv ? "data-only-csv" : "data-only-json"));
    setText("file-hint", filePanel.getAttribute(csv ? "data-hint-csv" : "data-hint-json"));
    input.setAttribute("accept", csv ? ".csv,text/csv" : ".json,application/json");
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
  form.querySelectorAll('[name="separador"], [name="encabezado"]').forEach(function (option) {
    option.addEventListener("change", function () {
      if (chosen && chosenOk) {
        submit.disabled = false;
      }
    });
  });

  function choose(file) {
    resetJob();
    chosen = file;
    chosenOk = false;
    input.value = "";
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
      chosenOk = true;
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
      return ok ? [] : [messages.utf8Error];
    });
  }

  function nameOrSizeReason(file) {
    var expected = tab === "csv" ? ".csv" : ".json";
    var name = file.name.toLowerCase();
    if (!name.endsWith(expected)) {
      return messages.extError.replace("{0}", expected);
    }
    if (name.slice(0, -expected.length).indexOf(".") >= 0) {
      return messages.multiExtError.replace("{0}", expected);
    }
    if (file.size > maxBytes) {
      return messages.sizeError.replace("{0}", megabytes(file.size)).replace("{1}", maxLabel);
    }
    return "";
  }

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
    setText("file-meta", megabytes(chosen ? chosen.size : 0) + " · " + messages.rejected);
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
    return messages.networkError;
  }

  function readJson(response) {
    var type = response.headers.get("content-type") || "";
    if (type.indexOf("application/json") < 0) {
      return Promise.resolve({ ok: false, status: response.status, data: null });
    }
    return response.json().then(function (data) {
      return { ok: response.ok, status: response.status, data: data };
    });
  }

  form.addEventListener("submit", function (event) {
    event.preventDefault();
    if (!chosen || submit.disabled) {
      return;
    }
    var header = form.querySelector('[name="encabezado"]');
    if (tab === "csv" && header && !header.checked) {
      rejectInBrowser([messages.headerError]);
      return;
    }
    upload(chosen);
  });

  function upload(file) {
    resetJob();
    setText("file-meta", megabytes(file.size));
    byId("file-chip").classList.remove("is-rejected");
    setStep(1, "done");
    setStep(2, "current");
    submit.disabled = true;
    var body = new FormData();
    var separator = form.querySelector('[name="separador"]');
    var header = form.querySelector('[name="encabezado"]');
    body.append("archivo", file, file.name);
    body.set("separador", separator ? separator.value : ",");
    body.set("encabezado", tab !== "csv" || (header && header.checked) ? "true" : "false");
    body.set("modo", selectedMode());
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
        updateLangSwitch(jobId);
        byId("job-report").href = jobUrl(jobId, "/informe.csv");
        show(byId("job"), true);
        show(byId("job-import-card"), true);
        poll();
      })
      .catch(function () {
        setStep(2, "failed");
        serverError(messages.networkError);
        submit.disabled = false;
      });
  }

  function poll() {
    stopPolling();
    var current = jobId;
    if (!current) {
      return;
    }
    fetch(jobUrl(current))
      .then(readJson)
      .then(function (result) {
        if (current !== jobId) {
          return;
        }
        if (!result.ok) {
          if (result.status === 404 || (result.data && result.data.code === "EFX-IMP-005")) {
            resetJob();
            return;
          }
          serverError(errorText(result.data));
          return;
        }
        paint(result.data);
      })
      .catch(function () {
        if (current !== jobId) {
          return;
        }
        if (!actionError) {
          serverError(messages.pollRetry);
        }
        timer = window.setTimeout(poll, 3000);
      });
  }

  function act(path) {
    busy = true;
    actionError = false;
    importButton.disabled = true;
    cancelButton.disabled = true;
    stopPolling();
    var current = jobId;
    fetch(jobUrl(current, "/" + path), { method: "POST" })
      .then(readJson)
      .then(function (result) {
        busy = false;
        if (current !== jobId) {
          return;
        }
        if (!result.ok) {
          actionFailed(errorText(result.data));
          return;
        }
        paint(result.data);
      })
      .catch(function () {
        busy = false;
        if (current === jobId) {
          actionFailed(messages.networkError);
        }
      });
  }

  function actionFailed(text) {
    actionError = true;
    serverError(text);
    poll();
  }

  importButton.addEventListener("click", function () { act("importar"); });
  cancelButton.addEventListener("click", function () { act("cancelar"); });

  function paint(screen) {
    var job = screen.job;
    var texts = screen.texts;
    if (resuming) {
      // Al volver con ?job= (por ejemplo, tras cambiar de idioma), la pestaña es la del tipo del trabajo.
      resuming = false;
      showTab(byId(job.kind === "csv" ? "tab-csv" : "tab-json"));
      paintMode(job.mode === "bulk" ? "bulk" : "append");
    }
    lastScreen = screen;
    show(byId("server-error"), actionError);
    paintSteps(job);
    if (job.fileCount > 0) {
      setText("file-meta", texts.fileMeta);
    }
    setText("count-read", texts.read);
    setText("count-valid", texts.valid);
    setText("count-replace", texts.replace);
    setText("count-replace-label", texts.replaceLabel);
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
    if (job.status === "publicado" || job.status === "agregado") {
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
    var tones = texts.columnTones || [];
    columns.forEach(function (column, index) {
      var row = document.createElement("tr");
      row.appendChild(cell(column.column));
      var fieldCell = document.createElement("td");
      var select = document.createElement("select");
      select.className = "inp";
      select.setAttribute("aria-label", messages.fieldFor.replace("{0}", column.column));
      var option = document.createElement("option");
      option.textContent = column.field;
      select.appendChild(option);
      fieldCell.appendChild(select);
      row.appendChild(fieldCell);
      row.appendChild(cell(column.content));
      var status = cell(column.status);
      status.className = "status-" + (tones[index] || "warn");
      row.appendChild(status);
      body.appendChild(row);
    });
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
    // Un trabajo validado en otro modo no se importa: hay que volver a subir el archivo con el modo elegido.
    importButton.disabled = busy || job.status !== "valido" || (job.mode || "append") !== selectedMode();
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

  // Reanuda el sondeo si la URL trae ?job=<jobId>. Un valor que no es un UUID se quita de la URL y se ignora.
  try {
    var params = new URLSearchParams(window.location.search);
    var initialJob = params.get("job");
    if (initialJob && !JOB_ID.test(initialJob)) {
      updateLangSwitch("");
    } else if (initialJob) {
      // Mientras llega el primer estado se muestra el panel de archivo; ese estado elige JSON o CSV.
      resuming = true;
      showTab(byId("tab-json"));
      jobId = initialJob;
      byId("job-report").href = jobUrl(jobId, "/informe.csv");
      show(byId("job"), true);
      show(byId("job-import-card"), true);
      updateLangSwitch(initialJob);
      poll();
    }
  } catch (err) {
    /* omit in non-browser testing */
  }
}());

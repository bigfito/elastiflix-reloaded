(function () {
  var filePanel = document.querySelector('[data-panel="file"]');
  var single = document.querySelector('[data-panel="single"]');
  var title = document.getElementById("file-title");
  var input = document.getElementById("archivo");
  var drop = document.getElementById("drop");
  var chosen = null;
  var tab = "json";
  var jobId = "";

  function show(node, visible) {
    if (!node) {
      return;
    }
    if (visible) {
      node.removeAttribute("hidden");
    } else {
      node.setAttribute("hidden", "");
    }
  }

  function showCsv(visible) {
    document.querySelectorAll("[data-csv]").forEach(function (node) {
      show(node, visible);
    });
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
      showCsv(tab === "csv");
      filePanel.setAttribute("aria-labelledby", button.id);
      paintFileCopy();
      chosen = null;
      input.value = "";
    });
  });

  function paintFileCopy() {
    var csv = tab === "csv";
    title.textContent = csv ? "Archivo CSV" : "Archivo JSON";
    document.getElementById("drop-title").textContent = csv
      ? "Arrastra tu archivo .csv aquí o haz clic para elegirlo"
      : "Arrastra tu archivo .json aquí o haz clic para elegirlo";
    document.getElementById("file-hint").textContent = csv
      ? "Texto UTF-8 (con o sin BOM), una película por fila y la primera fila con los nombres de columna. Las listas y los objetos anidados van como JSON dentro de la celda; los textos pueden tener saltos de línea entre comillas."
      : "El archivo de exportación del índice (objeto con la lista documents y, opcionalmente, mapping y document_count), un arreglo de películas o NDJSON. Los campos usan los nombres del índice.";
    input.setAttribute("accept", csv ? ".csv,text/csv" : ".json,application/json");
  }

  window.addEventListener("dragover", function (event) { event.preventDefault(); });
  window.addEventListener("drop", function (event) { event.preventDefault(); });
  if (drop) {
    drop.addEventListener("dragover", function (event) {
      event.preventDefault();
      drop.classList.add("is-over");
    });
    drop.addEventListener("dragleave", function () { drop.classList.remove("is-over"); });
    drop.addEventListener("drop", function (event) {
      event.preventDefault();
      drop.classList.remove("is-over");
      chosen = event.dataTransfer.files[0] || null;
      remember(chosen);
    });
  }
  if (input) {
    input.addEventListener("change", function () {
      chosen = input.files[0] || null;
      remember(chosen);
    });
  }

  function remember(file) {
    var label = document.getElementById("chosen");
    label.textContent = file ? file.name + " · " + file.size + " bytes" : "";
    show(label, !!file);
  }

  var form = document.getElementById("file-form");
  if (!form) {
    return;
  }
  form.addEventListener("submit", function (event) {
    event.preventDefault();
    var file = chosen || (input.files ? input.files[0] : null);
    if (!file) {
      block("Elige un archivo .json o .csv.");
      return;
    }
    var reason = localReason(file);
    if (reason) {
      block(reason);
      return;
    }
    readUtf8(file).then(function (ok) {
      if (!ok) {
        block("El contenido no es texto UTF-8.");
        return;
      }
      upload(file);
    }).catch(function () {
      block("No se pudo leer el archivo en el navegador.");
    });
  });

  function localReason(file) {
    var expected = tab === "csv" ? ".csv" : ".json";
    var name = file.name.toLowerCase();
    if (!name.endsWith(expected)) {
      return "La extensión no es " + expected + ".";
    }
    if (name.slice(0, -expected.length).indexOf(".") >= 0) {
      return "El archivo tiene más de una extensión. Solo se acepta " + expected + ".";
    }
    if (file.size > 150 * 1024 * 1024) {
      return "El archivo pesa más de 150 MB y el máximo permitido es 150 MB.";
    }
    return "";
  }

  function readUtf8(file) {
    return file.slice(0, 65536).arrayBuffer().then(function (buffer) {
      new TextDecoder("utf-8", { fatal: true }).decode(buffer);
      return true;
    }).catch(function () {
      return false;
    });
  }

  function readJson(response) {
    var type = response.headers.get("content-type") || "";
    if (type.indexOf("application/json") < 0) {
      return Promise.reject(new Error("respuesta sin JSON"));
    }
    return response.json().then(function (data) {
      return { ok: response.ok, data: data };
    });
  }

  function failureText(data) {
    if (!data) {
      return "No se pudo completar la carga.";
    }
    return (data.code || "") + " " + (data.message || "");
  }

  function block(text) {
    document.getElementById("file-error").textContent = text;
    show(document.getElementById("file-block"), true);
  }

  function upload(file) {
    var body = new FormData();
    body.append("archivo", file, file.name);
    var separator = form.querySelector('[name="separador"]');
    var header = form.querySelector('[name="encabezado"]');
    body.set("separador", separator ? separator.value : ",");
    body.set("encabezado", header && header.checked ? "true" : "false");
    fetch("/cargar/archivo", { method: "POST", body: body })
      .then(readJson)
      .then(function (result) {
        if (!result.ok) {
          block(failureText(result.data));
          return;
        }
        show(document.getElementById("file-block"), false);
        jobId = result.data.jobId;
        show(document.getElementById("job"), true);
        document.getElementById("job-report").href = "/cargar/trabajos/" + jobId + "/informe.csv";
        poll();
      })
      .catch(function () { block("No se pudo subir el archivo."); });
  }

  function poll() {
    if (!jobId) {
      return;
    }
    fetch("/cargar/trabajos/" + jobId)
      .then(readJson)
      .then(function (result) {
        if (!result.ok) {
          block(failureText(result.data));
          return;
        }
        paint(result.data);
      })
      .catch(function () { block("No se pudo consultar el trabajo."); });
  }

  function paint(job) {
    paintSteps(job);
    document.getElementById("count-read").textContent = job.fileCount;
    document.getElementById("count-valid").textContent = job.validCount;
    document.getElementById("count-replace").textContent = job.replaceCount;
    document.getElementById("count-errors").textContent = job.errorTotal;
    paintErrors(job);
    paintOk(job);
    paintFailure(job);
    paintImport(job);
    if (job.status === "validando" || job.status === "importando" || job.status === "recibido") {
      window.setTimeout(poll, 1000);
    }
  }

  function paintSteps(job) {
    var labels = { done: "completado", current: "en curso", failed: "detenido", ready: "listo para empezar", pending: "pendiente" };
    var marks = { done: "✓", failed: "✕", current: "…", ready: "4" };
    document.querySelectorAll("#steps li").forEach(function (item) {
      var index = Number(item.getAttribute("data-step"));
      var state = stepState(index, job);
      item.className = "is-" + state;
      item.querySelector(".mark").textContent = marks[state] || String(index);
      item.querySelector("em").textContent = "· " + labels[state];
    });
  }

  function stepState(index, job) {
    var step = job.step || 0;
    if (job.status === "publicado") {
      return "done";
    }
    if (job.status === "valido") {
      if (index < 4) {
        return "done";
      }
      return index === 4 ? "ready" : "pending";
    }
    if (job.status === "invalido" || job.status === "fallido") {
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

  function paintErrors(job) {
    var box = document.getElementById("job-stopped");
    var errors = job.errors || [];
    show(box, errors.length > 0 || job.errorTotal > 0);
    document.getElementById("job-stopped-text").textContent =
      "Se revisó el archivo completo y se encontraron " + job.errorTotal + " errores. No se importó ningún registro.";
    var body = document.getElementById("job-errors");
    body.replaceChildren();
    errors.forEach(function (issue) {
      var row = document.createElement("tr");
      [issue.recordLabel, issue.id, issue.field, issue.received, issue.expected].forEach(function (cell) {
        var td = document.createElement("td");
        td.textContent = cell || "";
        row.appendChild(td);
      });
      body.appendChild(row);
    });
    document.getElementById("error-range").textContent =
      "Mostrando " + errors.length + " de " + job.errorTotal + " errores.";
  }

  function paintOk(job) {
    var box = document.getElementById("job-ok");
    var checks = job.checks || [];
    show(box, checks.length > 0);
    fill(document.getElementById("job-checks"), checks);
    fill(document.getElementById("job-warnings"), grouped(job.warnings || []));
  }

  function grouped(warnings) {
    var vectors = false;
    var lines = [];
    warnings.forEach(function (issue) {
      if (issue.field === "plot_256" || issue.field === "plot_512" || issue.field === "plot_1024") {
        vectors = true;
        return;
      }
      lines.push((issue.field || "") + ": " + (issue.expected || issue.received || ""));
    });
    if (vectors) {
      lines.unshift("El archivo trae plot_256, plot_512 y plot_1024: se ignorarán y se generarán con el servicio de inferencia.");
    }
    return lines;
  }

  function fill(list, lines) {
    list.replaceChildren();
    lines.forEach(function (line) {
      var item = document.createElement("li");
      item.textContent = line;
      list.appendChild(item);
    });
  }

  function paintFailure(job) {
    var node = document.getElementById("job-failure");
    if (!job.failureCode) {
      show(node, false);
      return;
    }
    node.textContent = job.failureMessage ? job.failureCode + " · " + job.failureMessage : job.failureCode;
    show(node, true);
  }

  function paintImport(job) {
    var button = document.getElementById("job-import");
    var cancel = document.getElementById("job-cancel");
    var ready = job.status === "valido";
    button.disabled = !ready;
    button.textContent = ready ? "Importar " + job.fileCount + " registros" : "Importación bloqueada";
    cancel.disabled = job.status !== "valido" && job.status !== "importando";
    var max = job.fileCount || 0;
    var bar = document.getElementById("progress");
    bar.setAttribute("aria-valuemax", String(max));
    bar.setAttribute("aria-valuenow", String(job.written || 0));
    var width = max === 0 ? 0 : Math.round((job.written || 0) * 100 / max);
    bar.replaceChildren();
    var fillBar = document.createElement("span");
    fillBar.style.width = width + "%";
    bar.appendChild(fillBar);
    document.getElementById("progress-text").textContent =
      (job.written || 0) + " de " + max + " importadas · lote " + (job.batch || 0) + " de " + (job.batches || 0);
  }

  document.getElementById("job-import").addEventListener("click", function () {
    fetch("/cargar/trabajos/" + jobId + "/importar", { method: "POST" }).then(poll).catch(function () {
      block("No se pudo empezar la importación.");
    });
  });
  document.getElementById("job-cancel").addEventListener("click", function () {
    fetch("/cargar/trabajos/" + jobId + "/cancelar", { method: "POST" }).then(poll).catch(function () {
      block("No se pudo cancelar la importación.");
    });
  });
}());

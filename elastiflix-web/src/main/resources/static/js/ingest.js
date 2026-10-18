(function () {
  var filePanel = document.querySelector('[data-panel="file"]');
  var single = document.querySelector('[data-panel="single"]');
  var csvBox = document.querySelector("[data-csv]");
  var title = document.getElementById("file-title");

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

  document.querySelectorAll("[data-tab]").forEach(function (button) {
    button.addEventListener("click", function () {
      var tab = button.getAttribute("data-tab");
      document.querySelectorAll("[data-tab]").forEach(function (other) {
        var on = other === button;
        other.classList.toggle("is-on", on);
        other.setAttribute("aria-selected", on ? "true" : "false");
      });
      show(single, tab === "single");
      show(filePanel, tab !== "single");
      show(csvBox, tab === "csv");
      if (title) {
        title.textContent = tab === "csv" ? "Importar .csv" : "Importar .json";
      }
      var input = document.getElementById("archivo");
      if (input) {
        input.setAttribute("accept", tab === "csv" ? ".csv" : ".json");
      }
    });
  });

  var form = document.getElementById("file-form");
  if (!form) {
    return;
  }
  var jobId = "";
  form.addEventListener("submit", function (event) {
    event.preventDefault();
    var input = document.getElementById("archivo");
    var error = document.getElementById("file-error");
    var file = input.files[0];
    if (!file) {
      error.textContent = "Elige un archivo .json o .csv.";
      show(error, true);
      return;
    }
    if (file.size > 150 * 1024 * 1024) {
      error.textContent = "El archivo supera 150 MB.";
      show(error, true);
      return;
    }
    var body = new FormData(form);
    if (!form.querySelector('[name="encabezado"]').checked) {
      body.set("encabezado", "false");
    }
    fetch("/cargar/archivo", { method: "POST", body: body })
      .then(function (response) { return response.json().then(function (data) { return { ok: response.ok, data: data }; }); })
      .then(function (result) {
        if (!result.ok) {
          error.textContent = (result.data.code || "") + " " + (result.data.message || "");
          show(error, true);
          return;
        }
        show(error, false);
        jobId = result.data.jobId;
        show(document.getElementById("job"), true);
        document.getElementById("job-report").href = "/cargar/trabajos/" + jobId + "/informe.csv";
        poll();
      });
  });

  function poll() {
    if (!jobId) {
      return;
    }
    fetch("/cargar/trabajos/" + jobId)
      .then(function (response) { return response.json(); })
      .then(paint);
  }

  function paint(job) {
    document.getElementById("job-status").textContent = job.status || "";
    document.getElementById("job-step").textContent = job.step || "";
    document.getElementById("job-counts").textContent =
      "registros " + job.fileCount + " · vectorizados " + job.vectorized + " · escritos " + job.written;
    var body = document.getElementById("job-errors");
    body.replaceChildren();
    (job.errors || []).forEach(function (issue) {
      var row = document.createElement("tr");
      [issue.code, issue.recordLabel, issue.id, issue.field, issue.received, issue.expected].forEach(function (cell) {
        var td = document.createElement("td");
        td.textContent = cell || "";
        row.appendChild(td);
      });
      body.appendChild(row);
    });
    var warnings = document.getElementById("job-warnings");
    warnings.replaceChildren();
    (job.warnings || []).forEach(function (issue) {
      var item = document.createElement("li");
      item.textContent = issue.code + " " + (issue.expected || "");
      warnings.appendChild(item);
    });
    if (job.status === "validando" || job.status === "importando" || job.status === "recibido") {
      window.setTimeout(poll, 1000);
    }
  }

  document.getElementById("job-import").addEventListener("click", function () {
    fetch("/cargar/trabajos/" + jobId + "/importar", { method: "POST" }).then(poll);
  });
  document.getElementById("job-cancel").addEventListener("click", function () {
    fetch("/cargar/trabajos/" + jobId + "/cancelar", { method: "POST" }).then(poll);
  });
}());

(function () {
  var form = document.getElementById("search-form");
  if (!form) {
    return;
  }

  function value(name) {
    return form.querySelector('[name="' + name + '"]').value;
  }

  function setValue(name, next) {
    form.querySelector('[name="' + name + '"]').value = next;
  }

  function show(node, visible) {
    if (visible) {
      node.removeAttribute("hidden");
    } else {
      node.setAttribute("hidden", "");
    }
  }

  function paintPressed(name) {
    var current = value(name);
    form.querySelectorAll('[data-set="' + name + '"]').forEach(function (button) {
      var on = button.getAttribute("data-value") === current;
      button.classList.toggle("is-on", on);
      button.setAttribute("aria-pressed", on ? "true" : "false");
    });
  }

  function suggestionKey() {
    var mode = value("modo");
    if (mode === "vectorial") {
      return "vectorial-" + value("vector");
    }
    if (mode === "hibrida") {
      return "hibrida-" + value("operador");
    }
    var type = value("tipo") === "multi_match" ? "multi_match" : "match";
    var field = type === "match" ? value("campo") : "";
    var middle = type === "match" ? "-" + field : "";
    return "fulltext-" + type + middle + "-" + value("operador");
  }

  function refresh() {
    var mode = value("modo");
    var hybrid = mode === "hibrida";
    var lexical = mode === "fulltext" || hybrid;
    var vector = mode === "vectorial" || hybrid;
    form.querySelectorAll("[data-panel]").forEach(function (panel) {
      var id = panel.getAttribute("data-panel");
      var open = (id === "lexical" && lexical) || (id === "vector" && vector) || (id === "hybrid" && hybrid);
      show(panel, open);
    });
    var match = value("tipo") === "match";
    form.querySelectorAll("[data-when='match']").forEach(function (node) { show(node, match); });
    form.querySelectorAll(".multi-only").forEach(function (node) { show(node, !match); });
    form.querySelectorAll(".hybrid-only").forEach(function (node) { show(node, hybrid); });
    var dims = { plot_256: "256", plot_512: "512", plot_1024: "1024" };
    var label = form.querySelector("[data-dims]");
    if (label) {
      label.textContent = dims[value("vector")] + " dimensiones.";
    }
    var key = suggestionKey();
    document.querySelectorAll(".chip-set").forEach(function (set) {
      show(set, set.getAttribute("data-key") === key);
    });
    document.querySelectorAll(".chip").forEach(keepChosenSettings);
    ["modo", "tipo", "campo", "operador", "vector"].forEach(paintPressed);
    checkKnn();
  }

  function keepChosenSettings(chip) {
    var href = chip.getAttribute("href");
    href = replaceParam(href, "k", value("k"));
    href = replaceParam(href, "candidatos", value("candidatos"));
    href = replaceParam(href, "vector", value("vector"));
    chip.setAttribute("href", href);
  }

  function replaceParam(href, name, next) {
    var pattern = new RegExp("([?&]" + name + "=)[^&]*");
    if (!pattern.test(href)) {
      return href;
    }
    return href.replace(pattern, "$1" + encodeURIComponent(next));
  }

  function checkKnn() {
    var box = document.getElementById("knn-error");
    var k = Number(document.getElementById("knn-k").value);
    var candidates = Number(document.getElementById("knn-cand").value);
    var text = "";
    if (!Number.isInteger(k) || k < 1) {
      text = "k debe ser un entero mayor o igual que 1.";
    } else if (!Number.isInteger(candidates) || candidates > 10000) {
      text = "num_candidates debe ser un entero de máximo 10 000.";
    } else if (candidates < k) {
      text = "num_candidates debe ser mayor o igual que k.";
    }
    box.textContent = text;
    show(box, text !== "");
    var invalid = text !== "";
    document.getElementById("knn-k").setAttribute("aria-invalid", invalid ? "true" : "false");
    document.getElementById("knn-cand").setAttribute("aria-invalid", invalid ? "true" : "false");
    if (text === "") {
      setValue("k", String(k));
      setValue("candidatos", String(candidates));
    }
    return text === "";
  }

  form.querySelectorAll("[data-set]").forEach(function (button) {
    button.addEventListener("click", function () {
      setValue(button.getAttribute("data-set"), button.getAttribute("data-value"));
      refresh();
    });
  });
  document.getElementById("knn-k").addEventListener("input", checkKnn);
  document.getElementById("knn-cand").addEventListener("input", checkKnn);
  form.addEventListener("submit", function (event) {
    var query = document.getElementById("q").value.trim();
    var queryError = document.getElementById("query-error");
    var queryBox = document.getElementById("q");
    if (query === "") {
      event.preventDefault();
      show(queryError, true);
      queryBox.setAttribute("aria-invalid", "true");
      return;
    }
    show(queryError, false);
    queryBox.setAttribute("aria-invalid", "false");
    var mode = value("modo");
    if ((mode === "vectorial" || mode === "hibrida") && !checkKnn()) {
      event.preventDefault();
    }
  });
  refresh();
}());

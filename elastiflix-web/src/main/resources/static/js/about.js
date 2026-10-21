// «Acerca de»: el índice marca la sección que se está leyendo y, en pantallas angostas, se pliega.
(function () {
  var toc = document.querySelector(".toc");
  if (!toc) {
    return;
  }
  var details = document.getElementById("toc-details");
  var narrow = window.matchMedia("(max-width: 1099px)");
  var links = Array.prototype.slice.call(toc.querySelectorAll('a[href^="#"]'));
  // Una sección cuenta como la que se lee cuando su comienzo ya pasó esta línea, medida desde arriba.
  var READING_LINE = 140;
  var pending = false;

  // En pantallas angostas el índice empieza plegado; en escritorio siempre está abierto.
  function fitToScreen() {
    if (details) {
      details.open = !narrow.matches;
    }
  }

  function mark(id) {
    links.forEach(function (link) {
      var on = link.getAttribute("href") === "#" + id;
      link.classList.toggle("is-active", on);
      if (on) {
        link.setAttribute("aria-current", "location");
      } else {
        link.removeAttribute("aria-current");
      }
    });
  }

  var targets = links.map(function (link) {
    return document.getElementById(link.getAttribute("href").slice(1));
  }).filter(Boolean);

  // La última sección cuyo comienzo pasó la línea de lectura; al llegar al final de la página, la última.
  function current() {
    var atBottom = window.innerHeight + window.scrollY >= document.documentElement.scrollHeight - 2;
    if (atBottom) {
      return targets[targets.length - 1];
    }
    var found = targets[0];
    targets.forEach(function (target) {
      if (target.getBoundingClientRect().top <= READING_LINE) {
        found = target;
      }
    });
    return found;
  }

  function update() {
    pending = false;
    var target = current();
    if (target) {
      mark(target.id);
    }
  }

  fitToScreen();
  if (details) {
    // En escritorio el índice no se pliega: si alguien lo cierra con el teclado, se vuelve a abrir.
    details.addEventListener("toggle", function () {
      if (!details.open && !narrow.matches) {
        details.open = true;
      }
    });
  }
  if (narrow.addEventListener) {
    narrow.addEventListener("change", fitToScreen);
  }
  links.forEach(function (link) {
    link.addEventListener("click", function () {
      if (details && narrow.matches) {
        details.open = false;
      }
    });
  });
  window.addEventListener("scroll", function () {
    if (!pending) {
      pending = true;
      window.requestAnimationFrame(update);
    }
  }, { passive: true });
  update();
}());

(function () {
  var POSTER_ICON = '<svg aria-hidden="true" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" '
    + 'stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 4h18v16H3z M7 4v16 M17 4v16 M3 8h4 '
    + 'M3 12h4 M3 16h4 M17 8h4 M17 12h4 M17 16h4"></path></svg>';

  // Reparto: sin JavaScript se ve completo. Aquí se recoge a 18 personas y aparece el botón.
  var toggle = document.getElementById("cast-toggle");
  var castList = document.getElementById("cast-list");
  if (toggle && castList) {
    castList.classList.add("cast-collapsed");
    toggle.hidden = false;
    toggle.addEventListener("click", function () {
      var expanded = toggle.getAttribute("aria-expanded") !== "true";
      castList.classList.toggle("cast-collapsed", !expanded);
      toggle.setAttribute("aria-expanded", expanded ? "true" : "false");
      var textEl = toggle.querySelector(".toggle-text");
      if (textEl) {
        textEl.textContent = toggle.getAttribute(expanded ? "data-show-less" : "data-show-more") || "";
      }
      var iconPath = toggle.querySelector(".toggle-icon");
      if (iconPath) {
        iconPath.setAttribute("d", expanded ? "M6 15l6-6l6 6" : "M6 9l6 6l6-6");
      }
    });
  }

  // Fondo: el CSS no avisa si la imagen falla, así que se precarga y, si da error, se muestra el marcador.
  var backdrop = document.querySelector(".detail-backdrop[data-backdrop]");
  if (backdrop) {
    var probe = new Image();
    probe.addEventListener("error", function () {
      backdrop.style.backgroundImage = "none";
      backdrop.classList.add("missing");
    });
    probe.src = backdrop.getAttribute("data-backdrop");
  }

  var posterImg = document.querySelector(".detail-poster-img");
  if (posterImg) {
    if (posterImg.complete && posterImg.naturalWidth === 0) {
      replaceDetailPoster(posterImg);
    }
    posterImg.addEventListener("error", function () {
      replaceDetailPoster(posterImg);
    });
  }

  // El mismo marcador que pone el servidor cuando no hay poster_path.
  function replaceDetailPoster(image) {
    if (!image.isConnected) {
      return;
    }
    var mark = document.createElement("div");
    mark.className = "detail-poster-missing";
    mark.setAttribute("role", "img");
    mark.setAttribute("aria-label", image.alt || "");
    mark.innerHTML = POSTER_ICON + '<span class="mono-tag">poster_path</span>';
    image.replaceWith(mark);
  }
})();

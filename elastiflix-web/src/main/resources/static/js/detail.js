(function () {
  var POSTER_ICON = '<svg aria-hidden="true" width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" '
    + 'stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"><path d="M3 4h18v16H3z M7 4v16 M17 4v16 M3 8h4 '
    + 'M3 12h4 M3 16h4 M17 8h4 M17 12h4 M17 16h4"></path></svg>';

  var toggle = document.getElementById("cast-toggle");
  if (toggle) {
    toggle.addEventListener("click", function () {
      var isExpanded = toggle.getAttribute("aria-expanded") === "true";
      var nextState = !isExpanded;
      var total = toggle.getAttribute("data-total") || "";
      var extras = document.querySelectorAll(".cast-extra");
      extras.forEach(function (el) {
        el.style.display = nextState ? "inline-flex" : "none";
      });
      toggle.setAttribute("aria-expanded", nextState ? "true" : "false");
      var textEl = toggle.querySelector(".toggle-text");
      if (textEl) {
        textEl.textContent = nextState ? "Ver menos" : "Ver todo el reparto (" + total + ")";
      }
      var iconPath = toggle.querySelector(".toggle-icon");
      if (iconPath) {
        iconPath.setAttribute("d", nextState ? "M18 15l-6-6l-6 6" : "M6 9l6 6l6-6");
      }
    });
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

  function replaceDetailPoster(image) {
    if (!image.isConnected) {
      return;
    }
    var mark = document.createElement("div");
    mark.className = "detail-poster-missing";
    mark.setAttribute("role", "img");
    mark.setAttribute("aria-label", image.alt || "Póster de la película");
    mark.innerHTML = POSTER_ICON + "<span>poster_path</span>";
    image.replaceWith(mark);
  }
})();

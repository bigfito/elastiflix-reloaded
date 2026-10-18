(function () {
  document.querySelectorAll("[data-expand]").forEach(function (button) {
    button.addEventListener("click", function () {
      var box = button.closest("[data-facet]");
      if (box) {
        box.classList.toggle("show-extra");
        button.setAttribute("aria-expanded", box.classList.contains("show-extra") ? "true" : "false");
      }
    });
  });
  var sizes = document.getElementById("page-size");
  if (sizes) {
    sizes.addEventListener("change", function () {
      var chosen = sizes.options[sizes.selectedIndex];
      if (chosen && chosen.dataset.href) {
        window.location.assign(chosen.dataset.href);
      }
    });
  }
  document.querySelectorAll("img.poster").forEach(function (image) {
    if (image.complete && image.naturalWidth === 0) {
      replacePoster(image);
    }
    image.addEventListener("error", function () {
      replacePoster(image);
    });
  });
  var form = document.getElementById("filter-form");
  if (!form) {
    return;
  }
  var initial = new URLSearchParams(new FormData(form)).toString();
  form.addEventListener("change", function () {
    markDirty(form, initial);
    markDates(form);
  });
  form.addEventListener("submit", function (event) {
    if (datesReversed(form)) {
      event.preventDefault();
      markDates(form);
    }
  });
  function replacePoster(image) {
    if (!image.isConnected) {
      return;
    }
    var mark = document.createElement("div");
    mark.className = "poster missing";
    mark.setAttribute("role", "img");
    mark.setAttribute("aria-label", image.alt);
    mark.textContent = "poster_path";
    image.replaceWith(mark);
  }
  function markDirty(form, initial) {
    var dirty = new URLSearchParams(new FormData(form)).toString() !== initial;
    var note = document.getElementById("dirty");
    if (dirty) {
      note.removeAttribute("hidden");
    } else {
      note.setAttribute("hidden", "");
    }
  }

  function markDates(form) {
    var reversed = datesReversed(form);
    var alert = document.getElementById("date-error");
    var apply = form.querySelector(".apply");
    if (reversed) {
      alert.removeAttribute("hidden");
    } else {
      alert.setAttribute("hidden", "");
    }
    apply.disabled = reversed;
    form.querySelector("[name=desde]").setAttribute("aria-invalid", reversed ? "true" : "false");
    form.querySelector("[name=hasta]").setAttribute("aria-invalid", reversed ? "true" : "false");
  }

  function datesReversed(form) {
    var from = form.querySelector("[name=desde]").value;
    var to = form.querySelector("[name=hasta]").value;
    return from !== "" && to !== "" && from > to;
  }
}());
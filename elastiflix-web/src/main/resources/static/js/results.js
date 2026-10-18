(function () {
  document.querySelectorAll("[data-expand]").forEach(function (button) {
    button.addEventListener("click", function () {
      var box = button.closest("[data-facet]");
      if (box) {
        box.classList.toggle("show-extra");
      }
    });
  });
  var form = document.getElementById("filter-form");
  if (!form) {
    return;
  }
  var initial = new URLSearchParams(new FormData(form)).toString();
  form.addEventListener("change", function () {
    var dirty = new URLSearchParams(new FormData(form)).toString() !== initial;
    var note = document.getElementById("dirty");
    if (dirty) {
      note.removeAttribute("hidden");
    } else {
      note.setAttribute("hidden", "");
    }
  });
  document.querySelectorAll(".poster").forEach(function (image) {
    image.addEventListener("error", function () {
      var mark = document.createElement("div");
      mark.className = "poster missing";
      mark.setAttribute("role", "img");
      mark.setAttribute("aria-label", image.alt);
      mark.textContent = "poster_path";
      image.replaceWith(mark);
    });
  });
}());
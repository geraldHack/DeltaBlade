(function () {
  var PAGE_SIZE = 20;
  var page = 1;
  var body = document.getElementById("db-score-body");
  var meta = document.getElementById("db-score-meta");
  var pageLabel = document.getElementById("db-score-page");
  var prev = document.getElementById("db-score-prev");
  var next = document.getElementById("db-score-next");

  if (!body) {
    return;
  }

  function escapeHtml(value) {
    return String(value)
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;");
  }

  function formatScore(score) {
    return String(Math.max(0, Number(score) || 0)).replace(/\B(?=(\d{3})+(?!\d))/g, ".");
  }

  function padWave(wave) {
    var n = Math.max(1, Number(wave) || 1);
    return n < 10 ? "0" + n : String(n);
  }

  function renderEmpty(message) {
    body.innerHTML = '<tr><td colspan="4" class="text-secondary py-3">' + escapeHtml(message) + "</td></tr>";
    meta.textContent = "Keine Einträge";
    pageLabel.textContent = "Seite 1";
    prev.disabled = true;
    next.disabled = true;
  }

  function render(data) {
    var entries = Array.isArray(data.entries) ? data.entries : [];
    var total = Number(data.total) || 0;
    var pages = Math.max(1, Number(data.pages) || 1);
    page = Math.max(1, Number(data.page) || 1);
    var offset = (page - 1) * (Number(data.limit) || PAGE_SIZE);

    if (entries.length === 0) {
      renderEmpty("Noch niemand auf der Liste. Spiel eine Runde, dann steht dein Name hier.");
      return;
    }

    var html = "";
    for (var i = 0; i < entries.length; i++) {
      var rank = offset + i + 1;
      var row = entries[i] || {};
      var klass = rank <= 3 ? " db-top-" + rank : "";
      html +=
        '<tr class="' +
        klass.trim() +
        '">' +
        '<td class="ps-2">' +
        rank +
        "</td>" +
        "<td>" +
        escapeHtml(String(row.name || "—").replace(/\s+$/g, "")) +
        "</td>" +
        '<td class="text-end">' +
        formatScore(row.score) +
        "</td>" +
        '<td class="text-end pe-2">W' +
        padWave(row.wave) +
        "</td>" +
        "</tr>";
    }
    body.innerHTML = html;
    meta.textContent = total === 1 ? "1 Lauf" : total + " Läufe";
    pageLabel.textContent = "Seite " + page + " / " + pages;
    prev.disabled = page <= 1;
    next.disabled = page >= pages;
  }

  function load(nextPage) {
    var url = "/deltablade/scoreboard.php?page=" + encodeURIComponent(nextPage) + "&limit=" + PAGE_SIZE + "&_=" + Date.now();
    fetch(url, { cache: "no-store" })
      .then(function (response) {
        if (!response.ok) {
          throw new Error("status " + response.status);
        }
        return response.json();
      })
      .then(render)
      .catch(function () {
        renderEmpty("Liste gerade nicht erreichbar.");
      });
  }

  prev.addEventListener("click", function () {
    if (page > 1) {
      load(page - 1);
    }
  });
  next.addEventListener("click", function () {
    load(page + 1);
  });

  load(1);
})();

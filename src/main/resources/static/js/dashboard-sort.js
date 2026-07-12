/*
 * Client-side column sorting for the dashboard table. No dependencies.
 *
 * Click a sortable header to sort rows by that column (numeric or text, inferred from the header's
 * data-sort). Click again to reverse. Rows with no value sort last. The placeholder "empty
 * watchlist" row (if present) is left in place.
 */
(function () {
    "use strict";

    var table = document.getElementById("quotes-table");
    if (!table) return;
    var tbody = table.tBodies[0];
    if (!tbody) return;

    var headers = table.tHead.rows[0].cells;
    var sortState = { index: -1, dir: 1 };

    function dataRows() {
        return Array.prototype.filter.call(tbody.rows, function (r) {
            return r.querySelector("td[data-value], td.ticker");
        });
    }

    function cellValue(row, index, type) {
        var cell = row.cells[index];
        if (!cell) return type === "num" ? null : "";
        var raw = cell.getAttribute("data-value");
        if (type === "num") {
            if (raw === null || raw === "") return null;
            var n = parseFloat(raw);
            return isNaN(n) ? null : n;
        }
        return (raw !== null ? raw : cell.textContent).trim().toLowerCase();
    }

    function sortBy(index, type) {
        sortState.dir = sortState.index === index ? -sortState.dir : 1;
        sortState.index = index;
        var dir = sortState.dir;

        var rows = dataRows();
        rows.sort(function (a, b) {
            var va = cellValue(a, index, type);
            var vb = cellValue(b, index, type);
            // nulls always last, regardless of direction
            if (va === null && vb === null) return 0;
            if (va === null) return 1;
            if (vb === null) return -1;
            if (va < vb) return -1 * dir;
            if (va > vb) return 1 * dir;
            return 0;
        });
        rows.forEach(function (r) { tbody.appendChild(r); });

        for (var i = 0; i < headers.length; i++) {
            headers[i].classList.remove("sorted-asc", "sorted-desc");
        }
        headers[index].classList.add(dir === 1 ? "sorted-asc" : "sorted-desc");
    }

    Array.prototype.forEach.call(headers, function (th, index) {
        if (!th.classList.contains("sortable")) return;
        var type = th.getAttribute("data-sort") || "text";
        th.addEventListener("click", function () { sortBy(index, type); });
        th.setAttribute("role", "button");
        th.setAttribute("tabindex", "0");
        th.addEventListener("keydown", function (e) {
            if (e.key === "Enter" || e.key === " ") { e.preventDefault(); sortBy(index, type); }
        });
    });
})();

/*
 * Live dashboard updates over Server-Sent Events. No dependencies.
 *
 * Opens an EventSource to /api/stream/prices and, for each `tick`, updates the matching row's
 * price (and change/% when present) and briefly flashes the cell green/red. The browser's
 * EventSource reconnects automatically if the stream drops.
 */
(function () {
    "use strict";

    var table = document.getElementById("quotes-table");
    if (!table || typeof EventSource === "undefined") return;

    function fmt(n) {
        return Number(n).toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

    function flash(cell, up) {
        if (!cell) return;
        cell.classList.remove("flash-up", "flash-down");
        // force reflow so re-adding the class restarts the animation
        void cell.offsetWidth;
        cell.classList.add(up ? "flash-up" : "flash-down");
    }

    function setUpDown(cell, up) {
        if (!cell) return;
        cell.classList.remove("up", "down");
        cell.classList.add(up ? "up" : "down");
    }

    function applyTick(t) {
        var row = table.querySelector('tbody tr[data-ticker="' + (window.CSS && CSS.escape ? CSS.escape(t.symbol) : t.symbol) + '"]');
        if (!row) return;

        var priceCell = row.querySelector('td[data-field="price"]');
        var up = t.change != null ? t.change >= 0 : true;

        if (priceCell) {
            var prev = parseFloat(priceCell.getAttribute("data-value"));
            if (!isNaN(prev)) {
                up = t.price >= prev; // colour the price flash by its own direction
            }
            priceCell.textContent = fmt(t.price);
            priceCell.setAttribute("data-value", t.price);
            flash(priceCell, up);
        }

        if (t.change != null) {
            var changeCell = row.querySelector('td[data-field="change"]');
            var pctCell = row.querySelector('td[data-field="pct"]');
            var chgUp = t.change >= 0;
            if (changeCell) {
                changeCell.textContent = fmt(t.change);
                changeCell.setAttribute("data-value", t.change);
                setUpDown(changeCell, chgUp);
                flash(changeCell, chgUp);
            }
            if (pctCell && t.percentChange != null) {
                pctCell.textContent = fmt(t.percentChange) + "%";
                pctCell.setAttribute("data-value", t.percentChange);
                setUpDown(pctCell, chgUp);
                var alpha = Math.min(Math.abs(t.percentChange) / 3, 1) * 0.32;
                pctCell.style.backgroundColor = "rgba(" + (chgUp ? "22,163,74" : "220,38,38") + "," + alpha.toFixed(2) + ")";
            }
        }
    }

    function connect() {
        var source = new EventSource("/api/stream/prices");
        source.addEventListener("tick", function (e) {
            try {
                applyTick(JSON.parse(e.data));
            } catch (err) { /* ignore malformed */ }
        });
        source.addEventListener("connected", function () {
            document.body.setAttribute("data-live", "on");
        });
        source.onerror = function () {
            // EventSource retries on its own; mark the state for any indicator.
            document.body.setAttribute("data-live", "reconnecting");
        };
    }

    connect();
})();

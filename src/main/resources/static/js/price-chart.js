/*
 * Dependency-free interactive price-history chart.
 *
 * Fetches /api/stocks/{ticker}/history once (up to 5 years of daily closes), then:
 *   - draws a line/area chart of closing prices onto a <canvas>,
 *   - lets the user zoom the date range via preset buttons (1M / 3M / 6M / 1Y / 5Y),
 *   - shows a crosshair + tooltip with the date and price under the cursor.
 *
 * No external libraries (works fully offline / in a container). Zooming just re-slices the
 * already-loaded data client-side, so it never hits the network again.
 */
(function () {
    "use strict";

    var canvas = document.getElementById("price-chart");
    if (!canvas) return;
    // Capture the intended display height ONCE. draw() overwrites canvas.height (the drawing-buffer
    // size) each render, so we must not read it back or it would compound every redraw.
    var DISPLAY_HEIGHT = parseInt(canvas.getAttribute("height"), 10) || 320;
    var ticker = canvas.getAttribute("data-ticker");
    var emptyNote = document.getElementById("chart-empty");
    var tooltip = document.getElementById("chart-tooltip");
    var rangeBar = document.getElementById("chart-ranges");

    var PAD = { top: 16, right: 16, bottom: 28, left: 60 };
    var RANGES = [
        { label: "1M", days: 30 },
        { label: "3M", days: 91 },
        { label: "6M", days: 182 },
        { label: "1Y", days: 365 },
        { label: "5Y", days: 1826 }
    ];
    var DEFAULT_RANGE = "3M";

    var allPoints = [];   // full dataset, oldest first
    var view = [];        // currently visible slice
    var geom = null;      // {xs:[], mapped for hit-testing}
    var selectedDays = 91;

    function cssVar(name, fallback) {
        var v = getComputedStyle(document.documentElement).getPropertyValue(name);
        return (v && v.trim()) || fallback;
    }

    function fmtPrice(n) {
        return n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

    function sliceForDays(days) {
        if (allPoints.length === 0) return [];
        var last = new Date(allPoints[allPoints.length - 1].date + "T00:00:00");
        var cutoff = new Date(last);
        cutoff.setDate(cutoff.getDate() - days);
        var out = allPoints.filter(function (p) {
            return new Date(p.date + "T00:00:00") >= cutoff;
        });
        return out.length >= 2 ? out : allPoints.slice(-2);
    }

    function draw() {
        var ratio = window.devicePixelRatio || 1;
        var cssWidth = canvas.clientWidth || canvas.parentElement.clientWidth || 640;
        var cssHeight = DISPLAY_HEIGHT;
        canvas.width = cssWidth * ratio;
        canvas.height = cssHeight * ratio;
        // Pin the CSS layout size so the buffer size (attribute) never drives layout.
        canvas.style.width = cssWidth + "px";
        canvas.style.height = cssHeight + "px";

        var ctx = canvas.getContext("2d");
        ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
        ctx.clearRect(0, 0, cssWidth, cssHeight);

        var axis = cssVar("--muted", "#888");
        var line = cssVar("--accent", "#2563eb");
        var up = "#16a34a", down = "#dc2626";

        var closes = view.map(function (p) { return p.close; });
        var min = Math.min.apply(null, closes);
        var max = Math.max.apply(null, closes);
        if (min === max) { min -= 1; max += 1; }
        var range = max - min;

        var plotW = cssWidth - PAD.left - PAD.right;
        var plotH = cssHeight - PAD.top - PAD.bottom;

        function x(i) { return PAD.left + (view.length === 1 ? plotW / 2 : (i / (view.length - 1)) * plotW); }
        function y(v) { return PAD.top + plotH - ((v - min) / range) * plotH; }

        // Y grid + labels
        ctx.fillStyle = axis;
        ctx.font = "11px system-ui, sans-serif";
        ctx.textAlign = "right";
        ctx.textBaseline = "middle";
        [min, (min + max) / 2, max].forEach(function (v) {
            var gy = y(v);
            ctx.strokeStyle = axis;
            ctx.globalAlpha = 0.15;
            ctx.beginPath(); ctx.moveTo(PAD.left, gy); ctx.lineTo(cssWidth - PAD.right, gy); ctx.stroke();
            ctx.globalAlpha = 0.7;
            ctx.fillText(fmtPrice(v), PAD.left - 8, gy);
        });
        ctx.globalAlpha = 1;

        // X labels: first and last date
        ctx.textBaseline = "top";
        ctx.globalAlpha = 0.7;
        ctx.textAlign = "left";
        ctx.fillText(view[0].date, PAD.left, cssHeight - PAD.bottom + 8);
        ctx.textAlign = "right";
        ctx.fillText(view[view.length - 1].date, cssWidth - PAD.right, cssHeight - PAD.bottom + 8);
        ctx.globalAlpha = 1;

        // Area fill
        ctx.beginPath();
        ctx.moveTo(x(0), y(closes[0]));
        for (var i = 1; i < view.length; i++) ctx.lineTo(x(i), y(closes[i]));
        ctx.lineTo(x(view.length - 1), PAD.top + plotH);
        ctx.lineTo(x(0), PAD.top + plotH);
        ctx.closePath();
        ctx.globalAlpha = 0.08;
        ctx.fillStyle = line;
        ctx.fill();
        ctx.globalAlpha = 1;

        // Price line
        ctx.beginPath();
        ctx.moveTo(x(0), y(closes[0]));
        for (var j = 1; j < view.length; j++) ctx.lineTo(x(j), y(closes[j]));
        ctx.strokeStyle = line;
        ctx.lineWidth = 2;
        ctx.lineJoin = "round";
        ctx.stroke();

        // Last-point marker, coloured by direction over the visible range
        var lastX = x(view.length - 1), lastY = y(closes[closes.length - 1]);
        ctx.beginPath();
        ctx.arc(lastX, lastY, 3.5, 0, Math.PI * 2);
        ctx.fillStyle = closes[closes.length - 1] >= closes[0] ? up : down;
        ctx.fill();

        // Save geometry for hit-testing
        var xs = view.map(function (_, k) { return x(k); });
        var ys = closes.map(function (v) { return y(v); });
        geom = { xs: xs, ys: ys, plotTop: PAD.top, plotBottom: PAD.top + plotH, line: line };
    }

    function nearestIndex(mouseX) {
        if (!geom) return -1;
        var lo = 0, best = 0, bestD = Infinity;
        for (var i = 0; i < geom.xs.length; i++) {
            var d = Math.abs(geom.xs[i] - mouseX);
            if (d < bestD) { bestD = d; best = i; }
        }
        return best;
    }

    function drawCrosshair(index) {
        // Redraw base, then overlay the crosshair for `index`.
        draw();
        if (index < 0 || !geom) return;
        var ctx = canvas.getContext("2d");
        var px = geom.xs[index], py = geom.ys[index];

        ctx.save();
        ctx.strokeStyle = cssVar("--muted", "#888");
        ctx.globalAlpha = 0.5;
        ctx.setLineDash([4, 4]);
        ctx.beginPath();
        ctx.moveTo(px, geom.plotTop);
        ctx.lineTo(px, geom.plotBottom);
        ctx.stroke();
        ctx.restore();

        ctx.beginPath();
        ctx.arc(px, py, 4, 0, Math.PI * 2);
        ctx.fillStyle = geom.line;
        ctx.fill();
        ctx.lineWidth = 2;
        ctx.strokeStyle = cssVar("--bg", "#fff");
        ctx.stroke();
    }

    function showTooltip(index, clientX) {
        if (!tooltip || index < 0) return;
        var p = view[index];
        tooltip.innerHTML = '<span class="tt-price">' + fmtPrice(p.close) + '</span>' +
            '<span class="tt-date">' + p.date + '</span>';
        tooltip.hidden = false;
        var wrap = canvas.parentElement.getBoundingClientRect();
        var left = geom.xs[index];
        // keep tooltip inside the chart width
        var tw = tooltip.offsetWidth;
        left = Math.max(4, Math.min(left - tw / 2, wrap.width - tw - 4));
        tooltip.style.left = left + "px";
        tooltip.style.top = (geom.plotTop) + "px";
    }

    function hideTooltip() {
        if (tooltip) tooltip.hidden = true;
    }

    function onMove(evt) {
        if (view.length === 0) return;
        var rect = canvas.getBoundingClientRect();
        var mouseX = evt.clientX - rect.left;
        var idx = nearestIndex(mouseX);
        drawCrosshair(idx);
        showTooltip(idx, evt.clientX);
    }

    function selectRange(days, btn) {
        selectedDays = days;
        view = sliceForDays(days);
        if (rangeBar) {
            Array.prototype.forEach.call(rangeBar.children, function (b) { b.classList.remove("active"); });
            if (btn) btn.classList.add("active");
        }
        draw();
        hideTooltip();
    }

    function buildRangeButtons() {
        if (!rangeBar) return;
        RANGES.forEach(function (r) {
            var b = document.createElement("button");
            b.type = "button";
            b.textContent = r.label;
            b.className = "range-btn" + (r.label === DEFAULT_RANGE ? " active" : "");
            b.addEventListener("click", function () { selectRange(r.days, b); });
            rangeBar.appendChild(b);
        });
    }

    function showEmpty() {
        canvas.hidden = true;
        if (rangeBar) rangeBar.hidden = true;
        if (emptyNote) emptyNote.hidden = false;
    }

    fetch("/api/stocks/" + encodeURIComponent(ticker) + "/history")
        .then(function (r) { return r.ok ? r.json() : []; })
        .then(function (points) {
            if (!points || points.length === 0) { showEmpty(); return; }
            allPoints = points;
            buildRangeButtons();
            view = sliceForDays(selectedDays);
            draw();
            canvas.addEventListener("mousemove", onMove);
            canvas.addEventListener("mouseleave", function () { draw(); hideTooltip(); });
        })
        .catch(showEmpty);

    var resizeTimer;
    window.addEventListener("resize", function () {
        if (view.length === 0) return;
        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(draw, 150);
    });
})();

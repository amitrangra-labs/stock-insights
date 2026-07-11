/*
 * Dependency-free price-history chart.
 *
 * Fetches /api/stocks/{ticker}/history and draws a line chart of closing prices onto a <canvas>.
 * No external libraries (works fully offline / in a container with no internet). Redraws on resize
 * and adapts to the device pixel ratio for crisp lines and to light/dark via CSS variables.
 */
(function () {
    "use strict";

    var canvas = document.getElementById("price-chart");
    if (!canvas) return;
    var ticker = canvas.getAttribute("data-ticker");
    var emptyNote = document.getElementById("chart-empty");

    var PAD = { top: 16, right: 16, bottom: 28, left: 56 };

    function cssVar(name, fallback) {
        var v = getComputedStyle(document.documentElement).getPropertyValue(name);
        return (v && v.trim()) || fallback;
    }

    function fmtPrice(n) {
        return n.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    }

    function draw(points) {
        var ratio = window.devicePixelRatio || 1;
        var cssWidth = canvas.clientWidth || canvas.parentElement.clientWidth || 640;
        var cssHeight = parseInt(canvas.getAttribute("height"), 10) || 320;
        canvas.width = cssWidth * ratio;
        canvas.height = cssHeight * ratio;

        var ctx = canvas.getContext("2d");
        ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
        ctx.clearRect(0, 0, cssWidth, cssHeight);

        var axis = cssVar("--muted", "#888");
        var line = cssVar("--accent", "#2563eb");
        var up = "#16a34a", down = "#dc2626";

        var closes = points.map(function (p) { return p.close; });
        var min = Math.min.apply(null, closes);
        var max = Math.max.apply(null, closes);
        if (min === max) { min -= 1; max += 1; }
        var range = max - min;

        var plotW = cssWidth - PAD.left - PAD.right;
        var plotH = cssHeight - PAD.top - PAD.bottom;

        function x(i) { return PAD.left + (points.length === 1 ? plotW / 2 : (i / (points.length - 1)) * plotW); }
        function y(v) { return PAD.top + plotH - ((v - min) / range) * plotH; }

        // Y grid + labels (min, mid, max)
        ctx.strokeStyle = axis;
        ctx.fillStyle = axis;
        ctx.globalAlpha = 0.6;
        ctx.font = "11px system-ui, sans-serif";
        ctx.textAlign = "right";
        ctx.textBaseline = "middle";
        [min, (min + max) / 2, max].forEach(function (v) {
            var gy = y(v);
            ctx.globalAlpha = 0.15;
            ctx.beginPath(); ctx.moveTo(PAD.left, gy); ctx.lineTo(cssWidth - PAD.right, gy); ctx.stroke();
            ctx.globalAlpha = 0.7;
            ctx.fillText(fmtPrice(v), PAD.left - 8, gy);
        });
        ctx.globalAlpha = 1;

        // X labels: first and last date
        ctx.textAlign = "left";
        ctx.textBaseline = "top";
        ctx.globalAlpha = 0.7;
        ctx.fillText(points[0].date, PAD.left, cssHeight - PAD.bottom + 8);
        ctx.textAlign = "right";
        ctx.fillText(points[points.length - 1].date, cssWidth - PAD.right, cssHeight - PAD.bottom + 8);
        ctx.globalAlpha = 1;

        // Area fill
        ctx.beginPath();
        ctx.moveTo(x(0), y(closes[0]));
        for (var i = 1; i < points.length; i++) ctx.lineTo(x(i), y(closes[i]));
        ctx.lineTo(x(points.length - 1), PAD.top + plotH);
        ctx.lineTo(x(0), PAD.top + plotH);
        ctx.closePath();
        ctx.globalAlpha = 0.08;
        ctx.fillStyle = line;
        ctx.fill();
        ctx.globalAlpha = 1;

        // Price line
        ctx.beginPath();
        ctx.moveTo(x(0), y(closes[0]));
        for (var j = 1; j < points.length; j++) ctx.lineTo(x(j), y(closes[j]));
        ctx.strokeStyle = line;
        ctx.lineWidth = 2;
        ctx.lineJoin = "round";
        ctx.stroke();

        // Last point marker, coloured by overall direction
        var lastX = x(points.length - 1), lastY = y(closes[closes.length - 1]);
        ctx.beginPath();
        ctx.arc(lastX, lastY, 3.5, 0, Math.PI * 2);
        ctx.fillStyle = closes[closes.length - 1] >= closes[0] ? up : down;
        ctx.fill();
    }

    function showEmpty() {
        canvas.hidden = true;
        if (emptyNote) emptyNote.hidden = false;
    }

    var loaded = null;
    fetch("/api/stocks/" + encodeURIComponent(ticker) + "/history")
        .then(function (r) { return r.ok ? r.json() : []; })
        .then(function (points) {
            if (!points || points.length === 0) { showEmpty(); return; }
            loaded = points;
            draw(points);
        })
        .catch(showEmpty);

    var resizeTimer;
    window.addEventListener("resize", function () {
        if (!loaded) return;
        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(function () { draw(loaded); }, 150);
    });
})();

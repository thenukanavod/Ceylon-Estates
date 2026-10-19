// Draws the two "Market data" canvases on the Sell your property page: a
// line chart (.md-line-chart) and a grouped bar chart (.md-bar-chart).
// Each canvas carries its own data as data-* attributes, so this script is
// generic - it doesn't know anything about house/apartment prices itself.
// No external charting library: plain canvas, crisp on retina screens.
(function () {

    function parseConfig(canvas) {
        return {
            labels: JSON.parse(canvas.dataset.labels || '[]'),
            seriesA: JSON.parse(canvas.dataset.seriesA || '[]'),
            seriesB: JSON.parse(canvas.dataset.seriesB || '[]'),
            colorA: canvas.dataset.colorA || '#19555a',
            colorB: canvas.dataset.colorB || '#9a9a9a',
            prefix: canvas.dataset.prefix || '',
            unit: canvas.dataset.unit || '',
            divisor: parseFloat(canvas.dataset.divisor || '1')
        };
    }

    // Sizes the canvas's backing store for the screen's pixel ratio so
    // lines and text stay sharp, while the CSS size is set separately.
    function fitCanvas(canvas) {
        var rect = canvas.getBoundingClientRect();
        var ratio = window.devicePixelRatio || 1;
        var w = Math.max(rect.width, 280);
        var h = Math.max(rect.height, 180);
        canvas.width = w * ratio;
        canvas.height = h * ratio;
        var ctx = canvas.getContext('2d');
        ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
        return { ctx: ctx, w: w, h: h };
    }

    function niceMax(value) {
        if (value <= 0) { return 1; }
        var pow = Math.pow(10, Math.floor(Math.log10(value)));
        var n = value / pow;
        var step = n <= 1 ? 1 : n <= 2 ? 2 : n <= 5 ? 5 : 10;
        return step * pow;
    }

    function formatAxis(value, cfg) {
        var v = value / cfg.divisor;
        var rounded = Math.round(v * 10) / 10;
        return cfg.prefix + rounded + cfg.unit;
    }

    // Draws x-axis tick labels without crowding: skips ticks when there
    // isn't room for all of them, and keeps the first/last labels fully
    // inside the canvas instead of overflowing past the edges.
    function drawXLabels(ctx, labels, xOf, y, availableWidth) {
        var n = labels.length;
        var minSlot = 44; // px needed per label at this font size, with a little breathing room
        var maxTicks = Math.max(2, Math.floor(availableWidth / minSlot));
        var stride = Math.max(1, Math.ceil(n / maxTicks));

        // Build the set of indices to show: 0, stride, 2*stride, ... plus
        // the last one - but drop a middle tick if it would land right next
        // to the last tick and overlap it.
        var indices = [];
        for (var i = 0; i < n; i += stride) { indices.push(i); }
        if (indices[indices.length - 1] !== n - 1) {
            if (n - 1 - indices[indices.length - 1] < stride && indices.length > 1) {
                indices.pop();
            }
            indices.push(n - 1);
        }

        ctx.font = '10px Montserrat, sans-serif';
        ctx.fillStyle = '#8a8a8a';
        ctx.textBaseline = 'top';

        indices.forEach(function (i) {
            var x = xOf(i);
            // Left/right-align the very first/last tick so its text grows
            // inward, rather than spilling past the canvas edge the way a
            // centred label would.
            if (i === 0) { ctx.textAlign = 'left'; }
            else if (i === n - 1) { ctx.textAlign = 'right'; }
            else { ctx.textAlign = 'center'; }
            ctx.fillText(labels[i], x, y);
        });
    }

    function drawLineChart(canvas) {
        var cfg = parseConfig(canvas);
        var fit = fitCanvas(canvas), ctx = fit.ctx, w = fit.w, h = fit.h;
        ctx.clearRect(0, 0, w, h);

        var padL = 54, padR = 10, padT = 10, padB = 26;
        var plotW = w - padL - padR, plotH = h - padT - padB;

        var allValues = cfg.seriesA.concat(cfg.seriesB);
        var maxVal = niceMax(Math.max.apply(null, allValues) * 1.08);
        var minVal = 0;
        var steps = 4;

        ctx.font = '11px Montserrat, sans-serif';
        ctx.fillStyle = '#8a8a8a';
        ctx.strokeStyle = 'rgba(0,0,0,0.08)';
        ctx.lineWidth = 1;

        // Gridlines + y-axis labels
        for (var s = 0; s <= steps; s++) {
            var val = minVal + (maxVal - minVal) * (s / steps);
            var y = padT + plotH - (plotH * (s / steps));
            ctx.beginPath();
            ctx.moveTo(padL, y);
            ctx.lineTo(w - padR, y);
            ctx.stroke();
            ctx.textAlign = 'right';
            ctx.textBaseline = 'middle';
            ctx.fillText(formatAxis(val, cfg), padL - 8, y);
        }

        var n = cfg.labels.length;
        var xOf = function (i) { return padL + (n === 1 ? plotW / 2 : (plotW * i) / (n - 1)); };
        drawXLabels(ctx, cfg.labels, xOf, padT + plotH + 8, plotW);

        function plotSeries(series, color) {
            ctx.beginPath();
            series.forEach(function (val, i) {
                var x = padL + (n === 1 ? plotW / 2 : (plotW * i) / (n - 1));
                var y = padT + plotH - (plotH * ((val - minVal) / (maxVal - minVal)));
                if (i === 0) { ctx.moveTo(x, y); } else { ctx.lineTo(x, y); }
            });
            ctx.strokeStyle = color;
            ctx.lineWidth = 2.5;
            ctx.lineJoin = 'round';
            ctx.stroke();

            series.forEach(function (val, i) {
                var x = padL + (n === 1 ? plotW / 2 : (plotW * i) / (n - 1));
                var y = padT + plotH - (plotH * ((val - minVal) / (maxVal - minVal)));
                ctx.beginPath();
                ctx.arc(x, y, 3, 0, Math.PI * 2);
                ctx.fillStyle = color;
                ctx.fill();
            });
        }

        plotSeries(cfg.seriesA, cfg.colorA);
        plotSeries(cfg.seriesB, cfg.colorB);
    }

    function drawBarChart(canvas) {
        var cfg = parseConfig(canvas);
        var fit = fitCanvas(canvas), ctx = fit.ctx, w = fit.w, h = fit.h;
        ctx.clearRect(0, 0, w, h);

        var padL = 54, padR = 10, padT = 10, padB = 26;
        var plotW = w - padL - padR, plotH = h - padT - padB;

        var allValues = cfg.seriesA.concat(cfg.seriesB);
        var maxVal = niceMax(Math.max.apply(null, allValues) * 1.15);
        var steps = 4;

        ctx.font = '11px Montserrat, sans-serif';
        ctx.fillStyle = '#8a8a8a';
        ctx.strokeStyle = 'rgba(0,0,0,0.08)';
        ctx.lineWidth = 1;

        for (var s = 0; s <= steps; s++) {
            var val = (maxVal * s) / steps;
            var y = padT + plotH - (plotH * (s / steps));
            ctx.beginPath();
            ctx.moveTo(padL, y);
            ctx.lineTo(w - padR, y);
            ctx.stroke();
            ctx.textAlign = 'right';
            ctx.textBaseline = 'middle';
            ctx.fillText(formatAxis(val, cfg), padL - 8, y);
        }

        var n = cfg.labels.length;
        var groupW = plotW / n;
        var barW = Math.min(14, groupW * 0.32);
        var gap = 3;
        var groupCenter = function (i) { return padL + groupW * (i + 0.5); };

        cfg.labels.forEach(function (label, i) {
            var gc = groupCenter(i);
            var aVal = cfg.seriesA[i], bVal = cfg.seriesB[i];
            var aH = plotH * (aVal / maxVal);
            var bH = plotH * (bVal / maxVal);

            ctx.fillStyle = cfg.colorA;
            ctx.fillRect(gc - barW - gap / 2, padT + plotH - aH, barW, aH);
            ctx.fillStyle = cfg.colorB;
            ctx.fillRect(gc + gap / 2, padT + plotH - bH, barW, bH);
        });

        drawXLabels(ctx, cfg.labels, groupCenter, padT + plotH + 8, plotW);
    }

    function renderAll() {
        document.querySelectorAll('.md-line-chart').forEach(drawLineChart);
        document.querySelectorAll('.md-bar-chart').forEach(drawBarChart);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', renderAll);
    } else {
        renderAll();
    }
    // Redraw on resize so the canvases stay crisp at their new CSS size
    // (debounced so it doesn't redraw on every pixel while dragging).
    var resizeTimer;
    window.addEventListener('resize', function () {
        clearTimeout(resizeTimer);
        resizeTimer = setTimeout(renderAll, 150);
    });
})();

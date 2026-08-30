(function () {
    const namespace = "http://www.w3.org/2000/svg";

    function create({series, from, to, unit = "", ariaLabel = "Sensor readings", width = 800, height = 250,
                     safeMin = null, safeMax = null, showSafeZone = false}) {
        const normalized = series.map(item => ({...item, points: item.readings.map(reading => ({
            time: new Date(reading.measuredAt).getTime(), value: Number(reading.value),
            safeMin: reading.safeMin ?? item.safeMin ?? safeMin,
            safeMax: reading.safeMax ?? item.safeMax ?? safeMax
        })).filter(point => Number.isFinite(point.time) && Number.isFinite(point.value)).sort((a, b) => a.time - b.time)}))
            .filter(item => item.points.length);
        if (!normalized.length) return null;

        const points = normalized.flatMap(item => item.points);
        const bounds = points.flatMap(point => [point.value, point.safeMin, point.safeMax])
            .filter(value => value != null).map(Number).filter(Number.isFinite);
        let minimum = Math.min(...bounds), maximum = Math.max(...bounds);
        const padding = minimum === maximum ? Math.max(Math.abs(minimum) * .1, 1) : (maximum - minimum) * .12;
        minimum -= padding; maximum += padding;
        const left = 58, right = 20, top = 18, bottom = 36;
        const plotWidth = width - left - right, plotHeight = height - top - bottom;
        const first = from == null ? Math.min(...points.map(point => point.time)) : new Date(from).getTime();
        const requestedLast = to == null ? Math.max(...points.map(point => point.time)) : new Date(to).getTime();
        const last = Math.max(requestedLast, first + 1000);
        const x = time => left + (time - first) / (last - first) * plotWidth;
        const y = value => top + (maximum - Number(value)) / (maximum - minimum) * plotHeight;
        const svg = document.createElementNS(namespace, "svg");
        svg.setAttribute("viewBox", `0 0 ${width} ${height}`); svg.setAttribute("role", "img"); svg.setAttribute("aria-label", ariaLabel);
        if (showSafeZone && safeMin != null && safeMax != null) {
            const zone = document.createElementNS(namespace, "rect");
            zone.setAttribute("x", left); zone.setAttribute("y", y(safeMax)); zone.setAttribute("width", plotWidth);
            zone.setAttribute("height", Math.max(0, y(safeMin) - y(safeMax))); zone.setAttribute("class", "monitor-chart-safe-zone"); svg.append(zone);
        }
        normalized.forEach(item => {
            const line = document.createElementNS(namespace, "polyline");
            line.setAttribute("points", item.points.map(point => `${x(point.time)},${y(point.value)}`).join(" "));
            if (item.color) { line.setAttribute("fill", "none"); line.setAttribute("stroke", item.color); line.setAttribute("stroke-width", "3"); }
            else line.setAttribute("class", "monitor-chart-line");
            svg.append(line);
            item.points.forEach(point => {
                const dot = document.createElementNS(namespace, "circle");
                dot.setAttribute("cx", x(point.time)); dot.setAttribute("cy", y(point.value)); dot.setAttribute("r", item.pointRadius || 3);
                const outside = point.safeMin != null && point.value < Number(point.safeMin) || point.safeMax != null && point.value > Number(point.safeMax);
                if (item.color) dot.setAttribute("fill", outside ? "#dc2626" : item.color);
                else dot.setAttribute("class", outside ? "monitor-chart-point monitor-chart-point-alert" : "monitor-chart-point");
                svg.append(dot);
            });
        });
        addLabel(svg, 8, top + 5, `${maximum.toFixed(2)}${unit ? ` ${unit}` : ""}`);
        addLabel(svg, 8, top + plotHeight, `${minimum.toFixed(2)}${unit ? ` ${unit}` : ""}`);
        addLabel(svg, left, height - 10, formatTime(first)); addLabel(svg, width - right, height - 10, formatTime(last), "end");
        return svg;
    }

    function addLabel(svg, x, y, text, anchor = "start") {
        const label = document.createElementNS(namespace, "text"); label.setAttribute("x", x); label.setAttribute("y", y);
        label.setAttribute("text-anchor", anchor); label.setAttribute("class", "monitor-chart-label"); label.textContent = text; svg.append(label);
    }

    function formatTime(value) { return new Date(value).toLocaleTimeString([], {hour: "2-digit", minute: "2-digit"}); }
    window.LabMonitorCharts = {createTimeSeriesChart: create};
})();

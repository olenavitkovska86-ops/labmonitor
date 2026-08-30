(function () {
    const colors = ["#16778f", "#7c3aed", "#ea580c", "#059669", "#be123c", "#4f46e5"];

    function create(canvas, timeline, readings, {showLegend = false} = {}) {
        const from = new Date(timeline.from).getTime();
        const to = Math.max(new Date(timeline.to).getTime(), from + 1000);
        const values = readings.flatMap(reading => [reading.value, reading.safeMin, reading.safeMax])
            .filter(value => value != null).map(Number).filter(Number.isFinite);
        let minimum = Math.min(...values), maximum = Math.max(...values);
        if (minimum === maximum) { minimum -= 1; maximum += 1; }
        const padding = (maximum - minimum) * .08; minimum -= padding; maximum += padding;
        const bySensor = new Map();
        readings.forEach(reading => {
            if (!bySensor.has(reading.sensorId)) bySensor.set(reading.sensorId, []);
            bySensor.get(reading.sensorId).push(reading);
        });
        const datasets = [...bySensor.values()].map((sensorReadings, index) => readingDataset(sensorReadings, colors[index % colors.length]));
        timeline.events.forEach(event => datasets.push(markerDataset(event.occurredAt, minimum, maximum, "#d97706", [5, 4],
            `${humanize(event.category)}: ${event.title}`, from, to)));
        const sensorIds = new Set(bySensor.keys());
        timeline.alerts.filter(alert => alert.sensorId == null || sensorIds.has(alert.sensorId)).forEach(alert => {
            const originalTime = alert.violationStartedAt || alert.createdAt;
            const markerTime = new Date(originalTime).getTime() < from ? timeline.from : originalTime;
            datasets.push(markerDataset(markerTime, minimum, maximum, "#b42318", [2, 3], `${alert.severity}: ${alert.title}`, from, to));
        });
        return new Chart(canvas, {type: "line", data: {datasets}, options: {
            responsive: true, maintainAspectRatio: false, animation: false,
            interaction: {mode: "nearest", intersect: false},
            scales: {
                x: {type: "linear", min: from, max: to, grid: {color: "#e5eaed"}, ticks: {callback: value => new Date(value).toLocaleTimeString()}},
                y: {min: minimum, max: maximum, grid: {color: "#e5eaed"}}
            },
            plugins: {
                legend: {display: showLegend, labels: {filter: item => datasets[item.datasetIndex]?.timelineKind === "reading", usePointStyle: true}},
                tooltip: {callbacks: {
                    title: items => items.length ? new Intl.DateTimeFormat(undefined, {dateStyle: "medium", timeStyle: "medium"}).format(new Date(items[0].parsed.x)) : "",
                    label: item => item.dataset.timelineKind === "marker" ? item.dataset.markerLabel
                        : `${item.dataset.label}: ${item.parsed.y}${item.raw.reading.unit ? ` ${item.raw.reading.unit}` : ""}${item.raw.reading.status === "OUTSIDE_RANGE" ? " · Outside safe range" : ""}`
                }}
            }
        }});
    }

    function readingDataset(readings, color) {
        const data = readings.slice().sort((a, b) => new Date(a.measuredAt) - new Date(b.measuredAt))
            .map(reading => ({x: new Date(reading.measuredAt).getTime(), y: Number(reading.value), reading}));
        return {label: readings[0].sensorName, data, parsing: false, borderColor: color, borderWidth: 2.5,
            pointBackgroundColor: data.map(point => point.reading.status === "OUTSIDE_RANGE" ? "#b42318" : color),
            pointBorderColor: data.map(point => point.reading.status === "OUTSIDE_RANGE" ? "#fff" : color),
            pointBorderWidth: data.map(point => point.reading.status === "OUTSIDE_RANGE" ? 1.5 : 0),
            pointRadius: data.map(point => point.reading.status === "OUTSIDE_RANGE" ? 4 : 2.5), pointHoverRadius: 6,
            tension: 0, fill: false, timelineKind: "reading"};
    }

    function markerDataset(time, minimum, maximum, color, borderDash, label, from, to) {
        const timestamp = new Date(time).getTime();
        if (timestamp < from || timestamp > to) return {data: [], timelineKind: "marker"};
        return {label, data: [{x: timestamp, y: minimum}, {x: timestamp, y: maximum}], parsing: false,
            borderColor: color, borderWidth: 2, borderDash, pointRadius: [0, 4], pointHoverRadius: [0, 6],
            fill: false, timelineKind: "marker", markerLabel: label};
    }

    function humanize(value) { return String(value || "Event").toLowerCase().replaceAll("_", " ").replace(/^./, letter => letter.toUpperCase()); }
    window.LabMonitorSessionChart = {create};
})();

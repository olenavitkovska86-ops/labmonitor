const historyOrganization = document.querySelector("#history-organization");
const exportRoom = document.querySelector("#export-room");
const exportSensor = document.querySelector("#export-sensor");
const historyMessage = document.querySelector("#history-message");
const historyLoading = document.querySelector("#history-loading");
let selectedPeriod = "LAST_7_DAYS";
let roomsById = new Map();
let organizationAlerts = null;

async function initializeHistory() {
    setDefaultRange();
    try {
        await labMonitorAuthReady;
        const organizations = await apiRequest("/api/organizations");
        historyOrganization.replaceChildren(...organizations.map(item => option(item.id, item.name)));
        if (!organizations.length) {
            historyLoading.textContent = "No organizations are available for your account.";
            return;
        }
        historyOrganization.disabled = organizations.length === 1;
        await loadOrganization();
    } catch (error) { showHistoryMessage(error.message, true); }
}

async function loadOrganization() {
    historyLoading.classList.remove("hidden");
    hideHistoryMessage();
    try {
        const organizationId = historyOrganization.value;
        organizationAlerts = null;
        closeSelectedDay();
        const labs = await apiRequest(`/api/labs?organizationId=${organizationId}`);
        const roomLists = await Promise.all(labs.map(lab => apiRequest(`/api/rooms?labId=${lab.id}`)));
        const rooms = roomLists.flat();
        roomsById = new Map(rooms.map(room => [room.id, room]));
        exportRoom.replaceChildren(option("", "Select a room"), ...rooms.map(room => {
            const lab = labs.find(item => item.id === room.labId);
            return option(room.id, `${lab?.name || "Lab"} / ${room.name}`);
        }));
        exportRoom.disabled = rooms.length === 0;
        exportSensor.replaceChildren(option("", "All sensors in room"));
        exportSensor.disabled = true;
        await loadAlertHistory();
    } catch (error) { showHistoryMessage(error.message, true); }
    finally { historyLoading.classList.add("hidden"); }
}

async function loadAlertHistory() {
    const history = await apiRequest(`/api/analytics/organizations/${historyOrganization.value}/history?period=${selectedPeriod}`);
    renderStatus(history);
    renderHistoryChart(history.dailyAlerts);
    renderHistoryRooms(history.mostProblematicRooms);
}

function renderStatus(history) {
    const values = [[history.alertsCreated, "alerts created"], [history.criticalAlerts, "critical alerts"],
        [formatDuration(history.averageAcknowledgementMinutes), "average response"],
        [formatDuration(history.averageResolutionMinutes), "average resolution"]];
    document.querySelector("#history-status").replaceChildren(...values.map(([value, label]) => {
        const item = document.createElement("div");
        const term = document.createElement("dt"); term.textContent = label;
        const description = document.createElement("dd"); description.textContent = value;
        item.append(description, term); return item;
    }));
}

function renderHistoryChart(days) {
    const chart = document.querySelector("#history-chart");
    const maximum = Math.max(1, ...days.map(day => day.alerts));
    chart.replaceChildren(...days.map(day => {
        const item = document.createElement("button");
        item.type = "button";
        item.className = "history-day history-day-button";
        item.title = `${day.alerts} alerts, ${day.criticalAlerts} critical`;
        item.setAttribute("aria-label", `${formatLongDay(day.date)}: ${day.alerts} alerts, ${day.criticalAlerts} critical`);
        item.addEventListener("click", () => showDayAlerts(day.date, item));
        const count = document.createElement("strong"); count.textContent = day.alerts;
        const track = document.createElement("div"); track.className = "history-bar-track";
        const bar = document.createElement("div");
        bar.className = day.criticalAlerts ? "history-bar history-bar-critical" : "history-bar";
        bar.style.height = `${Math.max(day.alerts ? 12 : 2, day.alerts / maximum * 100)}%`;
        track.append(bar);
        const label = document.createElement("span"); label.textContent = formatDay(day.date, days.length);
        item.append(count, track, label); return item;
    }));
}

async function showDayAlerts(date, selectedBar) {
    document.querySelectorAll(".history-day-button").forEach(item => item.classList.remove("history-day-selected"));
    selectedBar.classList.add("history-day-selected");
    const panel = document.querySelector("#selected-day-alerts");
    const list = document.querySelector("#selected-day-list");
    panel.classList.remove("hidden");
    document.querySelector("#selected-day-title").textContent = `Alerts on ${formatLongDay(date)}`;
    list.replaceChildren();
    document.querySelector("#selected-day-empty").classList.add("hidden");
    try {
        organizationAlerts ||= await apiRequest(`/api/alerts?organizationId=${historyOrganization.value}`);
        const alerts = organizationAlerts
            .filter(alert => alert.createdAt?.slice(0, 10) === date)
            .sort((left, right) => right.createdAt.localeCompare(left.createdAt));
        document.querySelector("#selected-day-empty").classList.toggle("hidden", alerts.length !== 0);
        list.replaceChildren(...alerts.map(renderDayAlert));
    } catch (error) { showHistoryMessage(error.message, true); }
}

function renderDayAlert(alert) {
    const room = roomsById.get(alert.roomId);
    const row = document.createElement("article");
    row.className = "day-alert-row";
    const identity = document.createElement("div");
    identity.className = "day-alert-identity";
    const roomName = document.createElement("strong");
    roomName.textContent = room?.name || `Room ${alert.roomId}`;
    const time = document.createElement("span");
    time.textContent = new Date(alert.createdAt).toLocaleTimeString([], {hour: "2-digit", minute: "2-digit"});
    identity.append(roomName, time);
    const severity = document.createElement("span");
    severity.className = `priority-badge priority-${alert.severity.toLowerCase()}`;
    severity.textContent = alert.severity;
    const issue = document.createElement("div");
    issue.className = "day-alert-detail";
    const title = document.createElement("strong");
    title.textContent = alert.title;
    const status = document.createElement("span");
    status.textContent = alert.status;
    issue.append(title, status);
    const link = document.createElement("a");
    link.className = "button button-secondary button-small";
    link.href = `/alerts.html?alertId=${alert.id}`;
    link.textContent = "View alert";
    row.append(identity, severity, issue, link);
    return row;
}

function closeSelectedDay() {
    document.querySelector("#selected-day-alerts").classList.add("hidden");
    document.querySelectorAll(".history-day-button").forEach(item => item.classList.remove("history-day-selected"));
}

function renderHistoryRooms(rooms) {
    const list = document.querySelector("#history-room-list");
    document.querySelector("#history-empty").classList.toggle("hidden", rooms.length !== 0);
    list.replaceChildren(...rooms.map(room => {
        const link = document.createElement("a"); link.className = "history-room"; link.href = `/alerts.html?roomId=${room.roomId}`;
        const identity = document.createElement("span");
        const name = document.createElement("strong"); name.textContent = room.roomName;
        const lab = document.createElement("small"); lab.textContent = room.labName;
        identity.append(name, lab);
        const counts = document.createElement("span"); counts.className = "history-room-counts";
        counts.textContent = `${room.alerts} alerts · ${room.criticalAlerts} critical · ${room.resolvedAlerts} resolved`;
        link.append(identity, counts); return link;
    }));
}

async function loadSensors() {
    exportSensor.replaceChildren(option("", "All sensors in room"));
    if (!exportRoom.value) { exportSensor.disabled = true; return; }
    try {
        const sensors = await apiRequest(`/api/sensors?roomId=${exportRoom.value}`);
        exportSensor.append(...sensors.map(sensor => option(sensor.id, sensor.name)));
        exportSensor.disabled = false;
    } catch (error) { showHistoryMessage(error.message, true); }
}

async function downloadReadings(event) {
    event.preventDefault();
    hideHistoryMessage();
    const parameters = new URLSearchParams({roomId: exportRoom.value,
        from: document.querySelector("#export-from").value, to: document.querySelector("#export-to").value});
    if (exportSensor.value) parameters.set("sensorId", exportSensor.value);
    try {
        const response = await apiFetch(`/api/sensor-readings/export?${parameters}`);
        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.message || `Export failed with status ${response.status}`);
        }
        const blob = await response.blob();
        const disposition = response.headers.get("Content-Disposition") || "";
        const filename = disposition.match(/filename="([^"]+)"/)?.[1] || "sensor-readings.csv";
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a"); link.href = url; link.download = filename;
        document.body.append(link); link.click(); link.remove(); URL.revokeObjectURL(url);
        showHistoryMessage("CSV export downloaded.");
    } catch (error) { showHistoryMessage(error.message, true); }
}

function setDefaultRange() {
    const to = new Date(); const from = new Date(to.getTime() - 24 * 60 * 60 * 1000);
    document.querySelector("#export-from").value = localDateTime(from);
    document.querySelector("#export-to").value = localDateTime(to);
}
function localDateTime(date) { const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000); return local.toISOString().slice(0, 16); }
function formatDuration(value) { return value == null ? "No data" : value < 60 ? `${value} min` : `${Math.floor(value / 60)} h ${value % 60} min`; }
function formatDay(value, count) { return new Intl.DateTimeFormat(undefined, count > 7 ? {month: "short", day: "numeric"} : {weekday: "short"}).format(new Date(`${value}T00:00:00`)); }
function formatLongDay(value) { return new Intl.DateTimeFormat(undefined, {dateStyle: "long"}).format(new Date(`${value}T00:00:00`)); }
function option(value, label) { const item = document.createElement("option"); item.value = value; item.textContent = label; return item; }
function showHistoryMessage(text, error = false) { historyMessage.textContent = text; historyMessage.className = `message ${error ? "message-error" : "message-success"}`; }
function hideHistoryMessage() { historyMessage.classList.add("hidden"); }

historyOrganization.addEventListener("change", loadOrganization);
exportRoom.addEventListener("change", loadSensors);
document.querySelector("#reading-export-form").addEventListener("submit", downloadReadings);
document.querySelector("#close-selected-day").addEventListener("click", closeSelectedDay);
document.querySelectorAll("[data-period]").forEach(button => button.addEventListener("click", async () => {
    document.querySelectorAll("[data-period]").forEach(item => item.classList.remove("active"));
    button.classList.add("active"); selectedPeriod = button.dataset.period; closeSelectedDay();
    try { await loadAlertHistory(); } catch (error) { showHistoryMessage(error.message, true); }
}));
initializeHistory();

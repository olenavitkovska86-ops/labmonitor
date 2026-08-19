const alertsApiUrl = "/api/alerts";

const rows = document.querySelector("#alert-rows");
const tableWrapper = document.querySelector("#table-wrapper");
const loadingState = document.querySelector("#loading-state");
const emptyState = document.querySelector("#empty-state");
const pageMessage = document.querySelector("#page-message");
const filterForm = document.querySelector("#filter-form");
const statusFilter = document.querySelector("#status-filter");
const severityFilter = document.querySelector("#severity-filter");

async function request(url, options = {}) {
    const response = await csrfFetch(url, options);

    if (response.ok) {
        return response.status === 204 ? null : response.json();
    }

    let error;
    try {
        error = await response.json();
    } catch {
        throw new Error(`Request failed with status ${response.status}`);
    }
    const details = error.details?.length ? `: ${error.details.join(", ")}` : "";
    throw new Error(`${error.message || error.error}${details}`);
}

async function loadAlerts() {
    loadingState.classList.remove("hidden");
    emptyState.classList.add("hidden");
    tableWrapper.classList.add("hidden");
    hideMessage();

    const parameters = new URLSearchParams();
    if (statusFilter.value) parameters.set("status", statusFilter.value);
    if (severityFilter.value) parameters.set("severity", severityFilter.value);

    try {
        const queryString = parameters.toString();
        const url = queryString ? `${alertsApiUrl}?${queryString}` : alertsApiUrl;
        renderAlerts(await request(url));
    } catch (error) {
        showMessage(error.message, true);
    } finally {
        loadingState.classList.add("hidden");
    }
}

function renderAlerts(alerts) {
    rows.replaceChildren();

    if (alerts.length === 0) {
        emptyState.classList.remove("hidden");
        return;
    }

    for (const alert of alerts) {
        const row = document.createElement("tr");
        row.append(
            createCell(formatDate(alert.createdAt)),
            createBadgeCell(alert.severity, severityClass(alert.severity)),
            createBadgeCell(alert.status, statusClass(alert.status)),
            createAlertCell(alert),
            createSensorCell(alert),
            createActionsCell(alert)
        );
        rows.append(row);
    }

    tableWrapper.classList.remove("hidden");
}

function createCell(value) {
    const cell = document.createElement("td");
    cell.textContent = value;
    return cell;
}

function createBadgeCell(value, className) {
    const cell = document.createElement("td");
    const badge = document.createElement("span");
    badge.className = `status ${className}`;
    badge.textContent = value;
    cell.append(badge);
    return cell;
}

function createAlertCell(alert) {
    const cell = document.createElement("td");
    const title = document.createElement("strong");
    const message = document.createElement("div");
    title.textContent = alert.title;
    message.className = "table-description";
    message.textContent = alert.message || "—";
    cell.append(title, message);
    return cell;
}

function createSensorCell(alert) {
    const cell = document.createElement("td");
    if (alert.sensorId == null) {
        cell.textContent = "—";
        return cell;
    }
    const link = document.createElement("a");
    link.className = "table-link";
    link.href = `/sensor-readings.html?sensorId=${alert.sensorId}`;
    link.textContent = `Sensor ${alert.sensorId}`;
    cell.append(link);
    return cell;
}

function createActionsCell(alert) {
    const cell = document.createElement("td");
    const actions = document.createElement("div");
    actions.className = "row-actions";

    if (alert.status === "ACTIVE") {
        actions.append(createActionButton("Acknowledge", () => changeStatus(alert.id, "acknowledge")));
    }
    if (alert.status !== "RESOLVED") {
        actions.append(createActionButton("Resolve", () => changeStatus(alert.id, "resolve"), true));
    }
    if (!actions.hasChildNodes()) {
        actions.textContent = "—";
    }
    cell.append(actions);
    return cell;
}

function createActionButton(label, action, secondary = false) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = `button button-small ${secondary ? "button-secondary" : "button-primary"}`;
    button.textContent = label;
    button.addEventListener("click", action);
    return button;
}

async function changeStatus(id, action) {
    try {
        await request(`${alertsApiUrl}/${id}/${action}`, {method: "POST"});
        await loadAlerts();
        showMessage(action === "acknowledge" ? "Alert acknowledged." : "Alert resolved.");
    } catch (error) {
        showMessage(error.message, true);
    }
}

function severityClass(severity) {
    if (severity === "CRITICAL" || severity === "HIGH") return "status-error";
    if (severity === "MEDIUM") return "status-warning";
    return "status-inactive";
}

function statusClass(status) {
    if (status === "ACTIVE") return "status-error";
    if (status === "ACKNOWLEDGED") return "status-warning";
    return "status-active";
}

function formatDate(value) {
    return new Intl.DateTimeFormat(undefined, {dateStyle: "medium", timeStyle: "short"})
        .format(new Date(value));
}

function showMessage(text, isError = false) {
    pageMessage.textContent = text;
    pageMessage.classList.toggle("message-error", isError);
    pageMessage.classList.remove("hidden");
}

function hideMessage() {
    pageMessage.classList.add("hidden");
    pageMessage.classList.remove("message-error");
}

filterForm.addEventListener("submit", event => {
    event.preventDefault();
    loadAlerts();
});

statusFilter.addEventListener("change", loadAlerts);
severityFilter.addEventListener("change", loadAlerts);

document.querySelector("#clear-filters").addEventListener("click", () => {
    filterForm.reset();
    loadAlerts();
});

renderBreadcrumbs([{label: "Home", href: "/"}, {label: "Alerts"}]);
loadAlerts();

const alertsApiUrl = "/api/alerts";

const rows = document.querySelector("#alert-rows");
const tableWrapper = document.querySelector("#table-wrapper");
const loadingState = document.querySelector("#loading-state");
const emptyState = document.querySelector("#empty-state");
const pageMessage = document.querySelector("#page-message");
const filterForm = document.querySelector("#filter-form");
const statusFilter = document.querySelector("#status-filter");
const severityFilter = document.querySelector("#severity-filter");
const alertsUpdatedAt = document.querySelector("#alerts-updated-at");
const resolutionPanel = document.querySelector("#resolution-panel");
const resolutionForm = document.querySelector("#resolution-form");
const resolutionAlertId = document.querySelector("#resolution-alert-id");
const resolutionOutcome = document.querySelector("#resolution-outcome");
const resolutionComment = document.querySelector("#resolution-comment");
const resolutionError = document.querySelector("#resolution-error");
const fixedOutcome = document.querySelector("#fixed-outcome");
const resolutionGuidance = document.querySelector("#resolution-guidance");
const reopenPanel = document.querySelector("#reopen-panel");
const reopenForm = document.querySelector("#reopen-form");
const reopenAlertId = document.querySelector("#reopen-alert-id");
const reopenReason = document.querySelector("#reopen-reason");
const reopenError = document.querySelector("#reopen-error");
const detailsDialog = document.querySelector("#alert-details-dialog");
const detailsLoading = document.querySelector("#details-loading");
const detailsContent = document.querySelector("#details-content");
const detailsSummary = document.querySelector("#details-summary");
const detailsTimeline = document.querySelector("#alert-timeline");
const detailsActions = document.querySelector("#details-actions");
const detailsError = document.querySelector("#details-error");

async function request(url, options = {}) {
    const token = localStorage.getItem("token");
    const headers = new Headers(options.headers || {});
    if (token) {
        headers.set("Authorization", `Bearer ${token}`);
    }

    const response = await fetch(url, {...options, headers});

    if (response.status === 401 || response.status === 403) {
        localStorage.removeItem("token");
        window.location.href = "/login.html";
        throw new Error("Authentication is required");
    }

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

async function loadAlerts({silent = false} = {}) {
    if (!silent) {
        loadingState.classList.remove("hidden");
        emptyState.classList.add("hidden");
        tableWrapper.classList.add("hidden");
        hideMessage();
    }

    const parameters = new URLSearchParams();
    const pageParameters = new URLSearchParams(window.location.search);
    if (pageParameters.get("roomId")) parameters.set("roomId", pageParameters.get("roomId"));
    if (statusFilter.value) parameters.set("status", statusFilter.value);
    if (severityFilter.value) parameters.set("severity", severityFilter.value);

    try {
        const queryString = parameters.toString();
        const url = queryString ? `${alertsApiUrl}?${queryString}` : alertsApiUrl;
        renderAlerts(await request(url));
        alertsUpdatedAt.textContent = `Auto-refresh on · Updated ${formatUpdateTime(new Date())}`;
    } catch (error) {
        showMessage(error.message, true);
    } finally {
        loadingState.classList.add("hidden");
    }
}

function renderAlerts(alerts) {
    rows.replaceChildren();

    if (alerts.length === 0) {
        tableWrapper.classList.add("hidden");
        emptyState.classList.remove("hidden");
        return;
    }

    emptyState.classList.add("hidden");
    for (const alert of alerts) {
        const row = document.createElement("tr");
        row.append(
            createCell(formatDate(alert.createdAt)),
            createBadgeCell(alert.severity, severityClass(alert.severity)),
            createBadgeCell(alert.status, statusClass(alert.status)),
            createAlertCell(alert),
            createSensorCell(alert),
            createHandlingCell(alert),
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
    message.textContent = thresholdDetails(alert) || alert.message || "—";
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

function createHandlingCell(alert) {
    if (alert.status === "RESOLVED" && alert.resolutionOutcome === "AUTO_RECOVERED") {
        return createCell(`Resolved automatically\n${formatDate(alert.resolvedAt)}\nSensor returned to the safe range`);
    }
    if (alert.status === "RESOLVED" && alert.resolvedByUserId != null) {
        const outcome = alert.resolutionOutcome ? `\n${formatOutcome(alert.resolutionOutcome)}` : "";
        const comment = alert.resolutionComment ? `\n${alert.resolutionComment}` : "";
        return createCell(
            `Resolved by ${alert.resolvedByName || alert.resolvedByUserId}\n${formatDate(alert.resolvedAt)}${outcome}${comment}`
        );
    }
    if (alert.status === "ACKNOWLEDGED" && alert.acknowledgedByUserId != null) {
        return createCell(`Acknowledged by ${alert.acknowledgedByName || alert.acknowledgedByUserId}\n${formatDate(alert.acknowledgedAt)}`);
    }
    if (alert.status === "ACTIVE" && alert.reopenedByUserId != null) {
        return createCell(`Reopened by ${alert.reopenedByName || alert.reopenedByUserId}\n${formatDate(alert.reopenedAt)}`);
    }
    return createCell("—");
}

function createActionsCell(alert) {
    const cell = document.createElement("td");
    const actions = document.createElement("div");
    actions.className = "row-actions";

    if (alert.status === "ACTIVE") {
        actions.append(createActionButton("Acknowledge", () => changeStatus(alert.id, "acknowledge")));
    }
    if (alert.status === "ACKNOWLEDGED") {
        actions.append(createActionButton("Resolve", () => openResolutionForm(alert), true));
    }
    if (alert.status === "RESOLVED") {
        actions.append(createActionButton("Reopen issue", () => openReopenForm(alert), true));
    }
    actions.append(createActionButton("View details", () => openAlertDetails(alert.id), true));
    cell.append(actions);
    return cell;
}

async function openAlertDetails(alertId) {
    detailsLoading.classList.remove("hidden");
    detailsContent.classList.add("hidden");
    detailsError.classList.add("hidden");
    detailsDialog.showModal();

    try {
        const [alert, history] = await Promise.all([
            request(`${alertsApiUrl}/${alertId}`),
            request(`${alertsApiUrl}/${alertId}/history`)
        ]);
        renderAlertDetails(alert, history);
        detailsLoading.classList.add("hidden");
        detailsContent.classList.remove("hidden");
    } catch (error) {
        detailsLoading.classList.add("hidden");
        detailsError.textContent = error.message;
        detailsError.classList.remove("hidden");
    }
}

function renderAlertDetails(alert, history) {
    document.querySelector("#details-title").textContent = alert.title;
    detailsSummary.replaceChildren(
        createDetailItem("Status", alert.status),
        createDetailItem("Severity", alert.severity),
        createDetailItem("Location", `Lab ${alert.labId} · Room ${alert.roomId}`),
        createDetailItem("Sensor", alert.sensorId == null ? "—" : `Sensor ${alert.sensorId}`),
        createDetailItem("Initial value", alert.initialValue ?? "—"),
        createDetailItem("Latest / most extreme", `${alert.latestValue ?? "—"} / ${alert.mostExtremeValue ?? "—"}`)
    );
    renderTimeline(alert, history);
    renderDetailsActions(alert);
}

function createDetailItem(label, value) {
    const item = document.createElement("div");
    item.className = "detail-item";
    const heading = document.createElement("span");
    const content = document.createElement("strong");
    heading.textContent = label;
    content.textContent = value;
    item.append(heading, content);
    return item;
}

function renderTimeline(alert, history) {
    const events = [{
        time: alert.createdAt,
        title: "Alert created",
        comment: alert.message
    }];

    if (alert.violationStartedAt && alert.violationStartedAt !== alert.createdAt) {
        events.push({time: alert.violationStartedAt, title: "Threshold violation started"});
    }
    for (const entry of history) {
        events.push({
            time: entry.createdAt,
            title: historyTitle(entry),
            comment: [entry.performedByName, entry.comment].filter(Boolean).join(" · ")
        });
    }
    if (alert.acknowledgedAt && !history.some(entry => entry.action === "ACKNOWLEDGED")) {
        events.push({
            time: alert.acknowledgedAt,
            title: "Alert acknowledged",
            comment: alert.acknowledgedByName
        });
    }
    if (alert.recoveredAt) {
        events.push({time: alert.recoveredAt, title: "Sensor returned to the safe range"});
    }
    events.sort((left, right) => new Date(left.time) - new Date(right.time));

    detailsTimeline.replaceChildren(...events.map(createTimelineEvent));
}

function historyTitle(entry) {
    const labels = {
        ACKNOWLEDGED: "Alert acknowledged",
        RESOLVED: `Alert resolved${entry.resolutionOutcome ? ` · ${formatOutcome(entry.resolutionOutcome)}` : ""}`,
        REOPENED: "Alert reopened",
        AUTO_RECOVERED: "Alert resolved automatically"
    };
    return labels[entry.action] || entry.action;
}

function createTimelineEvent(event) {
    const item = document.createElement("li");
    item.className = "timeline-event";
    const title = document.createElement("strong");
    const time = document.createElement("span");
    title.textContent = event.title;
    time.className = "timeline-time";
    time.textContent = formatDate(event.time);
    item.append(title, time);
    if (event.comment) {
        const comment = document.createElement("span");
        comment.className = "timeline-comment";
        comment.textContent = event.comment;
        item.append(comment);
    }
    return item;
}

function renderDetailsActions(alert) {
    detailsActions.replaceChildren();
    if (alert.status === "ACTIVE") {
        detailsActions.append(createActionButton("Acknowledge", async () => {
            detailsDialog.close();
            await changeStatus(alert.id, "acknowledge");
        }));
    } else if (alert.status === "ACKNOWLEDGED") {
        detailsActions.append(createActionButton("Resolve", () => {
            detailsDialog.close();
            openResolutionForm(alert);
        }));
    } else if (alert.status === "RESOLVED") {
        detailsActions.append(createActionButton("Reopen issue", () => {
            detailsDialog.close();
            openReopenForm(alert);
        }));
    }
}

function thresholdDetails(alert) {
    if (alert.initialValue == null) return null;
    const state = alert.recoveredAt
        ? `Recovered ${formatDate(alert.recoveredAt)}`
        : "Violation is ongoing";
    return `Started: ${alert.initialValue} · Latest: ${alert.latestValue} · Most extreme: ${alert.mostExtremeValue}\n${state}`;
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
        showMessage(action === "acknowledge" ? "Alert acknowledged." : "Alert updated.");
    } catch (error) {
        showMessage(error.message, true);
    }
}

function openReopenForm(alert) {
    reopenForm.reset();
    reopenAlertId.value = alert.id;
    document.querySelector("#reopen-title").textContent = `Reopen unresolved issue: ${alert.title}`;
    reopenError.classList.add("hidden");
    reopenPanel.classList.remove("hidden");
    reopenPanel.scrollIntoView({behavior: "smooth", block: "start"});
    reopenReason.focus();
}

function closeReopenForm() {
    reopenPanel.classList.add("hidden");
    reopenForm.reset();
    reopenError.classList.add("hidden");
}

async function reopenAlert(event) {
    event.preventDefault();
    reopenError.classList.add("hidden");
    try {
        await request(`${alertsApiUrl}/${reopenAlertId.value}/reopen`, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({reason: reopenReason.value.trim()})
        });
        closeReopenForm();
        await loadAlerts();
        showMessage("The incorrectly resolved alert was reopened.");
    } catch (error) {
        reopenError.textContent = error.message;
        reopenError.classList.remove("hidden");
    }
}

function openResolutionForm(alert) {
    resolutionForm.reset();
    resolutionAlertId.value = alert.id;
    document.querySelector("#resolution-title").textContent = `Resolve: ${alert.title}`;
    resolutionError.classList.add("hidden");
    fixedOutcome.disabled = !alert.recoveredAt;
    resolutionGuidance.textContent = alert.recoveredAt
        ? "The sensor has returned to its safe range."
        : "Problem fixed is unavailable until the sensor returns to its safe range. A false alarm requires an explanation.";
    resolutionGuidance.classList.remove("hidden");
    resolutionPanel.classList.remove("hidden");
    resolutionPanel.scrollIntoView({behavior: "smooth", block: "start"});
    resolutionOutcome.focus();
}

function closeResolutionForm() {
    resolutionPanel.classList.add("hidden");
    resolutionForm.reset();
    resolutionError.classList.add("hidden");
}

async function resolveAlert(event) {
    event.preventDefault();
    resolutionError.classList.add("hidden");
    if (resolutionOutcome.value === "FALSE_ALARM" && !resolutionComment.value.trim()) {
        resolutionError.textContent = "Explain why this reading is considered a false alarm.";
        resolutionError.classList.remove("hidden");
        resolutionComment.focus();
        return;
    }
    try {
        await request(`${alertsApiUrl}/${resolutionAlertId.value}/resolve`, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                outcome: resolutionOutcome.value,
                comment: resolutionComment.value.trim() || null
            })
        });
        closeResolutionForm();
        await loadAlerts();
        showMessage("Alert resolved and its result was saved.");
    } catch (error) {
        resolutionError.textContent = error.message;
        resolutionError.classList.remove("hidden");
    }
}

function formatOutcome(outcome) {
    const labels = {
        FIXED: "Problem fixed",
        FALSE_ALARM: "False alarm",
        AUTO_RECOVERED: "Recovered automatically"
    };
    return labels[outcome] || outcome;
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

function formatUpdateTime(value) {
    return new Intl.DateTimeFormat(undefined, {timeStyle: "medium"}).format(value);
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
resolutionForm.addEventListener("submit", resolveAlert);
document.querySelector("#close-resolution").addEventListener("click", closeResolutionForm);
document.querySelector("#cancel-resolution").addEventListener("click", closeResolutionForm);
reopenForm.addEventListener("submit", reopenAlert);
document.querySelector("#close-reopen").addEventListener("click", closeReopenForm);
document.querySelector("#cancel-reopen").addEventListener("click", closeReopenForm);
document.querySelector("#close-alert-details").addEventListener("click", () => detailsDialog.close());
detailsDialog.addEventListener("click", event => {
    if (event.target === detailsDialog) detailsDialog.close();
});

renderBreadcrumbs([{label: "Home", href: "/"}, {label: "Alerts"}]);
loadAlerts().then(() => {
    const requestedAlertId = new URLSearchParams(window.location.search).get("alertId");
    if (requestedAlertId) openAlertDetails(requestedAlertId);
});

setInterval(() => {
    if (document.visibilityState === "visible") {
        loadAlerts({silent: true});
    }
}, 5000);

document.addEventListener("visibilitychange", () => {
    if (document.visibilityState === "visible") {
        loadAlerts({silent: true});
    }
});

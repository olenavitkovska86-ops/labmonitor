const loadingState = document.querySelector("#loading-state");
const analyticsContent = document.querySelector("#analytics-content");
const pageMessage = document.querySelector("#page-message");
const problemList = document.querySelector("#problem-list");
const healthyState = document.querySelector("#healthy-state");
const currentView = document.querySelector("#current-view");
const historyView = document.querySelector("#history-view");
let selectedOrganizationId;
let selectedHistoryPeriod = null;
let analyticsRefreshInProgress = false;
let currentAuth;
let accessibleOrganizations = [];

async function request(url) {
    return apiRequest(url);
}

async function initialize() {
    hideMessage();
    try {
        currentAuth = await labMonitorAuthReady;
        accessibleOrganizations = await request("/api/organizations");
        if (accessibleOrganizations.length === 0) {
            loadingState.textContent = "Create an organization to start monitoring laboratory operations.";
            return;
        }

        const requestedId = new URLSearchParams(window.location.search).get("organizationId");
        const selected = accessibleOrganizations.find(organization => String(organization.id) === requestedId)
            || accessibleOrganizations[0];
        configureRoleCopy(currentAuth, selected.id);
        await loadAnalytics(selected.id);
    } catch (error) {
        loadingState.classList.add("hidden");
        showMessage(error.message);
    }
}

async function loadAnalytics(organizationId, {silent = false} = {}) {
    selectedOrganizationId = organizationId;
    if (!silent) {
        loadingState.textContent = "Loading operational overview...";
        loadingState.classList.remove("hidden");
        analyticsContent.classList.add("hidden");
        hideMessage();
    }

    try {
        const baseUrl = `/api/analytics/organizations/${organizationId}`;
        const [overview, problemRooms, dashboard] = await Promise.all([
            request(`${baseUrl}/overview`),
            request(`${baseUrl}/problem-rooms`),
            loadDashboardData(organizationId)
        ]);
        if (String(selectedOrganizationId) !== String(organizationId)) return;
        renderOverview(overview);
        renderProblemRooms(problemRooms);
        renderRoleDashboard(overview, dashboard, organizationId);
        analyticsContent.classList.remove("hidden");
        updateUrl(organizationId);
    } catch (error) {
        if (!silent) showMessage(error.message);
    } finally {
        if (!silent) loadingState.classList.add("hidden");
    }
}

async function loadDashboardData(organizationId) {
    const [labs, allRooms, allSensors, alerts, sessions] = await Promise.all([
        request(`/api/labs?organizationId=${organizationId}`),
        request("/api/rooms"),
        request("/api/sensors"),
        request(`/api/alerts?organizationId=${organizationId}`),
        request("/api/monitoring-sessions?status=ACTIVE")
    ]);
    const rooms = allRooms.filter(room => String(room.organizationId) === String(organizationId));
    const sensors = allSensors.filter(sensor => String(sensor.organizationId) === String(organizationId));
    return {
        labs,
        rooms,
        sensors,
        alerts,
        sessions: sessions.filter(session => String(session.organizationId) === String(organizationId))
    };
}

function configureRoleCopy(auth, organizationId) {
    const membership = auth.membership(organizationId);
    const role = auth.user.globalRole === "SUPER_ADMIN" ? "SUPER_ADMIN" : membership?.role;
    const titles = {
        SUPER_ADMIN: ["System overview", "Monitor organizations, laboratory operations and system health."],
        LAB_ADMIN: ["Laboratory overview", "Monitor the labs and rooms within your responsibility."],
        LIMITED_EMPLOYEE: ["My monitoring overview", "Follow the rooms, sensors and work available to you."]
    };
    const copy = titles[role] || titles.LIMITED_EMPLOYEE;
    setText("#overview-title", copy[0]);
    setText("#overview-intro", copy[1]);
}

function renderRoleDashboard(overview, data, organizationId) {
    const membership = currentAuth.membership(organizationId);
    const role = currentAuth.user.globalRole === "SUPER_ADMIN" ? "SUPER_ADMIN" : membership?.role || "LIMITED_EMPLOYEE";
    const openAlerts = data.alerts.filter(alert => alert.status !== "RESOLVED");
    const monitorUrl = `/monitor.html?organizationId=${organizationId}`;
    const alertsUrl = `/alerts.html?organizationId=${organizationId}`;
    const metrics = role === "SUPER_ADMIN"
        ? [["Organizations", accessibleOrganizations.length, false, "/organizations.html"], ["Labs", data.labs.length, false, `/labs.html?organizationId=${organizationId}`], ["Rooms", data.rooms.length, false, monitorUrl], ["Sensors", data.sensors.length, false, monitorUrl], ["Open alerts", openAlerts.length, true, alertsUrl]]
        : role === "LAB_ADMIN"
            ? [["Labs", data.labs.length, false, `/labs.html?organizationId=${organizationId}`], ["Rooms", data.rooms.length, false, monitorUrl], ["Sensors", data.sensors.length, false, monitorUrl], ["Open alerts", openAlerts.length, true, alertsUrl], ["My role", "Lab admin"]]
            : [["My rooms", data.rooms.length, false, monitorUrl], ["My sensors", data.sensors.length, false, monitorUrl], ["Open alerts", openAlerts.length, true, alertsUrl], ["My access", scopeLabel(membership)]];
    renderMetricCards(metrics);
    renderSessions(data.sessions.filter(session => session.status === "ACTIVE").slice(0, 4));
    renderRecentActivity(data.alerts.slice().sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)).slice(0, 4), data);
    renderStatusPanel(role, overview, membership, data);
    renderQuickActions(role, organizationId);
}

function renderMetricCards(metrics) {
    const grid = document.querySelector("#role-metric-grid");
    grid.replaceChildren();
    metrics.forEach(([label, value, attention, href]) => {
        const card = document.createElement(href ? "a" : "article");
        card.className = `dashboard-summary-card${attention ? " dashboard-summary-attention" : ""}`;
        if (href) {
            card.classList.add("dashboard-summary-link");
            card.href = href;
            card.setAttribute("aria-label", `${label}: ${value}`);
        }
        if (typeof value === "string") card.classList.add("dashboard-summary-text");
        const name = document.createElement("span"); name.textContent = label;
        const count = document.createElement("strong"); count.textContent = value;
        const detail = document.createElement("small"); detail.textContent = attention ? "Requires review" : typeof value === "string" ? "Current assignment" : "Accessible now";
        card.append(name, count, detail); grid.append(card);
    });
}

function renderSessions(sessions) {
    const list = document.querySelector("#active-session-list"); list.replaceChildren();
    if (!sessions.length) { list.innerHTML = '<p class="dashboard-empty">No active monitoring sessions.</p>'; return; }
    sessions.forEach(session => list.append(createDashboardRow(session.name, session.roomName || `Room ${session.roomId}`, "Active", `/monitoring-sessions.html?sessionId=${session.id}`)));
}

function renderRecentActivity(alerts, data) {
    const list = document.querySelector("#recent-activity-list"); list.replaceChildren();
    if (!alerts.length) { list.innerHTML = '<p class="dashboard-empty">No recent alert activity.</p>'; return; }
    const roomNames = new Map(data.rooms.map(room => [room.id, room.name]));
    const sensorNames = new Map(data.sensors.map(sensor => [sensor.id, sensor.name]));
    alerts.forEach(alert => {
        const location = sensorNames.get(alert.sensorId) || roomNames.get(alert.roomId) || "Monitoring alert";
        list.append(createDashboardRow(alert.title, location, formatDate(alert.createdAt), `/alerts.html?roomId=${alert.roomId}`));
    });
}

function createDashboardRow(titleText, detailText, metaText, href) {
    const row = document.createElement("a"); row.className = "dashboard-list-row"; row.href = href;
    const body = document.createElement("span"); const title = document.createElement("strong"); title.textContent = titleText;
    const detail = document.createElement("small"); detail.textContent = detailText; body.append(title, detail);
    const meta = document.createElement("span"); meta.className = "dashboard-row-meta"; meta.textContent = metaText;
    row.append(body, meta); return row;
}

function renderStatusPanel(role, overview, membership, data) {
    const title = document.querySelector("#status-panel-title");
    const content = document.querySelector("#status-panel-content"); content.replaceChildren();
    if (role === "LIMITED_EMPLOYEE") {
        title.textContent = "Access information";
        addStatus("Organization role", membership?.role?.replaceAll("_", " ") || "Employee");
        addStatus("Scope", scopeLabel(membership));
        addStatus("Accessible rooms", data.rooms.length);
    } else {
        title.textContent = "System status";
        addStatus("Offline sensors", overview.offlineSensors, overview.offlineSensors > 0, `/monitor.html?organizationId=${data.organizationId || selectedOrganizationId}&sensorStatus=OFFLINE`);
        addStatus("Critical alerts", overview.criticalAlerts, overview.criticalAlerts > 0, `/alerts.html?organizationId=${data.organizationId || selectedOrganizationId}&severity=CRITICAL`);
        addStatus("Rooms healthy", Math.max(0, overview.totalRooms - overview.roomsRequiringAttention), false, `/monitor.html?organizationId=${data.organizationId || selectedOrganizationId}`);
    }
    function addStatus(label, value, attention = false, href) {
        const row = document.createElement(href ? "a" : "div"); row.className = `status-list-row${href ? " status-list-link" : ""}`;
        if (href) row.href = href;
        const name = document.createElement("span"); name.textContent = label;
        const result = document.createElement("strong"); result.textContent = value; result.classList.toggle("status-value-attention", attention);
        row.append(name, result); content.append(row);
    }
}

function renderQuickActions(role, organizationId) {
    const list = document.querySelector("#quick-action-list"); list.replaceChildren();
    const actions = [[role === "LIMITED_EMPLOYEE" ? "Open my rooms" : "Open Monitor", `/monitor.html?organizationId=${organizationId}`], ["Review alerts", `/alerts.html?organizationId=${organizationId}`], ["Monitoring sessions", "/monitoring-sessions.html"]];
    if (currentAuth.has("users.manage")) actions.push(["Manage users & access", "/administration.html"]);
    actions.forEach(([label, href]) => { const link = document.createElement("a"); link.className = "quick-action"; link.href = href; link.textContent = `${label} →`; list.append(link); });
}

function scopeLabel(membership) {
    if (!membership) return "Global access";
    return membership.scopeType === "ORGANIZATION" ? "Whole organization" : "Assigned resources";
}

async function refreshAnalytics() {
    if (!selectedOrganizationId
            || analyticsRefreshInProgress
            || document.visibilityState !== "visible") return;
    analyticsRefreshInProgress = true;
    try {
        const refreshes = [loadAnalytics(selectedOrganizationId, {silent: true})];
        if (selectedHistoryPeriod && !historyView.classList.contains("hidden")) {
            refreshes.push(loadHistory(selectedHistoryPeriod, {silent: true}));
        }
        await Promise.all(refreshes);
    } finally {
        analyticsRefreshInProgress = false;
    }
}

async function loadHistory(period, {silent = false} = {}) {
    selectedHistoryPeriod = period;
    if (!silent) {
        loadingState.textContent = "Loading alert history...";
        loadingState.classList.remove("hidden");
        historyView.classList.add("hidden");
        hideMessage();
    }
    try {
        const history = await request(
            `/api/analytics/organizations/${selectedOrganizationId}/history?period=${period}`
        );
        renderHistory(history);
        historyView.classList.remove("hidden");
    } catch (error) {
        if (!silent) showMessage(error.message);
    } finally {
        if (!silent) loadingState.classList.add("hidden");
    }
}

function renderOverview(overview) {
    setText("#rooms-attention", overview.roomsRequiringAttention);
    setText("#rooms-context", `of ${overview.totalRooms} rooms`);
    setText("#unacknowledged-alerts", overview.unacknowledgedAlerts);
    setText("#critical-alerts", overview.criticalAlerts);
    setText("#offline-sensors", overview.offlineSensors);
    setText("#updated-at", `Updated ${formatUpdatedDate(overview.generatedAt)}`);
    document.querySelector("#rooms-attention-link").href = `/monitor.html?organizationId=${selectedOrganizationId}`;
    document.querySelector("#unacknowledged-alerts-link").href = `/alerts.html?organizationId=${selectedOrganizationId}&status=ACTIVE`;
    document.querySelector("#critical-alerts-link").href = `/alerts.html?organizationId=${selectedOrganizationId}&severity=CRITICAL`;
    document.querySelector("#offline-sensors-link").href = `/monitor.html?organizationId=${selectedOrganizationId}&sensorStatus=OFFLINE`;
}

function renderProblemRooms(rooms) {
    problemList.replaceChildren();
    healthyState.classList.toggle("hidden", rooms.length !== 0);

    for (const room of rooms) {
        const article = document.createElement("article");
        article.className = `problem-card priority-${room.attentionLevel.toLowerCase()}`;
        article.tabIndex = 0;
        article.setAttribute("role", "link");
        article.setAttribute("aria-label", `Review alerts for ${room.roomName}`);
        const alertsHref = `/alerts.html?roomId=${room.roomId}`;
        article.addEventListener("click", event => {
            if (!event.target.closest("a, button")) window.location.assign(alertsHref);
        });
        article.addEventListener("keydown", event => {
            if (event.key === "Enter") window.location.assign(alertsHref);
        });

        const priority = document.createElement("span");
        priority.className = `priority-badge priority-${room.attentionLevel.toLowerCase()}`;
        priority.textContent = room.attentionLevel;

        const location = document.createElement("div");
        location.className = "problem-location";
        const roomLink = document.createElement("a");
        roomLink.className = "problem-room-link";
        roomLink.href = `/rooms.html?labId=${room.labId}`;
        roomLink.textContent = room.roomName;
        const labName = document.createElement("span");
        labName.textContent = room.labName;
        location.append(roomLink, labName);

        const issue = document.createElement("div");
        issue.className = "problem-issue";
        const title = document.createElement("strong");
        title.textContent = room.mainProblem;
        const details = document.createElement("span");
        details.textContent = alertSummary(room);
        issue.append(title, details);

        const age = document.createElement("div");
        age.className = "problem-age";
        const ageValue = document.createElement("strong");
        ageValue.textContent = formatDuration(room.openMinutes);
        const ageLabel = document.createElement("span");
        ageLabel.textContent = "open";
        age.append(ageValue, ageLabel);

        const action = document.createElement("a");
        action.className = "button button-secondary button-small";
        action.href = alertsHref;
        action.textContent = "Review alerts";

        article.append(priority, location, issue, age, action);
        problemList.append(article);
    }
}

function renderHistory(history) {
    setText("#history-created", history.alertsCreated);
    setText("#history-critical", history.criticalAlerts);
    setText("#average-response", formatOptionalDuration(history.averageAcknowledgementMinutes));
    setText("#average-resolution", formatOptionalDuration(history.averageResolutionMinutes));
    renderHistoryChart(history.dailyAlerts);
    renderHistoryRooms(history.mostProblematicRooms);
}

function renderHistoryChart(days) {
    const chart = document.querySelector("#history-chart");
    chart.replaceChildren();
    const maximum = Math.max(1, ...days.map(day => day.alerts));
    for (const day of days) {
        const item = document.createElement("div");
        item.className = "history-day";
        item.title = `${day.alerts} alerts, ${day.criticalAlerts} critical`;

        const count = document.createElement("strong");
        count.textContent = day.alerts;
        const track = document.createElement("div");
        track.className = "history-bar-track";
        const bar = document.createElement("div");
        bar.className = day.criticalAlerts > 0 ? "history-bar history-bar-critical" : "history-bar";
        bar.style.height = `${Math.max(day.alerts === 0 ? 2 : 12, day.alerts / maximum * 100)}%`;
        track.append(bar);
        const label = document.createElement("span");
        label.textContent = formatDay(day.date, days.length);
        item.append(count, track, label);
        chart.append(item);
    }
}

function renderHistoryRooms(rooms) {
    const list = document.querySelector("#history-room-list");
    const empty = document.querySelector("#history-empty");
    list.replaceChildren();
    empty.classList.toggle("hidden", rooms.length !== 0);

    for (const room of rooms) {
        const item = document.createElement("a");
        item.className = "history-room";
        item.href = `/alerts.html?roomId=${room.roomId}`;
        const identity = document.createElement("span");
        const name = document.createElement("strong");
        name.textContent = room.roomName;
        const lab = document.createElement("small");
        lab.textContent = room.labName;
        identity.append(name, lab);
        const counts = document.createElement("span");
        counts.className = "history-room-counts";
        counts.textContent = `${room.alerts} alerts · ${room.criticalAlerts} critical · ${room.resolvedAlerts} resolved`;
        item.append(identity, counts);
        list.append(item);
    }
}

function alertSummary(room) {
    const waiting = room.unacknowledgedAlerts === 1
        ? "1 waiting for response"
        : `${room.unacknowledgedAlerts} waiting for response`;
    const total = room.unresolvedAlerts === 1 ? "1 unresolved alert" : `${room.unresolvedAlerts} unresolved alerts`;
    return `${total} · ${waiting}`;
}

function formatDuration(minutes) {
    if (minutes < 60) return `${minutes} min`;
    if (minutes < 1440) return `${Math.floor(minutes / 60)} h ${minutes % 60} min`;
    return `${Math.floor(minutes / 1440)} d ${Math.floor((minutes % 1440) / 60)} h`;
}

function formatOptionalDuration(minutes) {
    return minutes == null ? "No data" : formatDuration(minutes);
}

function formatDay(value, totalDays) {
    return new Intl.DateTimeFormat(undefined, totalDays > 7
        ? {month: "short", day: "numeric"}
        : {weekday: "short"}).format(new Date(`${value}T00:00:00`));
}

function formatDate(value) {
    return new Intl.DateTimeFormat(undefined, {dateStyle: "medium", timeStyle: "short"})
        .format(new Date(value));
}

function formatUpdatedDate(value) {
    return new Intl.DateTimeFormat(undefined, {dateStyle: "medium", timeStyle: "medium"})
        .format(new Date(value));
}

function setText(selector, value) {
    document.querySelector(selector).textContent = value;
}

function updateUrl(organizationId) {
    const url = new URL(window.location.href);
    url.searchParams.set("organizationId", organizationId);
    window.history.replaceState({}, "", url);
}

function showMessage(text) {
    pageMessage.textContent = text;
    pageMessage.classList.add("message-error");
    pageMessage.classList.remove("hidden");
}

function hideMessage() {
    pageMessage.classList.add("hidden");
}

document.querySelectorAll(".analytics-tab").forEach(tab => {
    tab.addEventListener("click", async () => {
        document.querySelectorAll(".analytics-tab").forEach(item => item.classList.remove("active"));
        tab.classList.add("active");
        const isHistory = tab.dataset.view === "history";
        currentView.classList.toggle("hidden", isHistory);
        historyView.classList.add("hidden");
        if (isHistory) {
            await loadHistory(tab.dataset.period);
        } else {
            selectedHistoryPeriod = null;
            currentView.classList.remove("hidden");
        }
    });
});
renderBreadcrumbs([{label: "Home", href: "/"}, {label: "Operational overview"}]);
initialize();
document.addEventListener("labmonitor:refresh", refreshAnalytics);

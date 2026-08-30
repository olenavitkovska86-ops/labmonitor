const loadingState = document.querySelector("#loading-state");
const analyticsContent = document.querySelector("#analytics-content");
const pageMessage = document.querySelector("#page-message");
const problemList = document.querySelector("#problem-list");
const healthyState = document.querySelector("#healthy-state");
const historyView = document.querySelector("#history-view");
let selectedOrganizationId;
let selectedHistoryPeriod = "LAST_24_HOURS";
let analyticsRefreshInProgress = false;
let currentAuth;
let accessibleOrganizations = [];
let selectedActiveSessionId = null;
let dashboardSessionChartInstances = [];

async function request(url) {
    return apiRequest(url);
}

async function initialize(organization) {
    hideMessage();
    if (!organization) {
        loadingState.textContent = "No organizations are available for your account.";
        return;
    }
    try {
        currentAuth = await labMonitorAuthReady;
        accessibleOrganizations = window.labMonitorOrganizationContext?.organizations || [];
        configureRoleCopy(currentAuth, organization.id);
        await loadAnalytics(organization.id);
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
        const [overview, problemRooms, dashboard, history] = await Promise.all([
            request(`${baseUrl}/overview`),
            request(`${baseUrl}/problem-rooms`),
            loadDashboardData(organizationId),
            request(`${baseUrl}/history?period=${selectedHistoryPeriod}`)
        ]);
        if (String(selectedOrganizationId) !== String(organizationId)) return;
        renderOverview(overview);
        renderProblemRooms(problemRooms);
        renderRoleDashboard(overview, dashboard, problemRooms, organizationId);
        renderHistory(history);
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
        request("/api/monitoring-sessions")
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

function renderRoleDashboard(overview, data, problemRooms, organizationId) {
    const membership = currentAuth.membership(organizationId);
    const role = currentAuth.user.globalRole === "SUPER_ADMIN" ? "SUPER_ADMIN" : membership?.role || "LIMITED_EMPLOYEE";
    const openAlerts = data.alerts.filter(alert => alert.status !== "RESOLVED");
    const monitorUrl = `/monitor.html?organizationId=${organizationId}`;
    const alertsUrl = `/alerts.html?organizationId=${organizationId}`;
    renderSystemStatus(overview, data, problemRooms, openAlerts, monitorUrl, alertsUrl, organizationId);
    const activeSessions = data.sessions.filter(session => session.status === "ACTIVE");
    const completedSessions = data.sessions.filter(session => session.status === "COMPLETED" && session.startedAt);
    renderSessions((activeSessions.length ? activeSessions : completedSessions).slice(0, 4), organizationId,
        activeSessions.length ? "active" : completedSessions.length ? "completed" : "empty");
    renderRecentActivity(data.alerts.slice().sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt)).slice(0, 4), data);
    renderStatusPanel(role, overview, membership, data);
    renderQuickActions(role, organizationId);
}

function renderSystemStatus(overview, data, problemRoomDetails, openAlerts, monitorUrl, alertsUrl, organizationId) {
    const grid = document.querySelector("#role-metric-grid");
    grid.replaceChildren();
    const problemRooms = Number(overview.roomsRequiringAttention || 0);
    const offlineSensors = Number(overview.offlineSensors || 0);
    const roomsHref = problemRooms === 1 && problemRoomDetails.length === 1
        ? `/alerts.html?organizationId=${organizationId}&roomId=${problemRoomDetails[0].roomId}&openOnly=true`
        : problemRooms > 0 ? `/alerts.html?organizationId=${organizationId}&openOnly=true` : monitorUrl;
    const sensorsHref = offlineSensors > 0 ? `${monitorUrl}&sensorStatus=OFFLINE` : monitorUrl;
    const openAlertsHref = openAlerts.length > 0 ? `${alertsUrl}&openOnly=true` : alertsUrl;
    const items = [
        {label: "ROOMS", icon: "□", value: problemRooms || data.rooms.length,
            state: problemRooms ? "need attention" : data.rooms.length ? "healthy" : "none available",
            secondary: data.rooms.length, context: "accessible", attention: problemRooms > 0, href: roomsHref},
        {label: "SENSORS", icon: "⌁", value: offlineSensors || data.sensors.length,
            state: offlineSensors ? "offline" : data.sensors.length ? "online" : "none available",
            secondary: data.sensors.length, context: "accessible", attention: offlineSensors > 0, href: sensorsHref},
        {label: "ALERTS", icon: "△", value: openAlerts.length,
            state: openAlerts.length ? "open" : "no open alerts",
            secondary: Number(overview.criticalAlerts || 0), context: "critical", attention: openAlerts.length > 0, href: openAlertsHref}
    ];
    items.forEach(item => {
        const card = document.createElement("a"); card.className = `dashboard-status-item${item.attention ? " dashboard-status-attention" : ""}`;
        card.href = item.href; card.setAttribute("aria-label", `${item.label}: ${item.value} ${item.state}`);
        const icon = document.createElement("span"); icon.className = "dashboard-status-icon"; icon.textContent = item.icon; icon.setAttribute("aria-hidden", "true");
        const copy = document.createElement("span"); copy.className = "dashboard-status-copy";
        const label = document.createElement("span"); label.className = "dashboard-status-label"; label.textContent = item.label;
        const value = document.createElement("strong"); value.className = "dashboard-status-value"; value.textContent = item.value;
        const state = document.createElement("span"); state.className = "dashboard-status-state"; state.textContent = item.state;
        const divider = document.createElement("span"); divider.className = "dashboard-status-divider";
        const secondary = document.createElement("strong"); secondary.className = "dashboard-status-secondary"; secondary.textContent = item.secondary;
        const context = document.createElement("span"); context.className = "dashboard-status-context"; context.textContent = item.context;
        copy.append(label, value, state, divider, secondary, context); card.append(icon, copy); grid.append(card);
    });
}

function renderSessions(sessions, organizationId, mode) {
    const select = document.querySelector("#active-session-select");
    const state = document.querySelector("#active-session-state");
    const charts = document.querySelector("#active-session-charts");
    const link = document.querySelector("#active-session-link");
    const title = document.querySelector("#session-panel-title");
    const kicker = document.querySelector("#session-panel-kicker");
    dashboardSessionChartInstances.forEach(chart => chart.destroy());
    dashboardSessionChartInstances = [];
    select.replaceChildren(); charts.replaceChildren();
    if (!sessions.length) {
        selectedActiveSessionId = null;
        setSessionKicker(kicker, "Monitoring", false); title.textContent = "Live monitoring";
        select.classList.add("hidden"); link.textContent = "Start a session";
        link.href = `/monitoring-sessions.html?organizationId=${organizationId}`;
        state.textContent = "No session data yet. Start a monitoring session; its chart will appear after the first sensor reading."; state.classList.remove("hidden");
        return;
    }
    const isActive = mode === "active";
    setSessionKicker(kicker, isActive ? "Current session" : "Recent session", isActive);
    title.textContent = isActive ? "Live monitoring" : "Latest completed session";
    select.classList.toggle("hidden", sessions.length === 1);
    sessions.forEach(session => select.append(new Option(`${session.name} · ${session.roomName || `Room ${session.roomId}`}`, session.id)));
    const selected = sessions.find(session => String(session.id) === String(selectedActiveSessionId)) || sessions[0];
    selectedActiveSessionId = selected.id; select.value = selected.id;
    select.onchange = () => { selectedActiveSessionId = Number(select.value); const session = sessions.find(item => item.id === selectedActiveSessionId); if (session) loadActiveSessionChart(session, organizationId, isActive); };
    loadActiveSessionChart(selected, organizationId, isActive);
}

function setSessionKicker(element, text, live) {
    element.replaceChildren();
    if (live) { const dot = document.createElement("i"); dot.setAttribute("aria-hidden", "true"); element.append(dot); }
    element.append(text);
}

async function loadActiveSessionChart(session, organizationId, isActive) {
    const state = document.querySelector("#active-session-state");
    const charts = document.querySelector("#active-session-charts");
    const link = document.querySelector("#active-session-link");
    charts.replaceChildren(); state.textContent = "Loading session readings…"; state.classList.remove("hidden");
    link.textContent = "Open session";
    link.href = `/monitoring-sessions.html?organizationId=${organizationId}&sessionId=${session.id}`;
    try {
        const timeline = await request(`/api/monitoring-sessions/${session.id}/timeline`);
        if (String(selectedActiveSessionId) !== String(session.id)) return;
        renderActiveSessionCharts(timeline, charts);
        state.classList.toggle("hidden", timeline.readings.length > 0);
        if (!timeline.readings.length) state.textContent = isActive
            ? "This session is active, but no sensor readings have been recorded since it started."
            : "The latest completed session contains no sensor readings.";
    } catch (error) {
        state.textContent = error.message;
    }
}

function renderActiveSessionCharts(timeline, container) {
    dashboardSessionChartInstances.forEach(chart => chart.destroy());
    dashboardSessionChartInstances = [];
    const groups = new Map();
    timeline.readings.forEach(reading => {
        const unit = reading.unit || "Value";
        if (!groups.has(unit)) groups.set(unit, []);
        groups.get(unit).push(reading);
    });
    groups.forEach((readings, unit) => container.append(createSessionUnitChart(timeline, unit, readings)));
}

function createSessionUnitChart(timeline, unit, readings) {
    const section = document.createElement("section"); section.className = "dashboard-session-chart";
    const heading = document.createElement("div"); heading.className = "dashboard-session-chart-heading";
    const title = document.createElement("h3"); title.textContent = unit === "Value" ? "Sensor values" : unit;
    heading.append(title);
    const sensors = new Map(); readings.forEach(reading => { if (!sensors.has(reading.sensorId)) sensors.set(reading.sensorId, []); sensors.get(reading.sensorId).push(reading); });
    const chart = document.createElement("div"); chart.className = "dashboard-session-chart-canvas";
    const canvas = document.createElement("canvas"); canvas.setAttribute("role", "img"); canvas.setAttribute("aria-label", `Session readings in ${unit}`);
    chart.append(canvas); section.append(heading, chart);
    dashboardSessionChartInstances.push(LabMonitorSessionChart.create(canvas, timeline, readings, {showLegend: sensors.size > 1}));
    return section;
}

function renderRecentActivity(alerts, data) {
    const list = document.querySelector("#recent-activity-list"); list.replaceChildren();
    if (!alerts.length) { list.innerHTML = '<p class="dashboard-empty">No recent alert activity.</p>'; return; }
    const roomNames = new Map(data.rooms.map(room => [room.id, room.name]));
    const sensorNames = new Map(data.sensors.map(sensor => [sensor.id, sensor.name]));
    alerts.forEach(alert => {
        const location = sensorNames.get(alert.sensorId) || roomNames.get(alert.roomId) || "Monitoring alert";
        list.append(createDashboardRow(alert.title, location, formatDate(alert.createdAt), `/alerts.html?organizationId=${selectedOrganizationId}&roomId=${alert.roomId}`));
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
    const content = document.querySelector("#status-panel-content");
    if (!title || !content) return;
    content.replaceChildren();
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
    const actions = [[role === "LIMITED_EMPLOYEE" ? "Open my rooms" : "Open Monitor", `/monitor.html?organizationId=${organizationId}`], ["Review alerts", `/alerts.html?organizationId=${organizationId}`], ["Monitoring sessions", `/monitoring-sessions.html?organizationId=${organizationId}`]];
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
        await loadAnalytics(selectedOrganizationId, {silent: true});
    } finally {
        analyticsRefreshInProgress = false;
    }
}

async function loadHistory(period, {silent = false} = {}) {
    selectedHistoryPeriod = period;
    const panelLoading = document.querySelector("#history-loading-state");
    if (!silent) {
        panelLoading.classList.remove("hidden");
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
        if (!silent) panelLoading.classList.add("hidden");
    }
}

function renderOverview(overview) {
    setText("#updated-at", `Updated ${formatUpdatedDate(overview.generatedAt)}`);
}

function renderProblemRooms(rooms) {
    problemList.replaceChildren();
    healthyState.classList.toggle("hidden", rooms.length !== 0);
    document.querySelector("#attention-section").classList.toggle("dashboard-attention-healthy", rooms.length === 0);

    for (const room of rooms) {
        const article = document.createElement("article");
        article.className = `problem-card priority-${room.attentionLevel.toLowerCase()}`;
        article.tabIndex = 0;
        article.setAttribute("role", "link");
        article.setAttribute("aria-label", `Review alerts for ${room.roomName}`);
        const alertsHref = `/alerts.html?organizationId=${selectedOrganizationId}&roomId=${room.roomId}`;
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
        roomLink.href = `/rooms.html?organizationId=${selectedOrganizationId}&labId=${room.labId}`;
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
        item.href = `/alerts.html?organizationId=${selectedOrganizationId}&roomId=${room.roomId}`;
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
        historyView.classList.add("hidden");
        await loadHistory(tab.dataset.period);
    });
});
renderBreadcrumbs([{label: "Home", href: "/"}, {label: "Operational overview"}]);
if (window.labMonitorOrganizationContext) {
    initialize(window.labMonitorOrganizationContext.selected);
} else {
    document.addEventListener("labmonitor:organization-ready", event => initialize(event.detail), {once: true});
}
document.addEventListener("labmonitor:refresh", refreshAnalytics);
document.querySelector("#refresh-overview").addEventListener("click", async event => {
    if (analyticsRefreshInProgress) return;
    event.currentTarget.classList.add("is-refreshing");
    await refreshAnalytics();
    event.currentTarget.classList.remove("is-refreshing");
});

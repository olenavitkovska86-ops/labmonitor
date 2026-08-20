const organizationSelect = document.querySelector("#organization-select");
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

async function request(url) {
    const token = localStorage.getItem("token");
    const headers = new Headers();
    if (token) headers.set("Authorization", `Bearer ${token}`);

    const response = await fetch(url, {headers});
    if (response.status === 401 || response.status === 403) {
        localStorage.removeItem("token");
        window.location.href = "/login.html";
        throw new Error("Authentication is required");
    }
    if (response.ok) return response.json();

    try {
        const error = await response.json();
        throw new Error(error.message || error.error || `Request failed with status ${response.status}`);
    } catch (error) {
        if (error instanceof SyntaxError) throw new Error(`Request failed with status ${response.status}`);
        throw error;
    }
}

async function initialize() {
    hideMessage();
    try {
        const organizations = await request("/api/organizations");
        renderOrganizations(organizations);
        if (organizations.length === 0) {
            loadingState.textContent = "Create an organization to start monitoring laboratory operations.";
            return;
        }

        const requestedId = new URLSearchParams(window.location.search).get("organizationId");
        const selected = organizations.find(organization => String(organization.id) === requestedId)
            || organizations[0];
        organizationSelect.value = selected.id;
        organizationSelect.disabled = false;
        await loadAnalytics(selected.id);
    } catch (error) {
        loadingState.classList.add("hidden");
        showMessage(error.message);
    }
}

function renderOrganizations(organizations) {
    organizationSelect.replaceChildren();
    for (const organization of organizations) {
        const option = document.createElement("option");
        option.value = organization.id;
        option.textContent = organization.name;
        organizationSelect.append(option);
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
        const [overview, problemRooms] = await Promise.all([
            request(`${baseUrl}/overview`),
            request(`${baseUrl}/problem-rooms`)
        ]);
        if (String(selectedOrganizationId) !== String(organizationId)) return;
        renderOverview(overview);
        renderProblemRooms(problemRooms);
        analyticsContent.classList.remove("hidden");
        updateUrl(organizationId);
    } catch (error) {
        if (!silent) showMessage(error.message);
    } finally {
        if (!silent) loadingState.classList.add("hidden");
    }
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
}

function renderProblemRooms(rooms) {
    problemList.replaceChildren();
    healthyState.classList.toggle("hidden", rooms.length !== 0);

    for (const room of rooms) {
        const article = document.createElement("article");
        article.className = `problem-card priority-${room.attentionLevel.toLowerCase()}`;

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
        action.href = `/alerts.html?roomId=${room.roomId}`;
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

organizationSelect.addEventListener("change", () => loadAnalytics(organizationSelect.value));
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

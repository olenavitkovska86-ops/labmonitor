const $ = selector => document.querySelector(selector);
const organizationSelect = $("#organization-select");
const previewRoleSelect = $("#overview-preview-role");
let organizations = [];
let refreshing = false;

async function request(url) {
    const token = localStorage.getItem("token");
    const response = await fetch(url, {headers: token ? {Authorization: `Bearer ${token}`} : {}});
    if (response.status === 401 || response.status === 403) {
        localStorage.removeItem("token");
        window.location.href = "/login.html";
        throw new Error("Authentication is required");
    }
    if (response.ok) return response.json();
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || error.error || `Request failed with status ${response.status}`);
}

async function initialize() {
    previewRoleSelect.value = new URLSearchParams(location.search).get("overviewRole")
        || sessionStorage.getItem("overviewPreviewRole") || "LIMITED_EMPLOYEE";
    try {
        organizations = await request("/api/organizations");
        organizationSelect.replaceChildren(...organizations.map(organization => option(organization.id, organization.name)));
        if (!organizations.length) {
            $("#loading-state").textContent = "Create an organization to start monitoring.";
            return;
        }
        const requested = new URLSearchParams(location.search).get("organizationId");
        organizationSelect.value = organizations.some(item => String(item.id) === requested)
            ? requested : organizations[0].id;
        organizationSelect.disabled = false;
        await loadOverview();
    } catch (error) { showError(error.message); }
}

async function loadOverview({silent = false} = {}) {
    const role = previewRoleSelect.value;
    const systemMode = role === "SUPER_ADMIN";
    sessionStorage.setItem("overviewPreviewRole", role);
    $("#organization-picker").classList.toggle("hidden", systemMode);
    $("#operational-overview").classList.toggle("hidden", systemMode);
    $("#system-overview").classList.toggle("hidden", !systemMode);
    $("#overview-intro").textContent = systemMode
        ? "Organizations, system condition, and active monitoring at a glance."
        : "What needs attention and what is being monitored in your work area.";
    if (!silent) {
        $("#loading-state").classList.remove("hidden");
        $("#overview-content").classList.add("hidden");
        $("#page-message").classList.add("hidden");
    }
    try {
        if (systemMode) await loadSystem(); else await loadOperational(role);
        $("#overview-content").classList.remove("hidden");
    } catch (error) { if (!silent) showError(error.message); }
    finally { if (!silent) $("#loading-state").classList.add("hidden"); }
}

async function loadOperational(role) {
    const organizationId = organizationSelect.value;
    const [overview, problems, labs, sessions] = await Promise.all([
        request(`/api/analytics/organizations/${organizationId}/overview`),
        request(`/api/analytics/organizations/${organizationId}/problem-rooms`),
        request(`/api/labs?organizationId=${organizationId}`),
        request("/api/monitoring-sessions?status=ACTIVE")
    ]);
    const roomEntries = await Promise.all(labs.map(async lab => [lab.id, await request(`/api/rooms?labId=${lab.id}`)]));
    const roomsByLab = new Map(roomEntries);
    const roomToLab = new Map(roomEntries.flatMap(([labId, rooms]) => rooms.map(room => [room.id, labId])));
    renderProblems("#problem-list", "#healthy-state", problems);
    renderSessions("#active-session-list", "#sessions-empty",
        sessions.filter(session => roomToLab.has(session.roomId)), session => roomToLab.get(session.roomId));
    renderStatus("#operational-status", [
        [overview.roomsRequiringAttention, "rooms needing attention"],
        [overview.unacknowledgedAlerts, "unacknowledged alerts"],
        [overview.criticalAlerts, "critical alerts"],
        [overview.offlineSensors, "offline sensors"]
    ]);
    if (role === "LAB_ADMIN") renderLabs(labs, roomsByLab, problems);
    else renderEmployeeRooms(labs, roomsByLab, problems);
    $("#work-area-title").textContent = role === "LAB_ADMIN" ? "Labs in my responsibility" : "My work area";
    updated(overview.generatedAt);
    const url = new URL(location.href);
    url.searchParams.set("organizationId", organizationId);
    history.replaceState({}, "", url);
}

async function loadSystem() {
    const [labs, rooms, sensors, sessions, results] = await Promise.all([
        request("/api/labs"), request("/api/rooms"), request("/api/sensors"),
        request("/api/monitoring-sessions?status=ACTIVE"),
        Promise.all(organizations.map(async organization => ({
            organization,
            overview: await request(`/api/analytics/organizations/${organization.id}/overview`),
            problems: await request(`/api/analytics/organizations/${organization.id}/problem-rooms`)
        })))
    ]);
    renderOrganizations(results);
    renderStatus("#system-status", [[organizations.length, "organizations"], [labs.length, "labs"],
        [rooms.length, "rooms"], [sensors.length, "sensors"]]);
    const allProblems = results.flatMap(result => result.problems.map(problem => ({
        ...problem, organizationName: result.organization.name
    })));
    renderProblems("#system-problem-list", null, allProblems.slice(0, 6), true);
    const roomToLab = new Map(rooms.map(room => [room.id, room.labId]));
    renderSessions("#system-session-list", null, sessions, session => roomToLab.get(session.roomId));
    updated(new Date().toISOString());
}

function renderProblems(listSelector, emptySelector, problems, showOrganization = false) {
    const list = $(listSelector);
    list.replaceChildren();
    if (emptySelector) $(emptySelector).classList.toggle("hidden", problems.length !== 0);
    if (!problems.length && !emptySelector) list.append(empty("No rooms currently require attention."));
    problems.forEach(room => {
        const row = article("overview-row");
        row.append(identity(room.roomName, showOrganization ? `${room.labName} · ${room.organizationName}` : room.labName),
            badge(room.attentionLevel, `priority-${room.attentionLevel.toLowerCase()}`),
            detail(room.mainProblem, `${room.unresolvedAlerts} unresolved · ${room.unacknowledgedAlerts} awaiting response`),
            detail(duration(room.openMinutes), "open"),
            action(`/rooms.html?labId=${room.labId}&roomId=${room.roomId}`, "Open room"));
        list.append(row);
    });
}

function renderSessions(listSelector, emptySelector, sessions, labIdFor) {
    const list = $(listSelector);
    list.replaceChildren();
    if (emptySelector) $(emptySelector).classList.toggle("hidden", sessions.length !== 0);
    if (!sessions.length && !emptySelector) list.append(empty("No active monitoring sessions."));
    sessions.forEach(session => {
        const row = article("overview-row overview-session-row");
        row.append(identity(session.roomName, "Active session"), detail(session.name, session.description || "Monitoring in progress"),
            detail(durationSince(session.startedAt), `started ${dateTime(session.startedAt)}`),
            action(`/monitoring-sessions.html?roomId=${session.roomId}&sessionId=${session.id}`, "Open session"));
        row.dataset.labId = labIdFor(session) || "";
        list.append(row);
    });
}

function renderLabs(labs, roomsByLab, problems) {
    const counts = new Map();
    problems.forEach(room => counts.set(room.labId, (counts.get(room.labId) || 0) + 1));
    const list = $("#lab-status-list");
    list.replaceChildren(...labs.map(lab => {
        const affected = counts.get(lab.id) || 0;
        const row = article("overview-row overview-lab-row");
        row.append(identity(lab.name, `${(roomsByLab.get(lab.id) || []).length} rooms`), state(affected),
            detail(affected ? `${affected} rooms need attention` : "All monitored rooms look normal",
                affected ? "Review required" : "Healthy"), action(`/rooms.html?labId=${lab.id}`, "Open in Monitor"));
        return row;
    }));
}

function renderEmployeeRooms(labs, roomsByLab, problems) {
    const labNames = new Map(labs.map(lab => [lab.id, lab.name]));
    const problemByRoom = new Map(problems.map(problem => [problem.roomId, problem]));
    const rooms = [...roomsByLab.values()].flat();
    const list = $("#lab-status-list");
    if (!rooms.length) {
        list.replaceChildren(empty("No rooms are available in this work area."));
        return;
    }
    list.replaceChildren(...rooms.map(room => {
        const problem = problemByRoom.get(room.id);
        const row = article("overview-row overview-lab-row");
        row.append(identity(room.name, labNames.get(room.labId) || `Lab ${room.labId}`), state(problem ? 1 : 0),
            detail(problem ? problem.mainProblem : "No current problems",
                problem ? duration(problem.openMinutes) : "Healthy"),
            action(`/sensors.html?roomId=${room.id}`, "Open room"));
        return row;
    }));
}

function renderOrganizations(results) {
    const list = $("#organization-status-list");
    list.replaceChildren(...results.map(({organization, overview}) => {
        const affected = overview.roomsRequiringAttention;
        const row = article("overview-row overview-organization-row");
        row.append(identity(organization.name, `${overview.totalRooms} rooms`), state(affected),
            detail(affected ? `${affected} rooms need attention` : "All monitored rooms look normal",
                `${overview.offlineSensors} sensors offline`),
            action(`/analytics.html?organizationId=${organization.id}&overviewRole=LAB_ADMIN`, "Open organization"));
        return row;
    }));
}

function renderStatus(selector, values) {
    $(selector).replaceChildren(...values.map(([value, label]) => {
        const item = document.createElement("div");
        const dt = document.createElement("dt"); dt.textContent = label;
        const dd = document.createElement("dd"); dd.textContent = value;
        item.append(dd, dt); return item;
    }));
}

function identity(title, subtitle) { return textBlock("overview-identity", title, subtitle); }
function detail(title, subtitle) { return textBlock("overview-detail", title, subtitle); }
function textBlock(className, titleText, subtitleText) {
    const block = document.createElement("div"); block.className = className;
    const title = document.createElement("strong"); title.textContent = titleText;
    const subtitle = document.createElement("span"); subtitle.textContent = subtitleText;
    block.append(title, subtitle); return block;
}
function badge(text, className) { const node = document.createElement("span"); node.className = `priority-badge ${className}`; node.textContent = text; return node; }
function state(affected) { return badge(affected ? "Attention" : "Healthy", `overview-state ${affected ? "overview-state-attention" : "overview-state-healthy"}`); }
function action(href, text) { const link = document.createElement("a"); link.className = "button button-secondary button-small"; link.href = href; link.textContent = text; return link; }
function article(className) { const node = document.createElement("article"); node.className = className; return node; }
function empty(text) { const node = document.createElement("p"); node.className = "overview-empty-row"; node.textContent = text; return node; }
function option(value, text) { const node = document.createElement("option"); node.value = value; node.textContent = text; return node; }
function duration(minutes) { return minutes < 60 ? `${minutes} min` : minutes < 1440 ? `${Math.floor(minutes / 60)} h ${minutes % 60} min` : `${Math.floor(minutes / 1440)} d`; }
function durationSince(value) { return value ? duration(Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 60000))) : "Not started"; }
function dateTime(value) { return value ? new Intl.DateTimeFormat(undefined, {dateStyle: "medium", timeStyle: "short"}).format(new Date(value)) : "Not started"; }
function updated(value) { $("#updated-at").textContent = `Updated ${new Intl.DateTimeFormat(undefined, {hour: "2-digit", minute: "2-digit"}).format(new Date(value))}`; }
function showError(text) { $("#loading-state").classList.add("hidden"); $("#page-message").textContent = text; $("#page-message").classList.remove("hidden"); }

organizationSelect.addEventListener("change", () => loadOverview());
previewRoleSelect.addEventListener("change", () => {
    document.dispatchEvent(new CustomEvent("labmonitor:preview-role-change", {detail: previewRoleSelect.value}));
    loadOverview();
});
renderBreadcrumbs([{label: "Home", href: "/"}, {label: "Overview"}]);
initialize();
document.addEventListener("labmonitor:refresh", async () => {
    if (refreshing || document.visibilityState !== "visible") return;
    refreshing = true; try { await loadOverview({silent: true}); } finally { refreshing = false; }
});

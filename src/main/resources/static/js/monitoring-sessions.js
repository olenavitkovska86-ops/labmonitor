const sessionsApiUrl = "/api/monitoring-sessions";
const roomsApiUrl = "/api/rooms";

const rows = document.querySelector("#session-rows");
const tableWrapper = document.querySelector("#table-wrapper");
const loadingState = document.querySelector("#loading-state");
const emptyState = document.querySelector("#empty-state");
const pageMessage = document.querySelector("#page-message");
const formPanel = document.querySelector("#session-form-panel");
const sessionForm = document.querySelector("#session-form");
const sessionFormError = document.querySelector("#session-form-error");
const sessionRoom = document.querySelector("#session-room");
const sessionName = document.querySelector("#session-name");
const sessionDescription = document.querySelector("#session-description");
const filterForm = document.querySelector("#filter-form");
const roomFilter = document.querySelector("#room-filter");
const statusFilter = document.querySelector("#status-filter");
const detailsDialog = document.querySelector("#session-details-dialog");
const detailsTitle = document.querySelector("#details-title");
const detailsLoading = document.querySelector("#details-loading");
const detailsContent = document.querySelector("#details-content");
const detailsSummary = document.querySelector("#details-summary");
const detailsActions = document.querySelector("#details-actions");
const detailsError = document.querySelector("#details-error");
const eventFormSection = document.querySelector("#event-form-section");
const eventForm = document.querySelector("#event-form");
const eventCategory = document.querySelector("#event-category");
const eventTitle = document.querySelector("#event-title");
const eventTime = document.querySelector("#event-time");
const eventDescription = document.querySelector("#event-description");
const eventFormError = document.querySelector("#event-form-error");
const eventsList = document.querySelector("#session-events");
const eventsEmpty = document.querySelector("#events-empty");

let roomsById = new Map();
let openSessionId = null;

const statusLabels = {
    PLANNED: "Planned",
    ACTIVE: "Active",
    COMPLETED: "Completed",
    CANCELLED: "Cancelled"
};

const categoryLabels = {
    OBSERVATION: "Observation",
    INTERVENTION: "Intervention",
    CONFIGURATION_CHANGE: "Configuration change",
    MAINTENANCE: "Maintenance",
    INCIDENT: "Incident",
    OTHER: "Other"
};

async function request(url, options = {}) {
    const headers = new Headers(options.headers || {});
    const token = localStorage.getItem("token");
    if (token) headers.set("Authorization", `Bearer ${token}`);
    if (options.body) headers.set("Content-Type", "application/json");
    const response = await fetch(url, {...options, headers});

    if (response.status === 401 || response.status === 403) {
        localStorage.removeItem("token");
        window.location.href = "/login.html";
        throw new Error("Authentication is required");
    }
    if (response.ok) return response.status === 204 ? null : response.json();

    let error;
    try { error = await response.json(); }
    catch { throw new Error(`Request failed with status ${response.status}`); }
    const details = error.details?.length ? `: ${error.details.join(", ")}` : "";
    throw new Error(`${error.message || error.error}${details}`);
}

async function initializePage() {
    renderBreadcrumbs([{label: "Home", href: "/"}, {label: "Monitoring sessions"}]);
    try {
        const rooms = await request(roomsApiUrl);
        roomsById = new Map(rooms.map(room => [room.id, room]));
        renderRoomOptions(rooms);
        applyRoomFromUrl();
        await loadSessions();
    } catch (error) {
        loadingState.classList.add("hidden");
        showMessage(pageMessage, error.message, true);
    }
}

function renderRoomOptions(rooms) {
    for (const room of rooms) {
        roomFilter.append(createOption(room.id, room.name));
        if (room.active) sessionRoom.append(createOption(room.id, room.name));
    }
}

function createOption(value, label) {
    const option = document.createElement("option");
    option.value = value;
    option.textContent = label;
    return option;
}

function applyRoomFromUrl() {
    const roomId = new URLSearchParams(window.location.search).get("roomId");
    if (roomId && roomsById.has(Number(roomId))) {
        roomFilter.value = roomId;
        if ([...sessionRoom.options].some(option => option.value === roomId)) sessionRoom.value = roomId;
    }
}

async function loadSessions() {
    loadingState.classList.remove("hidden");
    emptyState.classList.add("hidden");
    tableWrapper.classList.add("hidden");
    hideMessage(pageMessage);
    const parameters = new URLSearchParams();
    if (roomFilter.value) parameters.set("roomId", roomFilter.value);
    if (statusFilter.value) parameters.set("status", statusFilter.value);
    try {
        const query = parameters.toString();
        renderSessions(await request(query ? `${sessionsApiUrl}?${query}` : sessionsApiUrl));
    } catch (error) {
        showMessage(pageMessage, error.message, true);
    } finally {
        loadingState.classList.add("hidden");
    }
}

function renderSessions(sessions) {
    rows.replaceChildren();
    if (sessions.length === 0) {
        emptyState.classList.remove("hidden");
        return;
    }
    for (const session of sessions) {
        const row = document.createElement("tr");
        row.append(
            createSessionCell(session),
            createCell(session.roomName || roomsById.get(session.roomId)?.name || `Room ${session.roomId}`),
            createStatusCell(session.status),
            createCell(formatDate(session.startedAt)),
            createCell(formatDate(session.endedAt)),
            createActionsCell(session)
        );
        rows.append(row);
    }
    tableWrapper.classList.remove("hidden");
}

function createCell(value) {
    const cell = document.createElement("td");
    cell.textContent = value ?? "—";
    return cell;
}

function createSessionCell(session) {
    const cell = document.createElement("td");
    const button = document.createElement("button");
    button.className = "table-link table-link-button";
    button.type = "button";
    button.textContent = session.name;
    button.addEventListener("click", () => openDetails(session.id));
    const description = document.createElement("div");
    description.className = "table-description";
    description.textContent = session.description || `Created by ${session.createdByName}`;
    cell.append(button, description);
    return cell;
}

function createStatusCell(status) {
    const cell = document.createElement("td");
    const badge = document.createElement("span");
    badge.className = `status ${statusClass(status)}`;
    badge.textContent = statusLabels[status] || status;
    cell.append(badge);
    return cell;
}

function statusClass(status) {
    if (status === "ACTIVE") return "status-active";
    if (status === "PLANNED") return "status-warning";
    if (status === "CANCELLED") return "status-error";
    return "status-inactive";
}

function createActionsCell(session) {
    const cell = document.createElement("td");
    const actions = document.createElement("div");
    actions.className = "row-actions";
    actions.append(actionButton("View", () => openDetails(session.id), true));
    if (session.status === "PLANNED") {
        actions.append(actionButton("Start", () => changeSessionStatus(session.id, "start")));
        actions.append(actionButton("Cancel", () => changeSessionStatus(session.id, "cancel"), true, true));
    } else if (session.status === "ACTIVE") {
        actions.append(actionButton("Complete", () => changeSessionStatus(session.id, "complete")));
        actions.append(actionButton("Cancel", () => changeSessionStatus(session.id, "cancel"), true, true));
    }
    cell.append(actions);
    return cell;
}

function actionButton(label, handler, secondary = false, danger = false) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = `button button-small ${danger ? "button-danger" : secondary ? "button-secondary" : "button-primary"}`;
    button.textContent = label;
    button.addEventListener("click", handler);
    return button;
}

async function changeSessionStatus(id, action) {
    if (action === "cancel" && !window.confirm("Cancel this monitoring session?")) return;
    try {
        await request(`${sessionsApiUrl}/${id}/${action}`, {method: "POST"});
        showMessage(pageMessage, `Session ${action === "start" ? "started" : action === "complete" ? "completed" : "cancelled"}.`);
        await loadSessions();
        if (detailsDialog.open && openSessionId === id) await refreshDetails();
    } catch (error) {
        showMessage(pageMessage, error.message, true);
        if (detailsDialog.open) showMessage(detailsError, error.message, true);
    }
}

async function openDetails(id) {
    openSessionId = id;
    detailsDialog.showModal();
    await refreshDetails();
}

async function refreshDetails() {
    detailsLoading.classList.remove("hidden");
    detailsContent.classList.add("hidden");
    hideMessage(detailsError);
    try {
        const [session, events] = await Promise.all([
            request(`${sessionsApiUrl}/${openSessionId}`),
            request(`${sessionsApiUrl}/${openSessionId}/events`)
        ]);
        renderDetails(session, events);
        detailsLoading.classList.add("hidden");
        detailsContent.classList.remove("hidden");
    } catch (error) {
        detailsLoading.classList.add("hidden");
        showMessage(detailsError, error.message, true);
    }
}

function renderDetails(session, events) {
    detailsTitle.textContent = session.name;
    detailsSummary.replaceChildren(
        detailItem("Room", session.roomName), detailItem("Status", statusLabels[session.status]),
        detailItem("Started", formatDate(session.startedAt)), detailItem("Ended", formatDate(session.endedAt)),
        detailItem("Created by", session.createdByName), detailItem("Created", formatDate(session.createdAt))
    );
    if (session.description) detailsSummary.append(detailItem("Description", session.description, true));

    detailsActions.replaceChildren();
    if (session.status === "PLANNED") {
        detailsActions.append(actionButton("Start session", () => changeSessionStatus(session.id, "start")));
        detailsActions.append(actionButton("Cancel", () => changeSessionStatus(session.id, "cancel"), true, true));
    } else if (session.status === "ACTIVE") {
        detailsActions.append(actionButton("Complete session", () => changeSessionStatus(session.id, "complete")));
        detailsActions.append(actionButton("Cancel", () => changeSessionStatus(session.id, "cancel"), true, true));
    }
    eventFormSection.classList.toggle("hidden", session.status !== "ACTIVE");
    if (session.status === "ACTIVE") {
        eventTime.min = toDateTimeLocal(session.startedAt);
        eventTime.removeAttribute("max");
        if (!eventTime.value) eventTime.value = toDateTimeLocal(new Date().toISOString());
    }
    renderEvents(events);
}

function detailItem(label, value, wide = false) {
    const item = document.createElement("div");
    item.className = wide ? "detail-item detail-item-wide" : "detail-item";
    const heading = document.createElement("span");
    heading.textContent = label;
    const content = document.createElement("strong");
    content.textContent = value ?? "—";
    item.append(heading, content);
    return item;
}

function renderEvents(events) {
    eventsList.replaceChildren();
    eventsEmpty.classList.toggle("hidden", events.length > 0);
    eventsList.classList.toggle("hidden", events.length === 0);
    for (const event of events) {
        const item = document.createElement("li");
        item.className = `timeline-event session-event session-event-${event.category.toLowerCase()}`;
        const title = document.createElement("strong");
        title.textContent = `${categoryLabels[event.category] || event.category}: ${event.title}`;
        const time = document.createElement("span");
        time.className = "timeline-time";
        time.textContent = `${formatDate(event.occurredAt)} · ${event.createdByName}`;
        item.append(title, time);
        if (event.description) {
            const description = document.createElement("span");
            description.className = "timeline-comment";
            description.textContent = event.description;
            item.append(description);
        }
        eventsList.append(item);
    }
}

sessionForm.addEventListener("submit", async event => {
    event.preventDefault();
    hideMessage(sessionFormError);
    try {
        await request(sessionsApiUrl, {
            method: "POST",
            body: JSON.stringify({roomId: Number(sessionRoom.value), name: sessionName.value, description: sessionDescription.value || null})
        });
        sessionForm.reset();
        closeSessionForm();
        showMessage(pageMessage, "Planned monitoring session created.");
        await loadSessions();
    } catch (error) { showMessage(sessionFormError, error.message, true); }
});

eventForm.addEventListener("submit", async event => {
    event.preventDefault();
    hideMessage(eventFormError);
    try {
        await request(`${sessionsApiUrl}/${openSessionId}/events`, {
            method: "POST",
            body: JSON.stringify({category: eventCategory.value, title: eventTitle.value,
                description: eventDescription.value || null, occurredAt: eventTime.value})
        });
        eventForm.reset();
        await refreshDetails();
    } catch (error) { showMessage(eventFormError, error.message, true); }
});

function closeSessionForm() { formPanel.classList.add("hidden"); hideMessage(sessionFormError); }
function formatDate(value) { return value ? new Date(value).toLocaleString() : "—"; }
function toDateTimeLocal(value) {
    const date = new Date(value);
    const local = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
    return local.toISOString().slice(0, 19);
}
function showMessage(element, text, error = false) {
    element.textContent = text;
    element.classList.toggle("message-error", error);
    element.classList.remove("hidden");
}
function hideMessage(element) { element.classList.add("hidden"); }

document.querySelector("#show-create-form").addEventListener("click", () => {
    formPanel.classList.remove("hidden");
    sessionName.focus();
});
document.querySelector("#close-session-form").addEventListener("click", closeSessionForm);
document.querySelector("#cancel-session-form").addEventListener("click", closeSessionForm);
document.querySelector("#close-details").addEventListener("click", () => detailsDialog.close());
filterForm.addEventListener("submit", event => { event.preventDefault(); loadSessions(); });
document.querySelector("#clear-filters").addEventListener("click", () => {
    roomFilter.value = ""; statusFilter.value = ""; loadSessions();
});

initializePage();

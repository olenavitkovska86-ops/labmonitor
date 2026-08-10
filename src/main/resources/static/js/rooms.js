const roomsApiUrl = "/api/rooms";
const labsApiUrl = "/api/labs";
const organizationsApiUrl = "/api/organizations";

const rows = document.querySelector("#room-rows");
const tableWrapper = document.querySelector("#table-wrapper");
const loadingState = document.querySelector("#loading-state");
const emptyState = document.querySelector("#empty-state");
const pageMessage = document.querySelector("#page-message");
const formPanel = document.querySelector("#room-form-panel");
const form = document.querySelector("#room-form");
const formTitle = document.querySelector("#form-title");
const formError = document.querySelector("#form-error");
const idInput = document.querySelector("#room-id");
const labInput = document.querySelector("#room-lab");
const nameInput = document.querySelector("#room-name");
const typeInput = document.querySelector("#room-type");
const floorInput = document.querySelector("#room-floor");
const areaInput = document.querySelector("#room-area");
const searchForm = document.querySelector("#search-form");
const searchInput = document.querySelector("#search-input");
const labFilter = document.querySelector("#lab-filter");

const roomTypeLabels = {
    EXPERIMENT_ROOM: "Experiment room",
    STORAGE_ROOM: "Storage room",
    SERVER_ROOM: "Server room",
    CLEAN_ROOM: "Clean room",
    OFFICE: "Office",
    ENTRANCE: "Entrance",
    OTHER: "Other"
};

let labsById = new Map();
let organizationsById = new Map();
let searchTimer;

async function request(url, options = {}) {
    const response = await fetch(url, options);

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

async function initializePage() {
    try {
        const [labs, organizations] = await Promise.all([
            request(labsApiUrl),
            request(organizationsApiUrl)
        ]);
        labsById = new Map(labs.map(lab => [lab.id, lab]));
        organizationsById = new Map(organizations.map(organization => [organization.id, organization]));
        renderLabOptions(labs);
        applyLabFromUrl();
        await loadRooms();
    } catch (error) {
        loadingState.classList.add("hidden");
        showMessage(pageMessage, error.message, true);
    }
}

function renderLabOptions(labs) {
    for (const lab of labs) {
        const organization = organizationsById.get(lab.organizationId);
        const label = `${lab.name} (${organization?.name || `organization ${lab.organizationId}`})`;
        labInput.append(createOption(lab.id, label));
        labFilter.append(createOption(lab.id, label));
    }
}

function createOption(value, label) {
    const option = document.createElement("option");
    option.value = value;
    option.textContent = label;
    return option;
}

function applyLabFromUrl() {
    const labId = new URLSearchParams(window.location.search).get("labId");

    if (labId && labsById.has(Number(labId))) {
        labFilter.value = labId;
        labInput.value = labId;
        const lab = labsById.get(Number(labId));
        document.querySelector("#back-to-labs").href = `/labs.html?organizationId=${lab.organizationId}`;
    }
}

async function loadRooms() {
    loadingState.classList.remove("hidden");
    emptyState.classList.add("hidden");
    tableWrapper.classList.add("hidden");
    hideMessage(pageMessage);

    try {
        const parameters = new URLSearchParams();
        const search = searchInput.value.trim();
        const labId = labFilter.value;

        if (search) {
            parameters.set("search", search);
        }
        if (labId) {
            parameters.set("labId", labId);
        }

        const query = parameters.toString();
        const rooms = await request(query ? `${roomsApiUrl}?${query}` : roomsApiUrl);
        renderRooms(rooms);
    } catch (error) {
        showMessage(pageMessage, error.message, true);
    } finally {
        loadingState.classList.add("hidden");
    }
}

function renderRooms(rooms) {
    rows.replaceChildren();

    if (rooms.length === 0) {
        emptyState.classList.remove("hidden");
        return;
    }

    for (const room of rooms) {
        const lab = labsById.get(room.labId);
        const row = document.createElement("tr");
        row.append(
            createCell(room.id),
            createRoomLinkCell(room),
            createCell(lab?.name || `Lab ${room.labId}`),
            createCell(roomTypeLabels[room.type] || room.type),
            createCell(room.floor ?? "—"),
            createCell(room.area == null ? "—" : `${room.area} m²`),
            createStatusCell(room.active),
            createActionsCell(room)
        );
        rows.append(row);
    }

    tableWrapper.classList.remove("hidden");
}

function createRoomLinkCell(room) {
    const cell = document.createElement("td");
    const link = document.createElement("a");
    link.className = "table-link";
    link.href = `/sensors.html?roomId=${room.id}`;
    link.textContent = room.name;
    cell.append(link);
    return cell;
}

function createCell(value) {
    const cell = document.createElement("td");
    cell.textContent = value;
    return cell;
}

function createStatusCell(active) {
    const cell = document.createElement("td");
    const status = document.createElement("span");
    status.className = `status ${active ? "status-active" : "status-inactive"}`;
    status.textContent = active ? "Active" : "Inactive";
    cell.append(status);
    return cell;
}

function createActionsCell(room) {
    const cell = document.createElement("td");
    const actions = document.createElement("div");
    actions.className = "row-actions";

    const editButton = createButton("Edit", "button button-secondary button-small");
    editButton.addEventListener("click", () => openEditForm(room));

    const deactivateButton = createButton("Deactivate", "button button-danger button-small");
    deactivateButton.disabled = !room.active;
    deactivateButton.addEventListener("click", () => deactivateRoom(room));

    actions.append(editButton, deactivateButton);
    cell.append(actions);
    return cell;
}

function createButton(label, className) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = className;
    button.textContent = label;
    return button;
}

function openCreateForm() {
    form.reset();
    idInput.value = "";
    labInput.disabled = false;
    labInput.value = labFilter.value;
    formTitle.textContent = "New room";
    hideMessage(formError);
    formPanel.classList.remove("hidden");
    labInput.focus();
}

function openEditForm(room) {
    idInput.value = room.id;
    labInput.value = room.labId;
    labInput.disabled = true;
    nameInput.value = room.name;
    typeInput.value = room.type;
    floorInput.value = room.floor ?? "";
    areaInput.value = room.area ?? "";
    formTitle.textContent = "Edit room";
    hideMessage(formError);
    formPanel.classList.remove("hidden");
    formPanel.scrollIntoView({behavior: "smooth", block: "start"});
}

function closeForm() {
    formPanel.classList.add("hidden");
    form.reset();
    labInput.disabled = false;
    hideMessage(formError);
}

async function saveRoom(event) {
    event.preventDefault();
    hideMessage(formError);

    const id = idInput.value;
    const room = {
        name: nameInput.value.trim(),
        type: typeInput.value,
        floor: floorInput.value === "" ? null : Number(floorInput.value),
        area: areaInput.value === "" ? null : Number(areaInput.value)
    };

    if (!id) {
        room.labId = Number(labInput.value);
    }

    try {
        await request(id ? `${roomsApiUrl}/${id}` : roomsApiUrl, {
            method: id ? "PUT" : "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(room)
        });

        closeForm();
        await loadRooms();
        showMessage(pageMessage, id ? "Room updated." : "Room created.");
    } catch (error) {
        showMessage(formError, error.message, true);
    }
}

async function deactivateRoom(room) {
    const confirmed = window.confirm(`Deactivate room "${room.name}"?`);
    if (!confirmed) {
        return;
    }

    try {
        await request(`${roomsApiUrl}/${room.id}/deactivate`, {method: "POST"});
        await loadRooms();
        showMessage(pageMessage, "Room deactivated.");
    } catch (error) {
        showMessage(pageMessage, error.message, true);
    }
}

function showMessage(element, text, isError = false) {
    element.textContent = text;
    element.classList.toggle("message-error", isError);
    element.classList.remove("hidden");
}

function hideMessage(element) {
    element.classList.add("hidden");
    element.classList.remove("message-error");
}

document.querySelector("#show-create-form").addEventListener("click", openCreateForm);
document.querySelector("#close-form").addEventListener("click", closeForm);
document.querySelector("#cancel-form").addEventListener("click", closeForm);
searchForm.addEventListener("submit", event => {
    event.preventDefault();
    loadRooms();
});
document.querySelector("#clear-search").addEventListener("click", () => {
    searchInput.value = "";
    labFilter.value = "";
    loadRooms();
});
searchInput.addEventListener("input", () => {
    window.clearTimeout(searchTimer);
    searchTimer = window.setTimeout(loadRooms, 300);
});
labFilter.addEventListener("change", loadRooms);
form.addEventListener("submit", saveRoom);

initializePage();

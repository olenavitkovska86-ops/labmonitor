const sensorsApiUrl = "/api/sensors";
const roomsApiUrl = "/api/rooms";
const labsApiUrl = "/api/labs";

const rows = document.querySelector("#sensor-rows");
const tableWrapper = document.querySelector("#table-wrapper");
const loadingState = document.querySelector("#loading-state");
const emptyState = document.querySelector("#empty-state");
const pageMessage = document.querySelector("#page-message");
const formPanel = document.querySelector("#sensor-form-panel");
const form = document.querySelector("#sensor-form");
const formTitle = document.querySelector("#form-title");
const formError = document.querySelector("#form-error");
const idInput = document.querySelector("#sensor-id");
const roomInput = document.querySelector("#sensor-room");
const nameInput = document.querySelector("#sensor-name");
const typeInput = document.querySelector("#sensor-type");
const unitInput = document.querySelector("#sensor-unit");
const safeRangePanel = document.querySelector("#safe-range-panel");
const safeRangeForm = document.querySelector("#safe-range-form");
const safeRangeTitle = document.querySelector("#safe-range-title");
const safeRangeError = document.querySelector("#safe-range-error");
const safeRangeSensorId = document.querySelector("#safe-range-sensor-id");
const minSafeValueInput = document.querySelector("#min-safe-value");
const maxSafeValueInput = document.querySelector("#max-safe-value");
const searchForm = document.querySelector("#search-form");
const searchInput = document.querySelector("#search-input");
const roomFilter = document.querySelector("#room-filter");

const sensorTypeLabels = {
    TEMPERATURE: "Temperature",
    HUMIDITY: "Humidity",
    CO2: "CO₂",
    SMOKE: "Smoke",
    MOTION: "Motion",
    DOOR: "Door",
    PRESSURE: "Pressure",
    LIGHT: "Light",
    NOISE: "Noise",
    ENERGY: "Energy",
    OCCUPANCY: "Occupancy",
    OTHER: "Other"
};

let roomsById = new Map();
let labsById = new Map();
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
        const [rooms, labs] = await Promise.all([
            request(roomsApiUrl),
            request(labsApiUrl)
        ]);
        roomsById = new Map(rooms.map(room => [room.id, room]));
        labsById = new Map(labs.map(lab => [lab.id, lab]));
        renderRoomOptions(rooms);
        applyRoomFromUrl();
        await loadSensors();
    } catch (error) {
        loadingState.classList.add("hidden");
        showMessage(pageMessage, error.message, true);
    }
}

function renderRoomOptions(rooms) {
    for (const room of rooms) {
        const lab = labsById.get(room.labId);
        const label = `${room.name} (${lab?.name || `lab ${room.labId}`})`;
        roomInput.append(createOption(room.id, label));
        roomFilter.append(createOption(room.id, label));
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
        roomInput.value = roomId;
    }
}

async function loadSensors() {
    loadingState.classList.remove("hidden");
    emptyState.classList.add("hidden");
    tableWrapper.classList.add("hidden");
    hideMessage(pageMessage);

    try {
        const parameters = new URLSearchParams();
        const search = searchInput.value.trim();
        const roomId = roomFilter.value;

        if (search) {
            parameters.set("search", search);
        }
        if (roomId) {
            parameters.set("roomId", roomId);
        }

        const query = parameters.toString();
        const sensors = await request(query ? `${sensorsApiUrl}?${query}` : sensorsApiUrl);
        renderSensors(sensors);
    } catch (error) {
        showMessage(pageMessage, error.message, true);
    } finally {
        loadingState.classList.add("hidden");
    }
}

function renderSensors(sensors) {
    rows.replaceChildren();

    if (sensors.length === 0) {
        emptyState.classList.remove("hidden");
        return;
    }

    for (const sensor of sensors) {
        const room = roomsById.get(sensor.roomId);
        const row = document.createElement("tr");
        const currentReadingCell = createCurrentReadingCell(sensor);
        row.append(
            createCell(sensor.id),
            createSensorLinkCell(sensor),
            createCell(room?.name || `Room ${sensor.roomId}`),
            createCell(sensorTypeLabels[sensor.type] || sensor.type),
            createCell(sensor.unit || "—"),
            createDeviceStatusCell(sensor.status),
            currentReadingCell,
            createCell(formatSafeRange(sensor)),
            createStatusCell(sensor.active),
            createActionsCell(sensor)
        );
        rows.append(row);
        loadCurrentReading(sensor, currentReadingCell);
    }

    tableWrapper.classList.remove("hidden");
}

function createSensorLinkCell(sensor) {
    const cell = document.createElement("td");
    const link = document.createElement("a");
    link.className = "table-link";
    link.href = `/sensor-readings.html?sensorId=${sensor.id}`;
    link.textContent = sensor.name;
    cell.append(link);
    return cell;
}

function createCurrentReadingCell(sensor) {
    const cell = document.createElement("td");
    cell.textContent = "Loading...";
    cell.dataset.sensorId = sensor.id;
    return cell;
}

async function loadCurrentReading(sensor, cell) {
    try {
        const response = await fetch(`${sensorsApiUrl}/${sensor.id}/current-reading`);

        if (response.status === 204) {
            cell.textContent = "No readings";
            return;
        }

        if (!response.ok) {
            cell.textContent = "Unavailable";
            return;
        }

        const reading = await response.json();
        cell.textContent = `${reading.value}${reading.unit ? ` ${reading.unit}` : ""}`;
        cell.className = isOutsideSafeRange(sensor, reading.value) ? "value-alert" : "value-safe";
    } catch {
        cell.textContent = "Unavailable";
    }
}

function isOutsideSafeRange(sensor, value) {
    return (sensor.minSafeValue != null && value < sensor.minSafeValue)
        || (sensor.maxSafeValue != null && value > sensor.maxSafeValue);
}

function createCell(value) {
    const cell = document.createElement("td");
    cell.textContent = value;
    return cell;
}

function createDeviceStatusCell(statusValue) {
    const classNames = {
        ONLINE: "status-active",
        OFFLINE: "status-inactive",
        MAINTENANCE: "status-warning",
        ERROR: "status-error"
    };
    const cell = document.createElement("td");
    const status = document.createElement("span");
    status.className = `status ${classNames[statusValue] || "status-inactive"}`;
    status.textContent = statusValue;
    cell.append(status);
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

function formatSafeRange(sensor) {
    const minimum = sensor.minSafeValue ?? "—";
    const maximum = sensor.maxSafeValue ?? "—";
    const unit = sensor.unit ? ` ${sensor.unit}` : "";
    return `${minimum} – ${maximum}${unit}`;
}

function createActionsCell(sensor) {
    const cell = document.createElement("td");
    const actions = document.createElement("div");
    actions.className = "row-actions";

    const editButton = createButton("Edit", "button button-secondary button-small");
    editButton.addEventListener("click", () => openEditForm(sensor));

    const rangeButton = createButton("Safe range", "button button-secondary button-small");
    rangeButton.addEventListener("click", () => openSafeRangeForm(sensor));

    const deactivateButton = createButton("Deactivate", "button button-danger button-small");
    deactivateButton.disabled = !sensor.active;
    deactivateButton.addEventListener("click", () => deactivateSensor(sensor));

    actions.append(editButton, rangeButton, deactivateButton);
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
    roomInput.disabled = false;
    typeInput.disabled = false;
    roomInput.value = roomFilter.value;
    formTitle.textContent = "New sensor";
    hideMessage(formError);
    safeRangePanel.classList.add("hidden");
    formPanel.classList.remove("hidden");
    roomInput.focus();
}

function openEditForm(sensor) {
    idInput.value = sensor.id;
    roomInput.value = sensor.roomId;
    roomInput.disabled = true;
    nameInput.value = sensor.name;
    typeInput.value = sensor.type;
    typeInput.disabled = true;
    unitInput.value = sensor.unit || "";
    formTitle.textContent = "Edit sensor";
    hideMessage(formError);
    safeRangePanel.classList.add("hidden");
    formPanel.classList.remove("hidden");
    formPanel.scrollIntoView({behavior: "smooth", block: "start"});
}

function closeForm() {
    formPanel.classList.add("hidden");
    form.reset();
    roomInput.disabled = false;
    typeInput.disabled = false;
    hideMessage(formError);
}

async function saveSensor(event) {
    event.preventDefault();
    hideMessage(formError);

    const id = idInput.value;
    const sensor = {
        name: nameInput.value.trim(),
        unit: unitInput.value.trim() || null
    };

    if (!id) {
        sensor.roomId = Number(roomInput.value);
        sensor.type = typeInput.value;
    }

    try {
        await request(id ? `${sensorsApiUrl}/${id}` : sensorsApiUrl, {
            method: id ? "PUT" : "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(sensor)
        });

        closeForm();
        await loadSensors();
        showMessage(pageMessage, id ? "Sensor updated." : "Sensor created.");
    } catch (error) {
        showMessage(formError, error.message, true);
    }
}

function openSafeRangeForm(sensor) {
    safeRangeSensorId.value = sensor.id;
    minSafeValueInput.value = sensor.minSafeValue ?? "";
    maxSafeValueInput.value = sensor.maxSafeValue ?? "";
    safeRangeTitle.textContent = `Safe range: ${sensor.name}`;
    hideMessage(safeRangeError);
    formPanel.classList.add("hidden");
    safeRangePanel.classList.remove("hidden");
    safeRangePanel.scrollIntoView({behavior: "smooth", block: "start"});
}

function closeSafeRangeForm() {
    safeRangePanel.classList.add("hidden");
    safeRangeForm.reset();
    hideMessage(safeRangeError);
}

async function saveSafeRange(event) {
    event.preventDefault();
    hideMessage(safeRangeError);

    const safeRange = {
        minSafeValue: minSafeValueInput.value === "" ? null : Number(minSafeValueInput.value),
        maxSafeValue: maxSafeValueInput.value === "" ? null : Number(maxSafeValueInput.value)
    };

    try {
        await request(`${sensorsApiUrl}/${safeRangeSensorId.value}/safe-range`, {
            method: "PUT",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(safeRange)
        });

        closeSafeRangeForm();
        await loadSensors();
        showMessage(pageMessage, "Safe range updated.");
    } catch (error) {
        showMessage(safeRangeError, error.message, true);
    }
}

async function deactivateSensor(sensor) {
    const confirmed = window.confirm(`Deactivate sensor "${sensor.name}"?`);
    if (!confirmed) {
        return;
    }

    try {
        await request(`${sensorsApiUrl}/${sensor.id}/deactivate`, {method: "POST"});
        await loadSensors();
        showMessage(pageMessage, "Sensor deactivated.");
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
document.querySelector("#close-safe-range").addEventListener("click", closeSafeRangeForm);
document.querySelector("#cancel-safe-range").addEventListener("click", closeSafeRangeForm);
searchForm.addEventListener("submit", event => {
    event.preventDefault();
    loadSensors();
});
document.querySelector("#clear-search").addEventListener("click", () => {
    searchInput.value = "";
    roomFilter.value = "";
    loadSensors();
});
searchInput.addEventListener("input", () => {
    window.clearTimeout(searchTimer);
    searchTimer = window.setTimeout(loadSensors, 300);
});
roomFilter.addEventListener("change", loadSensors);
form.addEventListener("submit", saveSensor);
safeRangeForm.addEventListener("submit", saveSafeRange);

initializePage();

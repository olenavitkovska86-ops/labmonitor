const sensorsApiUrl = "/api/sensors";
const roomsApiUrl = "/api/rooms";
const labsApiUrl = "/api/labs";
const organizationsApiUrl = "/api/organizations";

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
const roomExportPeriod = document.querySelector("#room-export-period");
const exportRoomReadingsButton = document.querySelector("#export-room-readings");

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
let organizationsById = new Map();
let visibleSensors = [];
let sensorRefreshInProgress = false;
let searchTimer;

async function request(url, options = {}) {
    return apiRequest(url, options);
}

async function initializePage() {
    try {
        await labMonitorAuthReady;
        const [rooms, labs, organizations] = await Promise.all([
            request(roomsApiUrl),
            request(labsApiUrl),
            request(organizationsApiUrl)
        ]);
        roomsById = new Map(rooms.map(room => [room.id, room]));
        labsById = new Map(labs.map(lab => [lab.id, lab]));
        organizationsById = new Map(organizations.map(organization => [organization.id, organization]));
        renderRoomOptions(rooms);
        applyRoomFromUrl();
        updateRoomExportButton();
        renderSensorBreadcrumbs();
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

function renderSensorBreadcrumbs() {
    const room = roomsById.get(Number(roomFilter.value));
    const lab = room && labsById.get(room.labId);
    const organization = lab && organizationsById.get(lab.organizationId);
    const items = [
        {label: "Home", href: "/"},
        {label: "Organizations", href: "/organizations.html"}
    ];

    if (organization) {
        items.push({label: organization.name, href: `/labs.html?organizationId=${organization.id}`});
    }
    if (lab) {
        items.push({label: lab.name, href: `/rooms.html?labId=${lab.id}`});
    } else {
        items.push({label: "Labs", href: "/labs.html"});
    }
    if (room) {
        items.push({label: room.name, href: `/sensors.html?roomId=${room.id}`});
    } else {
        items.push({label: "Rooms", href: "/rooms.html"});
    }
    items.push({label: "Sensors"});
    renderBreadcrumbs(items);
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
    visibleSensors = sensors;
    rows.replaceChildren();

    if (sensors.length === 0) {
        emptyState.classList.remove("hidden");
        return;
    }

    for (const sensor of sensors) {
        const room = roomsById.get(sensor.roomId);
        const row = document.createElement("tr");
        const currentReadingCell = createCurrentReadingCell(sensor);
        const deviceStatusCell = createDeviceStatusCell(sensor.id, sensor.status);
        row.append(
            createCell(sensor.id),
            createSensorLinkCell(sensor),
            createCell(room?.name || `Room ${sensor.roomId}`),
            createCell(sensorTypeLabels[sensor.type] || sensor.type),
            createCell(sensor.unit || "—"),
            deviceStatusCell,
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
        const reading = await request(`${sensorsApiUrl}/${sensor.id}/current-reading`);
        if (!reading) {
            cell.textContent = "No readings";
            cell.className = "";
            return;
        }
        cell.textContent = `${reading.value}${reading.unit ? ` ${reading.unit}` : ""}`;
        cell.className = isOutsideSafeRange(sensor, reading.value) ? "value-alert" : "value-safe";
    } catch {
        cell.textContent = "Unavailable";
    }
}

async function refreshVisibleSensors() {
    if (sensorRefreshInProgress) return;
    sensorRefreshInProgress = true;
    try {
        await Promise.allSettled(visibleSensors.map(refreshVisibleSensor));
    } finally {
        sensorRefreshInProgress = false;
    }
}

async function refreshVisibleSensor(sensor) {
    const currentReadingCell = rows.querySelector(`[data-sensor-id="${sensor.id}"]`);
    const deviceStatusCell = rows.querySelector(`[data-device-status-sensor-id="${sensor.id}"]`);
    if (!currentReadingCell || !deviceStatusCell) return;

    const currentSensor = await request(`${sensorsApiUrl}/${sensor.id}`);
    Object.assign(sensor, currentSensor);
    renderDeviceStatus(deviceStatusCell, currentSensor.status);
    await loadCurrentReading(sensor, currentReadingCell);
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

function createDeviceStatusCell(sensorId, statusValue) {
    const cell = document.createElement("td");
    cell.dataset.deviceStatusSensorId = sensorId;
    renderDeviceStatus(cell, statusValue);
    return cell;
}

function renderDeviceStatus(cell, statusValue) {
    const classNames = {
        ONLINE: "status-active",
        OFFLINE: "status-inactive",
        MAINTENANCE: "status-warning",
        ERROR: "status-error"
    };
    cell.replaceChildren();
    const status = document.createElement("span");
    status.className = `status ${classNames[statusValue] || "status-inactive"}`;
    status.textContent = statusValue;
    cell.append(status);
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

    const activityButton = createButton(
        sensor.active ? "Deactivate" : "Activate",
        `button ${sensor.active ? "button-danger" : "button-primary"} button-small`
    );
    activityButton.addEventListener("click", () => changeSensorActivity(sensor));

    const canManage = window.labMonitorAuth?.has("sensors.manage");
    const canUpdateSettings = window.labMonitorAuth?.hasForOrganization(
        "sensors.settings.update", sensor.organizationId);
    if (canManage) actions.append(editButton);
    if (canUpdateSettings) actions.append(rangeButton);
    if (canManage) actions.append(activityButton);
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

async function changeSensorActivity(sensor) {
    const action = sensor.active ? "deactivate" : "activate";
    const confirmed = window.confirm(`${sensor.active ? "Deactivate" : "Activate"} sensor "${sensor.name}"?`);
    if (!confirmed) {
        return;
    }

    try {
        await request(`${sensorsApiUrl}/${sensor.id}/${action}`, {method: "POST"});
        await loadSensors();
        showMessage(pageMessage, sensor.active ? "Sensor deactivated." : "Sensor activated.");
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

function formatLocalDateTime(date) {
    const pad = value => String(value).padStart(2, "0");
    return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
        + `T${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`;
}

function updateRoomExportButton() {
    const hasRoom = Boolean(roomFilter.value);
    exportRoomReadingsButton.disabled = !hasRoom;
    exportRoomReadingsButton.title = hasRoom
        ? "Export readings from all sensors in this room"
        : "Select a room to export its readings";
}

async function exportRoomReadings() {
    const roomId = roomFilter.value;
    if (!roomId) {
        return;
    }

    const originalLabel = exportRoomReadingsButton.textContent;
    exportRoomReadingsButton.disabled = true;
    exportRoomReadingsButton.textContent = "Preparing...";
    hideMessage(pageMessage);

    try {
        const hours = Number(roomExportPeriod.value);
        const to = new Date();
        const from = new Date(to.getTime() - hours * 60 * 60 * 1000);
        const parameters = new URLSearchParams({
            roomId,
            from: formatLocalDateTime(from),
            to: formatLocalDateTime(to)
        });
        const response = await apiFetch(`/api/sensor-readings/export?${parameters}`);
        if (!response.ok) {
            const error = await response.json().catch(() => ({}));
            throw new Error(error.message || `Export failed with status ${response.status}`);
        }

        const blobUrl = URL.createObjectURL(await response.blob());
        const link = document.createElement("a");
        link.href = blobUrl;
        const disposition = response.headers.get("Content-Disposition") || "";
        link.download = disposition.match(/filename="([^"]+)"/)?.[1] || `room-${roomId}-readings.csv`;
        link.click();
        URL.revokeObjectURL(blobUrl);
        showMessage(pageMessage, "Room CSV exported.");
    } catch (error) {
        showMessage(pageMessage, error.message, true);
    } finally {
        updateRoomExportButton();
        exportRoomReadingsButton.textContent = originalLabel;
    }
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
    updateRoomExportButton();
    renderSensorBreadcrumbs();
    loadSensors();
});
searchInput.addEventListener("input", () => {
    window.clearTimeout(searchTimer);
    searchTimer = window.setTimeout(loadSensors, 300);
});
roomFilter.addEventListener("change", () => {
    updateRoomExportButton();
    renderSensorBreadcrumbs();
    loadSensors();
});
exportRoomReadingsButton.addEventListener("click", exportRoomReadings);
form.addEventListener("submit", saveSensor);
safeRangeForm.addEventListener("submit", saveSafeRange);

initializePage();
document.addEventListener("labmonitor:refresh", refreshVisibleSensors);

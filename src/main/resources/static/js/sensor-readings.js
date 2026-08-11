const sensorsApiUrl = "/api/sensors";
const readingsApiUrl = "/api/sensor-readings";
const roomsApiUrl = "/api/rooms";
const labsApiUrl = "/api/labs";
const organizationsApiUrl = "/api/organizations";
const sensorId = new URLSearchParams(window.location.search).get("sensorId");

const sensorTitle = document.querySelector("#sensor-title");
const sensorDescription = document.querySelector("#sensor-description");
const currentReading = document.querySelector("#current-reading");
const safeRange = document.querySelector("#safe-range");
const lastMeasured = document.querySelector("#last-measured");
const rows = document.querySelector("#reading-rows");
const tableWrapper = document.querySelector("#table-wrapper");
const loadingState = document.querySelector("#loading-state");
const emptyState = document.querySelector("#empty-state");
const pageMessage = document.querySelector("#page-message");
const formPanel = document.querySelector("#reading-form-panel");
const form = document.querySelector("#reading-form");
const formError = document.querySelector("#form-error");
const valueInput = document.querySelector("#reading-value");
const measuredAtInput = document.querySelector("#measured-at");

let sensor;
let room;
let lab;
let organization;

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
    if (!sensorId) {
        renderBreadcrumbs([
            {label: "Home", href: "/"},
            {label: "Organizations", href: "/organizations.html"},
            {label: "Labs", href: "/labs.html"},
            {label: "Rooms", href: "/rooms.html"},
            {label: "Sensors", href: "/sensors.html"},
            {label: "Sensor readings"}
        ]);
        loadingState.classList.add("hidden");
        document.querySelector("#show-reading-form").disabled = true;
        showMessage(pageMessage, "Select a sensor from the Sensors page.", true);
        return;
    }

    try {
        sensor = await request(`${sensorsApiUrl}/${sensorId}`);
        [room, lab, organization] = await Promise.all([
            request(`${roomsApiUrl}/${sensor.roomId}`),
            request(`${labsApiUrl}/${sensor.labId}`),
            request(`${organizationsApiUrl}/${sensor.organizationId}`)
        ]);
        renderSensorDetails();
        await loadReadings();
    } catch (error) {
        loadingState.classList.add("hidden");
        showMessage(pageMessage, error.message, true);
    }
}

function renderSensorDetails() {
    sensorTitle.textContent = sensor.name;
    sensorDescription.textContent = `${sensor.type} sensor · ${sensor.status}`;
    safeRange.textContent = formatSafeRange();
    renderBreadcrumbs([
        {label: "Home", href: "/"},
        {label: "Organizations", href: "/organizations.html"},
        {label: organization.name, href: `/labs.html?organizationId=${organization.id}`},
        {label: lab.name, href: `/rooms.html?labId=${lab.id}`},
        {label: room.name, href: `/sensors.html?roomId=${room.id}`},
        {label: sensor.name}
    ]);
}

async function loadReadings() {
    loadingState.classList.remove("hidden");
    emptyState.classList.add("hidden");
    tableWrapper.classList.add("hidden");
    hideMessage(pageMessage);

    try {
        const readings = await request(`${sensorsApiUrl}/${sensorId}/readings`);
        renderReadings(readings);
    } catch (error) {
        showMessage(pageMessage, error.message, true);
    } finally {
        loadingState.classList.add("hidden");
    }
}

function renderReadings(readings) {
    rows.replaceChildren();

    if (readings.length === 0) {
        currentReading.textContent = "No readings";
        lastMeasured.textContent = "—";
        emptyState.classList.remove("hidden");
        return;
    }

    const latest = readings[0];
    currentReading.textContent = formatValue(latest.value);
    currentReading.className = `summary-value ${isOutsideSafeRange(latest.value) ? "value-alert" : "value-safe"}`;
    lastMeasured.textContent = formatDate(latest.measuredAt);

    for (const reading of readings) {
        const outsideRange = isOutsideSafeRange(reading.value);
        const row = document.createElement("tr");
        row.append(
            createCell(reading.id),
            createValueCell(reading.value, outsideRange),
            createCell(formatDate(reading.measuredAt)),
            createCell(formatDate(reading.createdAt)),
            createCell(outsideRange ? "Outside range" : "Safe")
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

function createValueCell(value, outsideRange) {
    const cell = createCell(formatValue(value));
    cell.className = outsideRange ? "value-alert" : "value-safe";
    return cell;
}

function formatValue(value) {
    return `${value}${sensor.unit ? ` ${sensor.unit}` : ""}`;
}

function formatSafeRange() {
    const minimum = sensor.minSafeValue ?? "—";
    const maximum = sensor.maxSafeValue ?? "—";
    const unit = sensor.unit ? ` ${sensor.unit}` : "";
    return `${minimum} – ${maximum}${unit}`;
}

function formatDate(value) {
    return new Intl.DateTimeFormat(undefined, {
        dateStyle: "medium",
        timeStyle: "medium"
    }).format(new Date(value));
}

function isOutsideSafeRange(value) {
    return (sensor.minSafeValue != null && value < sensor.minSafeValue)
        || (sensor.maxSafeValue != null && value > sensor.maxSafeValue);
}

function openForm() {
    form.reset();
    hideMessage(formError);
    formPanel.classList.remove("hidden");
    valueInput.focus();
}

function closeForm() {
    formPanel.classList.add("hidden");
    form.reset();
    hideMessage(formError);
}

async function saveReading(event) {
    event.preventDefault();
    hideMessage(formError);

    const reading = {
        sensorId: Number(sensorId),
        value: Number(valueInput.value),
        measuredAt: measuredAtInput.value || null
    };

    try {
        await request(readingsApiUrl, {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(reading)
        });

        closeForm();
        await loadReadings();
        showMessage(pageMessage, "Sensor reading added.");
    } catch (error) {
        showMessage(formError, error.message, true);
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

document.querySelector("#show-reading-form").addEventListener("click", openForm);
document.querySelector("#close-reading-form").addEventListener("click", closeForm);
document.querySelector("#cancel-reading-form").addEventListener("click", closeForm);
form.addEventListener("submit", saveReading);

initializePage();

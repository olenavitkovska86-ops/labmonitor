const clientSensorId = new URLSearchParams(window.location.search).get("sensorId");
const clientLoading = document.querySelector("#client-loading");
const clientContent = document.querySelector("#client-content");
const clientMessage = document.querySelector("#client-message");
const clientInterval = document.querySelector("#client-interval");
const clientStart = document.querySelector("#start-client");
const clientStop = document.querySelector("#stop-client");
let clientSensor;
let readingTimer;
let readingStep = 0;
let sendingReading = false;

async function initializeSensorClient() {
    if (!clientSensorId) return showClientMessage("Select a sensor from the Sensors page.", true);
    try {
        const auth = await labMonitorAuthReady;
        if (!auth.has("system.read")) throw new Error("SUPER_ADMIN access is required for experimental reading ingestion.");
        clientSensor = await apiRequest(`/api/sensors/${clientSensorId}`);
        renderBreadcrumbs([{label: "Overview", href: "/analytics.html"}, {label: "Monitor", href: "/monitor.html"},
            {label: "Sensors", href: `/sensors.html?roomId=${clientSensor.roomId}`}, {label: "Data client"}]);
        document.querySelector("#client-title").textContent = clientSensor.name;
        document.querySelector("#client-type").textContent = clientSensor.type;
        document.querySelector("#client-unit").textContent = clientSensor.unit || "—";
        document.querySelector("#client-range").textContent = `${clientSensor.minSafeValue ?? "—"} – ${clientSensor.maxSafeValue ?? "—"}`;
        clientLoading.classList.add("hidden");
        clientContent.classList.remove("hidden");
    } catch (error) {
        clientLoading.classList.add("hidden");
        showClientMessage(error.message, true);
    }
}

function startReadings() {
    if (readingTimer) return;
    readingStep = 0;
    clientInterval.disabled = true;
    clientStart.classList.add("hidden"); clientStop.classList.remove("hidden");
    document.querySelector("#client-state").textContent = "Sending";
    document.querySelector("#client-state").className = "status status-active";
    clientMessage.classList.add("hidden");
    submitReading();
    readingTimer = window.setInterval(submitReading, Number(clientInterval.value) * 1000);
}

function generatedValue() {
    const minimum = clientSensor.minSafeValue;
    const maximum = clientSensor.maxSafeValue;
    const width = minimum != null && maximum != null
        ? maximum - minimum
        : Math.max(Math.abs(maximum ?? minimum ?? 10) * 0.1, 1);
    const center = minimum != null && maximum != null ? (minimum + maximum) / 2
        : maximum != null ? maximum - width * 0.25 : minimum != null ? minimum + width * 0.25 : 0;
    const cycle = [0, 0.08, 0.18, 0.32, 0.48, 0.62, 0.85, 1.08, 1.22, 0.9, 0.45, 0.12];
    const offset = cycle[readingStep++ % cycle.length] * width;
    return Number((center + offset).toFixed(3));
}

async function submitReading() {
    if (sendingReading) return;
    sendingReading = true;
    const value = generatedValue();
    try {
        const reading = await apiRequest("/api/sensor-readings", {
            method: "POST", body: JSON.stringify({sensorId: Number(clientSensorId), value})
        });
        document.querySelector("#client-reading").textContent = `${reading.value}${reading.unit ? ` ${reading.unit}` : ""}`;
    } catch (error) {
        stopReadings();
        showClientMessage(error.message, true);
    } finally { sendingReading = false; }
}

function stopReadings() {
    window.clearInterval(readingTimer);
    readingTimer = null;
    clientInterval.disabled = false;
    clientStart.classList.remove("hidden"); clientStop.classList.add("hidden");
    document.querySelector("#client-state").textContent = "Stopped";
    document.querySelector("#client-state").className = "status status-inactive";
}

function showClientMessage(message, error = false) {
    clientMessage.textContent = message;
    clientMessage.className = `message ${error ? "message-error" : "message-success"}`;
}

clientStart.addEventListener("click", startReadings);
clientStop.addEventListener("click", stopReadings);
window.addEventListener("pagehide", stopReadings);
initializeSensorClient();

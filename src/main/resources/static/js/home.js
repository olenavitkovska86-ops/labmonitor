async function loadAlertCount() {
    const token = localStorage.getItem("token");
    if (!token) return;

    try {
        const response = await fetch("/api/alerts/unresolved-count", {
            headers: {Authorization: `Bearer ${token}`}
        });
        if (!response.ok) return;

        const result = await response.json();
        const badge = document.querySelector("#alert-count");
        badge.textContent = result.unresolvedAlerts > 99 ? "99+" : result.unresolvedAlerts;
        badge.setAttribute(
            "aria-label",
            `${result.unresolvedAlerts} unresolved ${result.unresolvedAlerts === 1 ? "alert" : "alerts"}`
        );
        badge.classList.toggle("alert-count-zero", result.unresolvedAlerts === 0);
        badge.classList.remove("hidden");
    } catch {
        // The home page remains usable when the optional counter cannot be loaded.
    }
}

const simulatorPanel = document.querySelector("#simulator-panel");
const simulatorDescription = document.querySelector("#simulator-description");
const simulatorInterval = document.querySelector("#simulator-interval");
const startSimulatorButton = document.querySelector("#start-simulator");
const stopSimulatorButton = document.querySelector("#stop-simulator");
const simulatorMessage = document.querySelector("#simulator-message");

async function simulatorRequest(path, options = {}) {
    const token = localStorage.getItem("token");
    if (!token) return null;
    const response = await fetch(`/api/simulator${path}`, {
        ...options,
        headers: {
            ...(options.headers || {}),
            Authorization: `Bearer ${token}`
        }
    });
    if (response.status === 401) {
        localStorage.removeItem("token");
        const error = new Error("Your session expired. Log in again to control the simulator.");
        error.status = 401;
        throw error;
    }
    if (response.status === 403) {
        const error = new Error("Simulator control requires an actual SUPER_ADMIN or LAB_ADMIN account.");
        error.status = 403;
        throw error;
    }
    if (response.ok) return response.json();
    const error = await response.json().catch(() => ({}));
    throw new Error(error.message || `Request failed with status ${response.status}`);
}

async function loadSimulatorStatus() {
    try {
        const status = await simulatorRequest("/status");
        if (!status) return;
        simulatorPanel.classList.remove("hidden");
        renderSimulatorStatus(status);
    } catch (error) {
        if (error.status === 403) return;
        if (error.status === 401) {
            window.location.href = "/login.html";
            return;
        }
        showSimulatorError(error.message);
    }
}

function renderSimulatorStatus(status) {
    const simulatedCount = Math.min(status.eligibleSensors, status.maxSensors);
    simulatorDescription.textContent = status.enabled
        ? `Running every ${status.intervalSeconds === 5 ? "5 seconds" : "minute"} for ${simulatedCount} sensors.`
        : `${status.eligibleSensors} eligible sensors. The simulator is off.`;
    simulatorInterval.value = String(status.intervalSeconds);
    simulatorInterval.disabled = status.enabled;
    startSimulatorButton.classList.toggle("hidden", status.enabled);
    stopSimulatorButton.classList.toggle("hidden", !status.enabled);
    simulatorMessage.classList.add("hidden");
}

async function startSimulator() {
    try {
        const status = await simulatorRequest("/start", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({intervalSeconds: Number(simulatorInterval.value)})
        });
        if (status) renderSimulatorStatus(status);
    } catch (error) {
        showSimulatorError(error.message);
    }
}

async function stopSimulator() {
    try {
        const status = await simulatorRequest("/stop", {method: "POST"});
        if (status) renderSimulatorStatus(status);
    } catch (error) {
        showSimulatorError(error.message);
    }
}

function showSimulatorError(message) {
    simulatorMessage.textContent = message;
    simulatorMessage.classList.remove("hidden");
}

startSimulatorButton.addEventListener("click", startSimulator);
stopSimulatorButton.addEventListener("click", stopSimulator);

loadAlertCount();
loadSimulatorStatus();
document.addEventListener("labmonitor:refresh", () => {
    loadAlertCount();
    loadSimulatorStatus();
});

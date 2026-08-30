const iphoneParameters = new URLSearchParams(window.location.search);
const iphoneSensorId = iphoneParameters.get("sensorId");
const iphoneDeviceChannel = iphoneParameters.get("channel");
const iphoneDeviceMode = Boolean(iphoneDeviceChannel);
const iphoneMessage = document.querySelector("#iphone-message");
const iphoneStart = document.querySelector("#start-iphone");
const iphoneStop = document.querySelector("#stop-iphone");
let motionSamples = [];
let motionTimer;
let readingsSent = 0;
let sendingReading = false;

async function initializeIphoneSensor() {
    if (!iphoneSensorId && !iphoneDeviceMode) return showIphoneMessage("Open this client from a sensor or data-client channel.", true);
    try {
        if (iphoneDeviceMode) {
            renderBreadcrumbs([{label: "LabMonitor", href: "/"}, {label: "Motion client"}]);
            document.querySelector("#iphone-title").textContent = `Motion client · ${iphoneDeviceChannel}`;
            document.querySelector("#iphone-intro").textContent = "Authenticate as a device and send motion readings through the configured channel.";
            document.querySelector("#iphone-device-setup").classList.remove("hidden");
            revealIphoneContent();
            return validateMotionEnvironment();
        }
        const sensor = await apiRequest(`/api/sensors/${iphoneSensorId}`);
        renderBreadcrumbs([{label: "Overview", href: "/analytics.html"}, {label: "Monitor", href: "/monitor.html"},
            {label: "Sensors", href: `/sensors.html?roomId=${sensor.roomId}`}, {label: "Motion client"}]);
        document.querySelector("#iphone-title").textContent = `Connect motion client to ${sensor.name}`;
        revealIphoneContent();
        validateMotionEnvironment();
    } catch (error) {
        document.querySelector("#iphone-loading").classList.add("hidden");
        showIphoneMessage(error.message, true);
    }
}

function revealIphoneContent() {
    document.querySelector("#iphone-loading").classList.add("hidden");
    document.querySelector("#iphone-content").classList.remove("hidden");
}

function validateMotionEnvironment() {
    if (!window.isSecureContext) showIphoneMessage("Motion access requires HTTPS on mobile devices.", true);
    else if (!("DeviceMotionEvent" in window)) showIphoneMessage("Device motion is not available in this browser.", true);
}

async function startIphoneMotion() {
    try {
        if (iphoneDeviceMode && !document.querySelector("#iphone-device-token").value.trim()) {
            throw new Error("Paste the device credential before starting.");
        }
        if (typeof window.DeviceMotionEvent?.requestPermission === "function") {
            const permission = await window.DeviceMotionEvent.requestPermission();
            if (permission !== "granted") throw new Error("Motion permission was not granted.");
        }
        motionSamples = [];
        readingsSent = 0;
        window.addEventListener("devicemotion", collectMotionSample);
        motionTimer = window.setInterval(sendMotionReading, 500);
        iphoneStart.classList.add("hidden"); iphoneStop.classList.remove("hidden");
        document.querySelector("#iphone-state").textContent = "Sending";
        iphoneMessage.classList.add("hidden");
    } catch (error) { showIphoneMessage(error.message, true); }
}

function collectMotionSample(event) {
    const source = event.acceleration || event.accelerationIncludingGravity;
    if (source?.x == null || source?.y == null || source?.z == null) return;
    let magnitude = Math.hypot(source.x, source.y, source.z);
    if (!event.acceleration) magnitude = Math.abs(magnitude - 9.80665);
    motionSamples.push(magnitude);
    document.querySelector("#iphone-current").textContent = `${magnitude.toFixed(3)} m/s²`;
}

async function sendMotionReading() {
    if (!motionSamples.length || sendingReading) return;
    const samples = motionSamples.splice(0);
    const rms = Math.sqrt(samples.reduce((sum, value) => sum + value * value, 0) / samples.length);
    sendingReading = true;
    try {
        if (iphoneDeviceMode) await sendDeviceMotionReading(rms);
        else await apiRequest("/api/sensor-readings", {method: "POST", body: JSON.stringify({sensorId: Number(iphoneSensorId), value: Number(rms.toFixed(3))})});
        readingsSent += 1;
        document.querySelector("#iphone-sent").textContent = `${rms.toFixed(3)} m/s²`;
        document.querySelector("#iphone-count").textContent = String(readingsSent);
    } catch (error) {
        stopIphoneMotion();
        showIphoneMessage(error.message, true);
    } finally { sendingReading = false; }
}

async function sendDeviceMotionReading(rms) {
    const token = document.querySelector("#iphone-device-token").value.trim();
    const measuredAt = new Date().toISOString();
    const messageId = typeof crypto.randomUUID === "function"
        ? crypto.randomUUID()
        : `iphone-${Date.now()}-${Math.random().toString(16).slice(2)}`;
    const response = await fetch("/api/device/readings", {
        method: "POST",
        headers: {"Authorization": `Device ${token}`, "Content-Type": "application/json"},
        body: JSON.stringify({channel: iphoneDeviceChannel, value: Number(rms.toFixed(3)), measuredAt, messageId})
    });
    if (response.ok) return response.json();
    const error = await response.json().catch(() => ({}));
    if (response.status === 401) throw new Error("The device credential is invalid, revoked, or the device is disabled.");
    throw new Error(error.message || `Reading was rejected (${response.status}).`);
}

function stopIphoneMotion() {
    window.removeEventListener("devicemotion", collectMotionSample);
    window.clearInterval(motionTimer);
    motionTimer = null; motionSamples = [];
    iphoneStart.classList.remove("hidden"); iphoneStop.classList.add("hidden");
    document.querySelector("#iphone-state").textContent = "Stopped";
}

function showIphoneMessage(message, error = false) {
    iphoneMessage.textContent = message;
    iphoneMessage.className = `message ${error ? "message-error" : "message-success"}`;
}

iphoneStart.addEventListener("click", startIphoneMotion);
iphoneStop.addEventListener("click", stopIphoneMotion);
window.addEventListener("pagehide", stopIphoneMotion);
initializeIphoneSensor();

const iphoneSensorId = new URLSearchParams(window.location.search).get("sensorId");
const iphoneMessage = document.querySelector("#iphone-message");
const iphoneStart = document.querySelector("#start-iphone");
const iphoneStop = document.querySelector("#stop-iphone");
let motionSamples = [];
let motionTimer;
let readingsSent = 0;
let sendingReading = false;

async function initializeIphoneSensor() {
    if (!iphoneSensorId) return showIphoneMessage("Select a sensor from the Sensors page.", true);
    try {
        const auth = await labMonitorAuthReady;
        if (!auth.has("system.read")) throw new Error("SUPER_ADMIN access is required for experimental reading ingestion.");
        const sensor = await apiRequest(`/api/sensors/${iphoneSensorId}`);
        renderBreadcrumbs([{label: "Overview", href: "/analytics.html"}, {label: "Monitor", href: "/monitor.html"},
            {label: "Sensors", href: `/sensors.html?roomId=${sensor.roomId}`}, {label: "Connect iPhone"}]);
        document.querySelector("#iphone-title").textContent = `Connect iPhone to ${sensor.name}`;
        document.querySelector("#iphone-loading").classList.add("hidden");
        document.querySelector("#iphone-content").classList.remove("hidden");
        if (!window.isSecureContext) showIphoneMessage("Motion access normally requires HTTPS on iPhone.", true);
        if (!("DeviceMotionEvent" in window)) showIphoneMessage("Device motion is not available in this browser.", true);
    } catch (error) {
        document.querySelector("#iphone-loading").classList.add("hidden");
        showIphoneMessage(error.message, true);
    }
}

async function startIphoneMotion() {
    try {
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
        await apiRequest("/api/sensor-readings", {method: "POST", body: JSON.stringify({sensorId: Number(iphoneSensorId), value: Number(rms.toFixed(3))})});
        readingsSent += 1;
        document.querySelector("#iphone-sent").textContent = `${rms.toFixed(3)} m/s²`;
        document.querySelector("#iphone-count").textContent = String(readingsSent);
    } catch (error) {
        stopIphoneMotion();
        showIphoneMessage(error.message, true);
    } finally { sendingReading = false; }
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

const deviceState = {devices: [], organizations: [], labs: [], rooms: [], sensors: [], selected: null, channels: [], credentials: []};
const byId = id => document.getElementById(id);

const defaultUnitsBySensorType = {
    TEMPERATURE: "°C", HUMIDITY: "%", CO2: "ppm", VIBRATION: "m/s²",
    PRESSURE: "hPa", LIGHT: "lx", NOISE: "dB", ENERGY: "kWh", OCCUPANCY: "people"
};

const defaultChannelsBySensorType = {
    TEMPERATURE: "temperature", HUMIDITY: "humidity", CO2: "co2", SMOKE: "smoke",
    MOTION: "motion_rms", VIBRATION: "vibration_rms", DOOR: "door", PRESSURE: "pressure",
    LIGHT: "light", NOISE: "noise", ENERGY: "energy", OCCUPANCY: "occupancy"
};

function defaultChannelForSensor(sensor) {
    if (defaultChannelsBySensorType[sensor.type]) return defaultChannelsBySensorType[sensor.type];
    return (sensor.name || "sensor").trim().toLowerCase().replace(/[^a-z0-9]+/g, "_").replace(/^_+|_+$/g, "") || "sensor";
}

function updateNewSensorDefaultUnit() {
    const type = byId("new-sensor-type");
    const unit = byId("new-sensor-unit");
    const channel = byId("new-sensor-channel");
    const previousDefault = defaultUnitsBySensorType[type.dataset.previousType] || "";
    if (!unit.value.trim() || unit.value === previousDefault) unit.value = defaultUnitsBySensorType[type.value] || "";
    const previousChannel = defaultChannelsBySensorType[type.dataset.previousType] || "";
    if (!channel.value.trim() || channel.value === previousChannel) channel.value = defaultChannelsBySensorType[type.value] || "";
    type.dataset.previousType = type.value;
}

function updateAssignedSensorChannel() {
    const sensor = deviceState.sensors.find(item => item.id === Number(byId("channel-sensor").value));
    byId("channel-key").value = sensor ? defaultChannelForSensor(sensor) : "";
}

function showDeviceMessage(text, error = false) {
    const element = byId("device-message");
    element.textContent = text;
    element.classList.toggle("message-error", error);
    element.classList.remove("hidden");
}

function hideDeviceMessage() { byId("device-message").classList.add("hidden"); }
function formatType(value) { return value.toLowerCase().split("_").map(word => word[0].toUpperCase() + word.slice(1)).join(" "); }
function formatDate(value) { return value ? new Date(value).toLocaleString() : "Never"; }
function organizationName(id) { return deviceState.organizations.find(item => item.id === id)?.name || `Organization ${id}`; }

async function initializeDevices() {
    try {
        const auth = await labMonitorAuthReady;
        if (!auth.has("users.manage")) { window.location.href = "/"; return; }
        [deviceState.organizations, deviceState.labs, deviceState.rooms, deviceState.sensors] = await Promise.all([
            apiRequest("/api/organizations"), apiRequest("/api/labs"), apiRequest("/api/rooms"), apiRequest("/api/sensors")
        ]);
        for (const organization of deviceState.organizations) {
            byId("device-organization").append(new Option(organization.name, organization.id));
            byId("device-filter-organization").append(new Option(organization.name, organization.id));
        }
        populateDeviceLabs();
        await loadDevices();
    } catch (error) {
        byId("devices-loading").classList.add("hidden");
        showDeviceMessage(error.message, true);
    }
}

async function loadDevices() {
    const organizationId = byId("device-filter-organization").value;
    const url = organizationId ? `/api/devices?organizationId=${organizationId}` : "/api/devices";
    deviceState.devices = await apiRequest(url);
    renderDevices();
}

function renderDevices() {
    const status = byId("device-filter-status").value;
    const devices = status ? deviceState.devices.filter(device => device.status === status) : deviceState.devices;
    const rows = byId("device-rows"); rows.replaceChildren();
    for (const device of devices) {
        const row = document.createElement("tr");
        row.append(cell(device.name), cell(device.organizationName), cell(`${device.labName} / ${device.roomName}`), cell(formatType(device.type)), cell(device.status), cell(formatDate(device.lastSeenAt)));
        const action = document.createElement("td"); action.append(button("Manage", "button button-secondary button-small", () => openDevice(device))); row.append(action); rows.append(row);
    }
    byId("devices-loading").classList.add("hidden");
    byId("devices-empty").classList.toggle("hidden", devices.length !== 0);
    byId("devices-table").classList.toggle("hidden", devices.length === 0);
}

function cell(value) { const result = document.createElement("td"); result.textContent = value; return result; }
function button(label, className, handler) { const result = document.createElement("button"); result.type = "button"; result.className = className; result.textContent = label; result.addEventListener("click", handler); return result; }

async function openDevice(device) {
    deviceState.selected = device;
    byId("credential-token-panel").classList.add("hidden");
    await refreshSelectedDevice();
    byId("device-details").classList.remove("hidden");
    byId("device-details").scrollIntoView({behavior: "smooth", block: "start"});
}

async function refreshSelectedDevice() {
    const id = deviceState.selected.id;
    [deviceState.selected, deviceState.channels, deviceState.credentials] = await Promise.all([
        apiRequest(`/api/devices/${id}`), apiRequest(`/api/devices/${id}/channels`), apiRequest(`/api/devices/${id}/credentials`)
    ]);
    const device = deviceState.selected;
    byId("device-details-title").textContent = device.name;
    byId("device-summary").textContent = `${device.organizationName} · ${device.labName} / ${device.roomName} · ${formatType(device.type)} · ${device.status} · Last seen: ${formatDate(device.lastSeenAt)}`;
    byId("toggle-device-status").textContent = device.status === "ACTIVE" ? "Disable device" : "Activate device";
    byId("toggle-device-status").className = `button button-small ${device.status === "ACTIVE" ? "button-danger" : "button-primary"}`;
    const activeCredential = deviceState.credentials.some(item => item.status === "ACTIVE");
    byId("provision-credential").classList.toggle("hidden", activeCredential);
    byId("rotate-credential").classList.toggle("hidden", !activeCredential);
    renderChannels(); renderCredentials(); renderSensorOptions();
}

function renderSensorOptions() {
    const select = byId("channel-sensor"); select.replaceChildren();
    byId("channel-key").value = "";
    const roomSensors = deviceState.sensors.filter(sensor => sensor.roomId === deviceState.selected.roomId);
    const available = roomSensors.filter(sensor => sensor.deviceId == null);
    select.append(new Option(available.length ? "Select sensor" : "No unassigned sensors in this room", ""));
    for (const sensor of roomSensors) {
        const suffix = sensor.deviceId == null ? "" : sensor.deviceId === deviceState.selected.id
            ? " — already assigned here" : " — assigned to another device";
        const option = new Option(`${sensor.name} (#${sensor.id})${suffix}`, sensor.id);
        option.disabled = sensor.deviceId != null;
        select.append(option);
    }
    select.disabled = available.length === 0;
}

function renderChannels() {
    const rows = byId("channel-rows"); rows.replaceChildren();
    for (const channel of deviceState.channels) {
        const row = document.createElement("tr"); row.append(cell(channel.channelKey), cell(channel.sensorName), cell(channel.roomName));
        const action = document.createElement("td");
        if (deviceState.selected.type === "DATA_CLIENT") {
            const client = document.createElement("a"); client.className = "button button-secondary button-small";
            client.href = `/motion-client.html?channel=${encodeURIComponent(channel.channelKey)}`; client.textContent = "Open motion client";
            action.append(client);
        }
        action.append(button("Unassign", "button button-danger button-small", () => unassignChannel(channel))); row.append(action); rows.append(row);
    }
    byId("channels-empty").classList.toggle("hidden", deviceState.channels.length !== 0);
    byId("channels-table").classList.toggle("hidden", deviceState.channels.length === 0);
}

function renderCredentials() {
    const rows = byId("credential-rows"); rows.replaceChildren();
    for (const credential of deviceState.credentials) {
        const row = document.createElement("tr"); row.append(cell(credential.id), cell(credential.status), cell(formatDate(credential.issuedAt)), cell(formatDate(credential.lastUsedAt)));
        const action = document.createElement("td");
        if (credential.status === "ACTIVE") action.append(button("Revoke", "button button-danger button-small", () => revokeCredential(credential)));
        else action.textContent = "—";
        row.append(action); rows.append(row);
    }
    byId("credentials-empty").classList.toggle("hidden", deviceState.credentials.length !== 0);
    byId("credentials-table").classList.toggle("hidden", deviceState.credentials.length === 0);
}

async function createDevice(event) {
    event.preventDefault();
    try {
        const created = await apiRequest("/api/devices", {method: "POST", body: JSON.stringify({name: byId("device-name").value.trim(), roomId: Number(byId("device-room").value), type: byId("device-type").value})});
        closeDeviceForm(); await loadDevices(); showDeviceMessage("Device created."); await openDevice(created);
    } catch (error) { const target = byId("device-form-error"); target.textContent = error.message; target.classList.remove("hidden"); }
}

function populateDeviceLabs() {
    const organizationId = Number(byId("device-organization").value);
    const select = byId("device-lab"); select.replaceChildren(new Option("Select laboratory", ""));
    for (const lab of deviceState.labs.filter(item => item.organizationId === organizationId)) select.append(new Option(lab.name, lab.id));
    populateDeviceRooms();
}

function populateDeviceRooms() {
    const labId = Number(byId("device-lab").value);
    const select = byId("device-room"); select.replaceChildren(new Option("Select room", ""));
    for (const room of deviceState.rooms.filter(item => item.labId === labId)) select.append(new Option(room.name, room.id));
}

async function createSensorChannel(event) {
    event.preventDefault();
    const numberOrNull = id => byId(id).value === "" ? null : Number(byId(id).value);
    try {
        await apiRequest(`/api/devices/${deviceState.selected.id}/sensor-channels`, {
            method: "POST",
            body: JSON.stringify({
                name: byId("new-sensor-name").value.trim(), type: byId("new-sensor-type").value,
                unit: byId("new-sensor-unit").value.trim() || null,
                channelKey: byId("new-sensor-channel").value.trim(),
                minSafeValue: numberOrNull("new-sensor-min"), maxSafeValue: numberOrNull("new-sensor-max")
            })
        });
        deviceState.sensors = await apiRequest("/api/sensors");
        byId("sensor-channel-form").reset(); byId("sensor-channel-form").classList.add("hidden");
        await refreshSelectedDevice(); showDeviceMessage("Sensor channel created and assigned.");
    } catch (error) { showDeviceMessage(error.message, true); }
}

async function assignChannel(event) {
    event.preventDefault();
    try {
        await apiRequest(`/api/devices/${deviceState.selected.id}/sensors/${byId("channel-sensor").value}`, {method: "PUT", body: JSON.stringify({channelKey: byId("channel-key").value.trim()})});
        deviceState.sensors = await apiRequest("/api/sensors"); byId("channel-form").reset(); await refreshSelectedDevice(); showDeviceMessage("Channel assigned.");
    } catch (error) { showDeviceMessage(error.message, true); }
}

async function unassignChannel(channel) {
    if (!confirm(`Unassign channel "${channel.channelKey}"?`)) return;
    try { await apiRequest(`/api/devices/${deviceState.selected.id}/sensors/${channel.sensorId}`, {method: "DELETE"}); deviceState.sensors = await apiRequest("/api/sensors"); await refreshSelectedDevice(); showDeviceMessage("Channel unassigned."); }
    catch (error) { showDeviceMessage(error.message, true); }
}

async function updateDeviceStatus() {
    const status = deviceState.selected.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    try { await apiRequest(`/api/devices/${deviceState.selected.id}/status`, {method: "PATCH", body: JSON.stringify({status})}); await refreshSelectedDevice(); await loadDevices(); showDeviceMessage(status === "ACTIVE" ? "Device activated." : "Device disabled."); }
    catch (error) { showDeviceMessage(error.message, true); }
}

async function issueCredential(action) {
    if (action === "rotate" && !confirm("Rotate the credential? The current token will stop working immediately.")) return;
    try {
        const result = await apiRequest(`/api/devices/${deviceState.selected.id}/credentials/${action}`, {method: "POST"});
        await refreshSelectedDevice(); byId("credential-token").textContent = result.token; byId("credential-token-panel").classList.remove("hidden");
    } catch (error) { showDeviceMessage(error.message, true); }
}

async function revokeCredential(credential) {
    if (!confirm("Revoke this credential? The device will no longer be able to submit readings.")) return;
    try { await apiRequest(`/api/devices/${deviceState.selected.id}/credentials/${credential.id}/revoke`, {method: "POST"}); await refreshSelectedDevice(); showDeviceMessage("Credential revoked."); }
    catch (error) { showDeviceMessage(error.message, true); }
}

function closeDeviceForm() { byId("device-form-panel").classList.add("hidden"); byId("device-form").reset(); byId("device-form-error").classList.add("hidden"); }
byId("show-device-form").addEventListener("click", () => { hideDeviceMessage(); byId("device-form-panel").classList.remove("hidden"); byId("device-name").focus(); });
byId("close-device-form").addEventListener("click", closeDeviceForm); byId("cancel-device-form").addEventListener("click", closeDeviceForm);
byId("device-form").addEventListener("submit", createDevice); byId("channel-form").addEventListener("submit", assignChannel);
byId("channel-sensor").addEventListener("change", updateAssignedSensorChannel);
byId("device-organization").addEventListener("change", populateDeviceLabs); byId("device-lab").addEventListener("change", populateDeviceRooms);
byId("show-sensor-channel-form").addEventListener("click", () => {
    byId("sensor-channel-form").classList.remove("hidden");
    updateNewSensorDefaultUnit();
});
byId("cancel-sensor-channel-form").addEventListener("click", () => byId("sensor-channel-form").classList.add("hidden"));
byId("new-sensor-type").addEventListener("change", updateNewSensorDefaultUnit);
byId("sensor-channel-form").addEventListener("submit", createSensorChannel);
byId("close-device-details").addEventListener("click", () => byId("device-details").classList.add("hidden"));
byId("toggle-device-status").addEventListener("click", updateDeviceStatus);
byId("provision-credential").addEventListener("click", () => issueCredential("provision")); byId("rotate-credential").addEventListener("click", () => issueCredential("rotate"));
byId("copy-credential-token").addEventListener("click", async () => { await navigator.clipboard.writeText(byId("credential-token").textContent); showDeviceMessage("Credential copied."); });
byId("device-filter-organization").addEventListener("change", loadDevices); byId("device-filter-status").addEventListener("change", renderDevices);
initializeDevices();

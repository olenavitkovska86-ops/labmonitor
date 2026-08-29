const resourceTree = document.querySelector("#resource-tree");
const monitorLoading = document.querySelector("#monitor-loading");
const monitorWorkspace = document.querySelector("#monitor-workspace");
const monitorMessage = document.querySelector("#monitor-message");
let monitorAuth;

async function loadResourceTree() {
    monitorLoading.classList.remove("hidden");
    monitorMessage.classList.add("hidden");
    resourceTree.replaceChildren();
    try {
        monitorAuth ||= await labMonitorAuthReady;
        const [organizations, labs, rooms, sensors] = await Promise.all([
            apiRequest("/api/organizations"),
            apiRequest("/api/labs"),
            apiRequest("/api/rooms"),
            apiRequest("/api/sensors")
        ]);
        const requestedId = new URLSearchParams(window.location.search).get("organizationId");
        const visibleOrganizations = requestedId
            ? organizations.filter(item => String(item.id) === requestedId)
            : organizations;
        const trees = visibleOrganizations.map(organization => loadOrganization(organization, labs, rooms, sensors));
        trees.forEach(tree => resourceTree.append(renderOrganization(tree)));
        if (!trees.length) resourceTree.innerHTML = '<p class="dashboard-empty">No accessible resources.</p>';
    } catch (error) {
        monitorMessage.textContent = error.message;
        monitorMessage.classList.add("message-error");
        monitorMessage.classList.remove("hidden");
    } finally {
        monitorLoading.classList.add("hidden");
    }
}

function loadOrganization(organization, allLabs, allRooms, allSensors) {
    const labs = allLabs.filter(lab => lab.organizationId === organization.id);
    return {organization, labs: labs.map(lab => ({
        lab,
        rooms: allRooms.filter(room => room.labId === lab.id).map(room => ({
            room,
            sensors: allSensors.filter(sensor => sensor.roomId === room.id)
        }))
    }))};
}

function renderOrganization(tree) {
    const group = document.createElement("div"); group.className = "resource-tree-group";
    group.append(createResourceButton("organization", tree.organization.name, `${tree.labs.length} labs`, () => showOrganization(tree)));
    const labs = document.createElement("div"); labs.className = "resource-tree-children";
    tree.labs.forEach(labTree => {
        labs.append(createResourceButton("lab", labTree.lab.name, `${labTree.rooms.length} rooms`, () => showLab(tree.organization, labTree)));
        const rooms = document.createElement("div"); rooms.className = "resource-tree-children";
        labTree.rooms.forEach(roomTree => {
            rooms.append(createResourceButton("room", roomTree.room.name, `${roomTree.sensors.length} sensors`, () => showRoom(tree.organization, labTree.lab, roomTree)));
            const sensors = document.createElement("div"); sensors.className = "resource-tree-children resource-tree-sensors";
            roomTree.sensors.forEach(sensor => sensors.append(createResourceButton("sensor", sensor.name, sensor.status, () => showSensor(tree.organization, labTree.lab, roomTree.room, sensor))));
            rooms.append(sensors);
        });
        labs.append(rooms);
    });
    group.append(labs); return group;
}

function createResourceButton(type, name, meta, action) {
    const button = document.createElement("button"); button.className = `resource-tree-item resource-tree-${type}`; button.type = "button";
    button.innerHTML = `<span class="resource-tree-symbol" aria-hidden="true">${type === "organization" ? "◇" : type === "lab" ? "⌂" : type === "room" ? "□" : "·"}</span><span class="resource-tree-identity"><strong></strong><small></small></span>`;
    button.querySelector("strong").textContent = name; button.querySelector("small").textContent = meta;
    button.addEventListener("click", () => { document.querySelectorAll(".resource-tree-item-active").forEach(item => item.classList.remove("resource-tree-item-active")); button.classList.add("resource-tree-item-active"); action(); });
    return button;
}

function showOrganization(tree) { const actions = [["Open organization", `/organizations.html`]]; if (monitorAuth.has("labs.manage")) actions.push(["Manage labs", `/labs.html?organizationId=${tree.organization.id}`]); renderWorkspace("Organization", tree.organization.name, tree.organization.description, [["Labs", tree.labs.length]], actions); }
function showLab(organization, tree) { const actions = [["Open lab", `/labs.html?organizationId=${organization.id}`]]; if (monitorAuth.has("rooms.manage")) actions.push(["Manage rooms", `/rooms.html?labId=${tree.lab.id}`]); renderWorkspace(organization.name, tree.lab.name, tree.lab.description || tree.lab.location, [["Rooms", tree.rooms.length], ["Location", tree.lab.location || "—"]], actions); }
function showRoom(organization, lab, tree) { renderWorkspace(`${organization.name} / ${lab.name}`, tree.room.name, tree.room.type?.replaceAll("_", " "), [["Sensors", tree.sensors.length], ["Floor", tree.room.floor ?? "—"]], [["Open room", `/rooms.html?labId=${lab.id}`], ["View sensors", `/sensors.html?roomId=${tree.room.id}`], ["Review alerts", `/alerts.html?roomId=${tree.room.id}`]]); }
function showSensor(organization, lab, room, sensor) { const actions = [["Readings", `/sensor-readings.html?sensorId=${sensor.id}`]]; if (monitorAuth.hasForOrganization("sensors.settings.update", organization.id)) actions.push(["Sensor settings", `/sensors.html?roomId=${room.id}`]); renderWorkspace(`${organization.name} / ${lab.name} / ${room.name}`, sensor.name, `${sensor.type?.replaceAll("_", " ")} · ${sensor.status}`, [["Safe minimum", sensor.minSafeValue ?? "—"], ["Safe maximum", sensor.maxSafeValue ?? "—"], ["Unit", sensor.unit || "—"]], actions); }

function renderWorkspace(context, title, description, facts, actions) {
    monitorWorkspace.replaceChildren();
    const header = document.createElement("div"); header.className = "monitor-workspace-header";
    const copy = document.createElement("div"); copy.innerHTML = '<p class="panel-kicker"></p><h2></h2><p class="section-description"></p>';
    copy.querySelector(".panel-kicker").textContent = context; copy.querySelector("h2").textContent = title; copy.querySelector(".section-description").textContent = description || "No description provided.";
    const links = document.createElement("div"); links.className = "monitor-workspace-actions";
    actions.forEach(([label, href], index) => { const link = document.createElement("a"); link.className = index ? "button button-secondary" : "button button-primary"; link.href = href; link.textContent = label; links.append(link); });
    header.append(copy, links);
    const factGrid = document.createElement("div"); factGrid.className = "monitor-fact-grid";
    facts.forEach(([label, value]) => { const fact = document.createElement("div"); const name = document.createElement("span"); name.textContent = label; const result = document.createElement("strong"); result.textContent = value; fact.append(name, result); factGrid.append(fact); });
    monitorWorkspace.append(header, factGrid);
}

document.querySelector("#refresh-tree").addEventListener("click", loadResourceTree);
renderBreadcrumbs([{label: "Overview", href: "/analytics.html"}, {label: "Monitor"}]);
loadResourceTree();

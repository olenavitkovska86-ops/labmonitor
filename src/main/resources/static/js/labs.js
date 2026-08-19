const labsApiUrl = "/api/labs";
const organizationsApiUrl = "/api/organizations";

const rows = document.querySelector("#lab-rows");
const tableWrapper = document.querySelector("#table-wrapper");
const loadingState = document.querySelector("#loading-state");
const emptyState = document.querySelector("#empty-state");
const pageMessage = document.querySelector("#page-message");
const formPanel = document.querySelector("#lab-form-panel");
const form = document.querySelector("#lab-form");
const formTitle = document.querySelector("#form-title");
const formError = document.querySelector("#form-error");
const idInput = document.querySelector("#lab-id");
const organizationInput = document.querySelector("#lab-organization");
const nameInput = document.querySelector("#lab-name");
const locationInput = document.querySelector("#lab-location");
const descriptionInput = document.querySelector("#lab-description");
const searchForm = document.querySelector("#search-form");
const searchInput = document.querySelector("#search-input");
const organizationFilter = document.querySelector("#organization-filter");

let organizationsById = new Map();
let searchTimer;

async function request(url, options = {}) {
    const token = localStorage.getItem("token");
    const headers = {"Content-Type": "application/json"};
    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }

    const response = await fetch(url, {
        ...options,
        headers
    });

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
        const organizations = await request(organizationsApiUrl);
        organizationsById = new Map(organizations.map(organization => [organization.id, organization]));
        renderOrganizationOptions(organizations);
        applyOrganizationFromUrl();
        renderLabBreadcrumbs();
        await loadLabs();
    } catch (error) {
        loadingState.classList.add("hidden");
        showMessage(pageMessage, error.message, true);
    }
}

function applyOrganizationFromUrl() {
    const organizationId = new URLSearchParams(window.location.search).get("organizationId");

    if (organizationId && organizationsById.has(Number(organizationId))) {
        organizationFilter.value = organizationId;
        organizationInput.value = organizationId;
    }
}

function renderLabBreadcrumbs() {
    const organization = organizationsById.get(Number(organizationFilter.value));
    const items = [
        {label: "Home", href: "/"},
        {label: "Organizations", href: "/organizations.html"}
    ];

    if (organization) {
        items.push({label: organization.name, href: `/labs.html?organizationId=${organization.id}`});
    }
    items.push({label: "Labs"});
    renderBreadcrumbs(items);
}

function renderOrganizationOptions(organizations) {
    for (const organization of organizations) {
        organizationInput.append(createOption(organization.id, organization.name));
        organizationFilter.append(createOption(organization.id, organization.name));
    }
}

function createOption(value, label) {
    const option = document.createElement("option");
    option.value = value;
    option.textContent = label;
    return option;
}

async function loadLabs() {
    loadingState.classList.remove("hidden");
    emptyState.classList.add("hidden");
    tableWrapper.classList.add("hidden");
    hideMessage(pageMessage);

    try {
        const parameters = new URLSearchParams();
        const search = searchInput.value.trim();
        const organizationId = organizationFilter.value;

        if (search) {
            parameters.set("search", search);
        }
        if (organizationId) {
            parameters.set("organizationId", organizationId);
        }

        const query = parameters.toString();
        const labs = await request(query ? `${labsApiUrl}?${query}` : labsApiUrl);
        renderLabs(labs);
    } catch (error) {
        showMessage(pageMessage, error.message, true);
    } finally {
        loadingState.classList.add("hidden");
    }
}

function renderLabs(labs) {
    rows.replaceChildren();

    if (labs.length === 0) {
        emptyState.classList.remove("hidden");
        return;
    }

    for (const lab of labs) {
        const organization = organizationsById.get(lab.organizationId);
        const row = document.createElement("tr");
        row.append(
            createCell(lab.id),
            createLabLinkCell(lab),
            createCell(organization?.name || `Organization ${lab.organizationId}`),
            createCell(lab.location || "—"),
            createStatusCell(lab.active),
            createActionsCell(lab)
        );
        rows.append(row);
    }

    tableWrapper.classList.remove("hidden");
}

function createLabLinkCell(lab) {
    const cell = document.createElement("td");
    const link = document.createElement("a");
    link.className = "table-link";
    link.href = `/rooms.html?labId=${lab.id}`;
    link.textContent = lab.name;
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

function createActionsCell(lab) {
    const cell = document.createElement("td");
    const actions = document.createElement("div");
    actions.className = "row-actions";

    const editButton = createButton("Edit", "button button-secondary button-small");
    editButton.addEventListener("click", () => openEditForm(lab));

    const lifecycleButton = createButton(
        lab.active ? "Deactivate" : "Activate",
        `button ${lab.active ? "button-danger" : "button-primary"} button-small`
    );
    lifecycleButton.addEventListener("click", () => changeLabActivity(lab));

    actions.append(editButton, lifecycleButton);
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
    organizationInput.disabled = false;
    organizationInput.value = organizationFilter.value;
    formTitle.textContent = "New lab";
    hideMessage(formError);
    formPanel.classList.remove("hidden");
    organizationInput.focus();
}

function openEditForm(lab) {
    idInput.value = lab.id;
    organizationInput.value = lab.organizationId;
    organizationInput.disabled = true;
    nameInput.value = lab.name;
    locationInput.value = lab.location || "";
    descriptionInput.value = lab.description || "";
    formTitle.textContent = "Edit lab";
    hideMessage(formError);
    formPanel.classList.remove("hidden");
    formPanel.scrollIntoView({behavior: "smooth", block: "start"});
}

function closeForm() {
    formPanel.classList.add("hidden");
    form.reset();
    organizationInput.disabled = false;
    hideMessage(formError);
}

async function saveLab(event) {
    event.preventDefault();
    hideMessage(formError);

    const id = idInput.value;
    const lab = {
        name: nameInput.value.trim(),
        location: locationInput.value.trim() || null,
        description: descriptionInput.value.trim() || null
    };

    if (!id) {
        lab.organizationId = Number(organizationInput.value);
    }

    try {
        await request(id ? `${labsApiUrl}/${id}` : labsApiUrl, {
            method: id ? "PUT" : "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify(lab)
        });

        closeForm();
        await loadLabs();
        showMessage(pageMessage, id ? "Lab updated." : "Lab created.");
    } catch (error) {
        showMessage(formError, error.message, true);
    }
}

async function changeLabActivity(lab) {
    const action = lab.active ? "deactivate" : "activate";
    const confirmed = window.confirm(`${lab.active ? "Deactivate" : "Activate"} lab "${lab.name}"?`);
    if (!confirmed) {
        return;
    }

    try {
        await request(`${labsApiUrl}/${lab.id}/${action}`, {method: "POST"});
        await loadLabs();
        showMessage(pageMessage, lab.active ? "Lab deactivated." : "Lab activated.");
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
    loadLabs();
});
document.querySelector("#clear-search").addEventListener("click", () => {
    searchInput.value = "";
    organizationFilter.value = "";
    renderLabBreadcrumbs();
    loadLabs();
});
searchInput.addEventListener("input", () => {
    window.clearTimeout(searchTimer);
    searchTimer = window.setTimeout(loadLabs, 300);
});
organizationFilter.addEventListener("change", () => {
    renderLabBreadcrumbs();
    loadLabs();
});
form.addEventListener("submit", saveLab);

initializePage();

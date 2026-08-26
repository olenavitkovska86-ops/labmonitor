const apiUrl = "/api/organizations";

const rows = document.querySelector("#organization-rows");
const tableWrapper = document.querySelector("#table-wrapper");
const loadingState = document.querySelector("#loading-state");
const emptyState = document.querySelector("#empty-state");
const pageMessage = document.querySelector("#page-message");
const formPanel = document.querySelector("#organization-form-panel");
const form = document.querySelector("#organization-form");
const formTitle = document.querySelector("#form-title");
const formError = document.querySelector("#form-error");
const idInput = document.querySelector("#organization-id");
const nameInput = document.querySelector("#organization-name");
const descriptionInput = document.querySelector("#organization-description");
const searchForm = document.querySelector("#search-form");
const searchInput = document.querySelector("#search-input");
let searchTimer;

async function request(url, options = {}) {
    return apiRequest(url, options);
}

async function loadOrganizations() {
    loadingState.classList.remove("hidden");
    emptyState.classList.add("hidden");
    tableWrapper.classList.add("hidden");
    hideMessage(pageMessage);

    try {
        const search = searchInput.value.trim();
        const url = search ? `${apiUrl}?search=${encodeURIComponent(search)}` : apiUrl;
        const organizations = await request(url);

        renderOrganizations(organizations);
    } catch (error) {
        showMessage(pageMessage, error.message, true);
    } finally {
        loadingState.classList.add("hidden");
    }
}

function renderOrganizations(organizations) {
    rows.replaceChildren();

    if (organizations.length === 0) {
        emptyState.classList.remove("hidden");
        return;
    }

    for (const organization of organizations) {
        const row = document.createElement("tr");
        row.append(
            createCell(organization.id),
            createOrganizationLinkCell(organization),
            createCell(organization.description || "—"),
            createActionsCell(organization)
        );
        rows.append(row);
    }

    tableWrapper.classList.remove("hidden");
}

function createOrganizationLinkCell(organization) {
    const cell = document.createElement("td");
    const link = document.createElement("a");
    link.className = "table-link";
    link.href = `/labs.html?organizationId=${organization.id}`;
    link.textContent = organization.name;
    cell.append(link);
    return cell;
}

function createCell(value) {
    const cell = document.createElement("td");
    cell.textContent = value;
    return cell;
}

function createActionsCell(organization) {
    const cell = document.createElement("td");
    const actions = document.createElement("div");
    actions.className = "row-actions";

    const editButton = createButton("Edit", "button button-secondary button-small");
    editButton.addEventListener("click", () => openEditForm(organization));

    const analyticsLink = document.createElement("a");
    analyticsLink.className = "button button-secondary button-small";
    analyticsLink.href = `/analytics.html?organizationId=${organization.id}`;
    analyticsLink.textContent = "Overview";

    const deleteButton = createButton("Delete", "button button-danger button-small");
    deleteButton.addEventListener("click", () => deleteOrganization(organization));

    actions.append(analyticsLink);
    if (window.labMonitorAuth?.has("organizations.manage")) actions.append(editButton, deleteButton);
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
    formTitle.textContent = "New organization";
    hideMessage(formError);
    formPanel.classList.remove("hidden");
    nameInput.focus();
}

function openEditForm(organization) {
    idInput.value = organization.id;
    nameInput.value = organization.name;
    descriptionInput.value = organization.description || "";
    formTitle.textContent = "Edit organization";
    hideMessage(formError);
    formPanel.classList.remove("hidden");
    formPanel.scrollIntoView({behavior: "smooth", block: "start"});
}

function closeForm() {
    formPanel.classList.add("hidden");
    form.reset();
    hideMessage(formError);
}

async function saveOrganization(event) {
    event.preventDefault();
    hideMessage(formError);

    const id = idInput.value;
    const body = JSON.stringify({
        name: nameInput.value.trim(),
        description: descriptionInput.value.trim() || null
    });

    try {
        await request(id ? `${apiUrl}/${id}` : apiUrl, {
            method: id ? "PUT" : "POST",
            headers: {"Content-Type": "application/json"},
            body
        });

        closeForm();
        await loadOrganizations();
        showMessage(pageMessage, id ? "Organization updated." : "Organization created.");
    } catch (error) {
        showMessage(formError, error.message, true);
    }
}

async function deleteOrganization(organization) {
    const confirmed = window.confirm(`Delete organization "${organization.name}"?`);
    if (!confirmed) {
        return;
    }

    try {
        await request(`${apiUrl}/${organization.id}`, {method: "DELETE"});
        await loadOrganizations();
        showMessage(pageMessage, "Organization deleted.");
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
    loadOrganizations();
});
document.querySelector("#clear-search").addEventListener("click", () => {
    searchInput.value = "";
    loadOrganizations();
});
searchInput.addEventListener("input", () => {
    window.clearTimeout(searchTimer);
    searchTimer = window.setTimeout(() => {
        loadOrganizations();
    }, 300);
});
form.addEventListener("submit", saveOrganization);

labMonitorAuthReady.then(loadOrganizations).catch(error => {
    loadingState.classList.add("hidden");
    showMessage(pageMessage, error.message, true);
});

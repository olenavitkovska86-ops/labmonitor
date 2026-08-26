const userRows = document.querySelector("#user-rows");
const userFormPanel = document.querySelector("#user-form-panel");
const userForm = document.querySelector("#user-form");
const roleInput = document.querySelector("#user-role");
const organizationInput = document.querySelector("#user-organization");
const adminMessage = document.querySelector("#admin-message");
const membershipFormPanel = document.querySelector("#membership-form-panel");
const membershipForm = document.querySelector("#membership-form");
const membershipOrganization = document.querySelector("#membership-organization");
const membershipScope = document.querySelector("#membership-scope");
let administrationUsers = [];
let administrationOrganizations = [];
let administrationLabs = [];
let administrationRooms = [];
let currentAdministratorId;

async function initializeAdministration() {
    try {
        const auth = await labMonitorAuthReady;
        if (!auth.has("users.manage")) {
            window.location.href = "/";
            return;
        }
        const [users, organizations, labs, rooms] = await Promise.all([
            apiRequest("/api/users"), apiRequest("/api/organizations"),
            apiRequest("/api/labs"), apiRequest("/api/rooms")
        ]);
        administrationUsers = users;
        administrationOrganizations = organizations;
        administrationLabs = labs;
        administrationRooms = rooms;
        currentAdministratorId = auth.user.id;
        organizations.forEach(organization => organizationInput.append(new Option(organization.name, organization.id)));
        organizations.forEach(organization => membershipOrganization.append(new Option(organization.name, organization.id)));
        renderUsers(users, auth.user.id);
    } catch (error) {
        showAdminMessage(error.message, true);
    }
}

function renderUsers(users, currentUserId) {
    userRows.replaceChildren();
    users.forEach(user => {
        const row = document.createElement("tr");
        row.append(cell(`${user.firstName} ${user.lastName}\n${user.email}`));
        row.append(cell(user.globalRole));
        row.append(createMembershipCell(user));
        row.append(cell(user.status));
        const actionCell = document.createElement("td");
        const button = document.createElement("button");
        button.type = "button";
        button.className = `button button-small ${user.status === "ACTIVE" ? "button-danger" : "button-primary"}`;
        button.textContent = user.status === "ACTIVE" ? "Disable" : "Enable";
        button.disabled = user.id === currentUserId;
        button.addEventListener("click", () => changeStatus(user, currentUserId));
        const addAccess = document.createElement("button");
        addAccess.type = "button";
        addAccess.className = "button button-secondary button-small";
        addAccess.textContent = "Add access";
        addAccess.disabled = user.globalRole === "SUPER_ADMIN"
            || user.memberships.length >= administrationOrganizations.length;
        addAccess.addEventListener("click", () => openMembershipForm(user));
        actionCell.append(button, addAccess);
        row.append(actionCell);
        userRows.append(row);
    });
    document.querySelector("#users-loading").classList.add("hidden");
    document.querySelector("#users-table").classList.remove("hidden");
}

function createMembershipCell(user) {
    const result = document.createElement("td");
    if (!user.memberships.length) {
        result.textContent = "—";
        return result;
    }
    user.memberships.forEach(membership => {
        const item = document.createElement("div");
        item.className = "membership-row";
        const label = document.createElement("span");
        label.textContent = `${membership.organizationName}: ${formatRole(membership.role)} (${formatScope(membership)})`;
        const edit = smallButton("Edit", "button-secondary", () => openMembershipForm(user, membership));
        const remove = smallButton("Remove", "button-danger", () => deleteMembership(membership));
        item.append(label, edit, remove);
        result.append(item);
    });
    return result;
}

function smallButton(label, style, handler) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = `button button-small ${style}`;
    button.textContent = label;
    button.addEventListener("click", handler);
    return button;
}

function openMembershipForm(user, membership = null) {
    membershipForm.reset();
    document.querySelector("#membership-id").value = membership?.id || "";
    document.querySelector("#membership-user-id").value = user.id;
    document.querySelector("#membership-form-title").textContent = membership
        ? `Edit access for ${user.firstName} ${user.lastName}`
        : `Add access for ${user.firstName} ${user.lastName}`;
    membershipOrganization.disabled = Boolean(membership);
    membershipOrganization.value = membership?.organizationId || firstAvailableOrganization(user);
    document.querySelector("#membership-role").value = membership?.role || "LIMITED_EMPLOYEE";
    membershipScope.value = membership?.scopeType || "ORGANIZATION";
    renderScopeOptions(membership?.labIds || [], membership?.roomIds || []);
    updateScopeVisibility();
    document.querySelector("#membership-form-error").classList.add("hidden");
    membershipFormPanel.classList.remove("hidden");
    membershipFormPanel.scrollIntoView({behavior: "smooth", block: "start"});
}

function firstAvailableOrganization(user) {
    const assigned = new Set(user.memberships.map(item => item.organizationId));
    return administrationOrganizations.find(item => !assigned.has(item.id))?.id || "";
}

function renderScopeOptions(selectedLabIds = [], selectedRoomIds = []) {
    const organizationId = Number(membershipOrganization.value);
    renderChecks("#membership-labs", administrationLabs.filter(lab => lab.organizationId === organizationId), selectedLabIds, lab => lab.name);
    renderChecks("#membership-rooms", administrationRooms.filter(room => room.organizationId === organizationId), selectedRoomIds, room => {
        const lab = administrationLabs.find(item => item.id === room.labId);
        return `${room.name} · ${lab?.name || "Unknown lab"}`;
    });
}

function renderChecks(selector, items, selectedIds, label) {
    const container = document.querySelector(selector);
    container.replaceChildren();
    items.forEach(item => {
        const option = document.createElement("label");
        option.className = "scope-option";
        const input = document.createElement("input");
        input.type = "checkbox";
        input.value = item.id;
        input.checked = selectedIds.includes(item.id);
        option.append(input, document.createTextNode(label(item)));
        container.append(option);
    });
    if (!items.length) container.textContent = "No resources in this organization.";
}

function updateScopeVisibility() {
    document.querySelector("#specific-scope-fields").classList.toggle("hidden", membershipScope.value !== "SPECIFIC");
}

membershipForm.addEventListener("submit", async event => {
    event.preventDefault();
    const id = document.querySelector("#membership-id").value;
    const scope = {type: membershipScope.value, labIds: [], roomIds: []};
    if (scope.type === "SPECIFIC") {
        scope.labIds = checkedIds("#membership-labs");
        scope.roomIds = checkedIds("#membership-rooms");
    }
    const payload = {role: document.querySelector("#membership-role").value, scope};
    if (!id) {
        payload.userId = Number(document.querySelector("#membership-user-id").value);
        payload.organizationId = Number(membershipOrganization.value);
    }
    try {
        await apiRequest(id ? `/api/memberships/${id}` : "/api/memberships", {
            method: id ? "PUT" : "POST", body: JSON.stringify(payload)
        });
        closeMembershipForm();
        await refreshUsers();
        showAdminMessage("Organization access saved.");
    } catch (error) {
        const message = document.querySelector("#membership-form-error");
        message.textContent = error.message;
        message.classList.remove("hidden");
    }
});

async function deleteMembership(membership) {
    if (!window.confirm(`Remove access to ${membership.organizationName}?`)) return;
    try {
        await apiRequest(`/api/memberships/${membership.id}`, {method: "DELETE"});
        await refreshUsers();
        showAdminMessage("Organization access removed.");
    } catch (error) { showAdminMessage(error.message, true); }
}

async function refreshUsers() {
    administrationUsers = await apiRequest("/api/users");
    renderUsers(administrationUsers, currentAdministratorId);
}

function checkedIds(selector) {
    return [...document.querySelectorAll(`${selector} input:checked`)].map(input => Number(input.value));
}

function closeMembershipForm() {
    membershipFormPanel.classList.add("hidden");
    membershipOrganization.disabled = false;
}

async function changeStatus(user, currentUserId) {
    try {
        await apiRequest(`/api/users/${user.id}/status`, {method: "PUT", body: JSON.stringify({status: user.status === "ACTIVE" ? "DISABLED" : "ACTIVE"})});
        await refreshUsers();
    } catch (error) { showAdminMessage(error.message, true); }
}

userForm.addEventListener("submit", async event => {
    event.preventDefault();
    try {
        const role = roleInput.value;
        await apiRequest("/api/users", {method: "POST", body: JSON.stringify({
            email: document.querySelector("#user-email").value.trim(),
            password: document.querySelector("#user-password").value,
            firstName: document.querySelector("#user-first-name").value.trim(),
            lastName: document.querySelector("#user-last-name").value.trim(),
            role,
            organization: role === "SUPER_ADMIN" ? null : Number(organizationInput.value)
        })});
        userForm.reset(); userFormPanel.classList.add("hidden");
        await refreshUsers();
        showAdminMessage("User created.");
    } catch (error) { showAdminMessage(error.message, true); }
});

function cell(value) { const result = document.createElement("td"); result.textContent = value; result.className = "pre-line"; return result; }
function formatRole(role) { return role === "LAB_ADMIN" ? "Lab admin" : "Limited employee"; }
function formatScope(item) { return item.scopeType === "ORGANIZATION" ? "all resources" : `${item.labIds.length} labs, ${item.roomIds.length} rooms`; }
function showAdminMessage(message, error = false) { adminMessage.textContent = message; adminMessage.className = `message ${error ? "message-error" : "message-success"}`; }
function closeForm() { userFormPanel.classList.add("hidden"); }
document.querySelector("#show-user-form").addEventListener("click", () => userFormPanel.classList.remove("hidden"));
document.querySelector("#close-user-form").addEventListener("click", closeForm);
document.querySelector("#cancel-user-form").addEventListener("click", closeForm);
roleInput.addEventListener("change", () => document.querySelector("#organization-field").classList.toggle("hidden", roleInput.value === "SUPER_ADMIN"));
membershipOrganization.addEventListener("change", () => renderScopeOptions());
membershipScope.addEventListener("change", updateScopeVisibility);
document.querySelector("#close-membership-form").addEventListener("click", closeMembershipForm);
document.querySelector("#cancel-membership-form").addEventListener("click", closeMembershipForm);
initializeAdministration();

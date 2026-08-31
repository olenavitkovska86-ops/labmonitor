const userRows = document.querySelector("#user-rows");
const userFormPanel = document.querySelector("#user-form-panel");
const userForm = document.querySelector("#user-form");
const roleInput = document.querySelector("#user-role");
const organizationInput = document.querySelector("#user-organization");
const adminMessage = document.querySelector("#admin-message");
const membershipFormPanel = document.querySelector("#membership-form-panel");
const membershipForm = document.querySelector("#membership-form");
const membershipScope = document.querySelector("#membership-scope");
let administrationUsers = [];
let administrationOrganizations = [];
let administrationLabs = [];
let administrationRooms = [];
let currentAdministratorId;
let administrationMode = "SUPER_ADMIN";
let administrationOrganization;
let administrationActorMembership;

async function initializeAdministration(organization) {
    try {
        const auth = await labMonitorAuthReady;
        currentAdministratorId = auth.user.id;
        if (auth.has("users.manage")) {
            administrationMode = "SUPER_ADMIN";
            await initializeSuperAdministration();
            return;
        }
        if (!organization || !auth.hasForOrganization("team.access.manage", organization.id)) {
            window.location.href = "/";
            return;
        }
        administrationMode = "LAB_ADMIN";
        administrationOrganization = organization;
        administrationActorMembership = auth.membership(organization.id);
        configureLabAdminScopeOptions();
        document.querySelector("#show-user-form").classList.add("hidden");
        document.querySelector("#administration-eyebrow").textContent = "Organization administration";
        document.querySelector("#administration-intro").textContent = "Manage limited-employee assignments within your own resources.";
        document.querySelector("#users-heading").textContent = "Organization team";
        document.querySelector("#role-column-heading").textContent = "Role";
        document.querySelector("#access-column-heading").textContent = "Access scope";
        const [members, labs, rooms] = await Promise.all([
            apiRequest(`/api/team-access/organizations/${organization.id}`),
            apiRequest(`/api/labs?organizationId=${organization.id}`), apiRequest("/api/rooms")
        ]);
        administrationOrganizations = [organization];
        administrationLabs = labs;
        administrationRooms = rooms.filter(room => room.organizationId === organization.id);
        administrationUsers = members;
        renderTeamUsers(administrationUsers);
    } catch (error) {
        showAdminMessage(error.message, true);
    }
}

function configureLabAdminScopeOptions() {
    const organizationOption = membershipScope.querySelector('option[value="ORGANIZATION"]');
    const scoped = administrationActorMembership.scopeType !== "ORGANIZATION";
    organizationOption.disabled = scoped;
    document.querySelector("#membership-scope-guidance").classList.toggle("hidden", !scoped);
    if (scoped) membershipScope.value = "SPECIFIC";
}

async function initializeSuperAdministration() {
        const [users, organizations, labs, rooms] = await Promise.all([
            apiRequest("/api/users/managed"), apiRequest("/api/organizations"),
            apiRequest("/api/labs"), apiRequest("/api/rooms")
        ]);
        administrationUsers = users;
        administrationOrganizations = organizations;
        administrationLabs = labs;
        administrationRooms = rooms;
        organizations.forEach(organization => organizationInput.append(new Option(organization.name, organization.id)));
        renderUsers(users, currentAdministratorId);
}

function renderTeamUsers(users) {
    userRows.replaceChildren();
    users.forEach(user => {
        const membership = user.assignments[0];
        const row = document.createElement("tr");
        row.append(cell(`${user.firstName} ${user.lastName}\n${user.email}`));
        row.append(cell("Limited employee"));
        row.append(cell(formatScope(membership)));
        row.append(cell("—"));
        const action = document.createElement("td");
        if (user.allowedActions.includes("ACCESS_EDIT")) {
            action.append(smallButton("Manage", "button-secondary", () => openTeamMemberForm(user, membership)));
        }
        row.append(action); userRows.append(row);
    });
    document.querySelector("#users-loading").classList.add("hidden");
    document.querySelector("#users-table").classList.remove("hidden");
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
        const manage = document.createElement("button");
        manage.type = "button";
        manage.className = "button button-secondary button-small";
        manage.textContent = "Manage";
        manage.addEventListener("click", () => openMembershipForm(user));
        actionCell.append(manage);
        row.append(actionCell);
        userRows.append(row);
    });
    document.querySelector("#users-loading").classList.add("hidden");
    document.querySelector("#users-table").classList.remove("hidden");
}

function createMembershipCell(user) {
    const result = document.createElement("td");
    if (!user.assignments.length) {
        result.textContent = "—";
        return result;
    }
    user.assignments.forEach(membership => {
        const item = document.createElement("div");
        item.className = "membership-row";
        const label = document.createElement("span");
        label.textContent = `${membership.organizationName}: ${formatRole(membership.role)} (${formatScope(membership)})`;
        item.append(label);
        if (user.allowedActions.includes("ACCESS_EDIT")) {
            item.append(smallButton("Edit", "button-secondary", () => openMembershipForm(user, membership)));
        }
        if (user.allowedActions.includes("ACCESS_REMOVE")) {
            item.append(smallButton("Remove", "button-danger", () => deleteMembership(membership)));
        }
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
    document.querySelector("#membership-id").value = membership?.membershipId || "";
    document.querySelector("#membership-user-id").value = user.id;
    document.querySelector("#membership-form-title").textContent = membership
        ? `Edit access for ${user.firstName} ${user.lastName}`
        : `Manage ${user.firstName} ${user.lastName}`;
    renderManagedUserStatus(user);
    const canManageAccess = user.globalRole !== "SUPER_ADMIN";
    document.querySelector("#membership-current-access").classList.toggle("hidden", !canManageAccess);
    document.querySelector("#membership-access-fields").classList.toggle("hidden", !canManageAccess);
    document.querySelector("#specific-scope-fields").classList.toggle("hidden", !canManageAccess || membershipScope.value !== "SPECIFIC");
    document.querySelector("#membership-submit").classList.toggle("hidden", !canManageAccess);
    renderCurrentOrganizations(user, membership);
    renderOrganizationOptions(user, membership);
    membershipScope.value = membership?.scopeType || "ORGANIZATION";
    document.querySelector("#membership-organization-legend").textContent = membership ? "Organization" : "Organizations to add";
    document.querySelector("#membership-submit").textContent = membership ? "Save changes" : "Add selected access";
    renderScopeOptions(membership?.labIds || [], membership?.roomIds || []);
    updateScopeVisibility();
    document.querySelector("#membership-form-error").classList.add("hidden");
    membershipFormPanel.classList.remove("hidden");
    membershipFormPanel.scrollIntoView({behavior: "smooth", block: "start"});
}

function openTeamMemberForm(user, membership) {
    openMembershipForm(user, membership);
    if (administrationActorMembership.scopeType !== "ORGANIZATION") membershipScope.value = "SPECIFIC";
    updateScopeVisibility();
    document.querySelector("#membership-form-title").textContent = `Manage access for ${user.firstName} ${user.lastName}`;
    document.querySelector("#managed-user-status-section").classList.add("hidden");
    document.querySelector("#membership-current-access").classList.add("hidden");
    document.querySelector("#membership-organization-field").classList.add("hidden");
    document.querySelector("#membership-submit").textContent = "Save assignment";
}

function renderManagedUserStatus(user) {
    document.querySelector("#managed-user-status").textContent = user.status === "ACTIVE" ? "Active" : "Disabled";
    const toggle = document.querySelector("#managed-user-status-toggle");
    toggle.textContent = user.status === "ACTIVE" ? "Disable account" : "Enable account";
    toggle.className = `button button-small ${user.status === "ACTIVE" ? "button-danger" : "button-primary"}`;
    toggle.disabled = user.id === currentAdministratorId;
    toggle.onclick = async () => {
        if (!await changeStatus(user)) return;
        const updatedUser = administrationUsers.find(item => item.id === user.id);
        if (updatedUser) openMembershipForm(updatedUser);
    };
}

function renderCurrentOrganizations(user, editedMembership) {
    const container = document.querySelector("#membership-current-organizations");
    container.replaceChildren();
    if (!user.assignments.length) {
        container.textContent = "No organization access assigned yet.";
        container.className = "state";
        return;
    }
    container.className = "membership-list";
    user.assignments.forEach(item => {
        const card = document.createElement("div");
        card.className = "membership-card";
        const title = document.createElement("h3");
        title.textContent = item.organizationName;
        const details = document.createElement("p");
        details.textContent = `${formatRole(item.role)} · ${formatScope(item)}`;
        card.append(title, details);
        if (!editedMembership) {
            const edit = smallButton("Edit access", "button-secondary", () => openMembershipForm(user, item));
            const remove = smallButton("Remove access", "button-danger", () => removeAccessFromForm(user.id, item));
            card.append(edit, remove);
        }
        container.append(card);
    });
}

async function removeAccessFromForm(userId, membership) {
    if (!window.confirm(`Remove access to ${membership.organizationName}?`)) return;
    try {
        await apiRequest(`/api/memberships/${membership.membershipId}`, {method: "DELETE"});
        await refreshUsers();
        const updatedUser = administrationUsers.find(user => user.id === userId);
        if (updatedUser) openMembershipForm(updatedUser);
        showAdminMessage("Organization access removed.");
    } catch (error) {
        showMembershipError(error.message);
    }
}

function renderOrganizationOptions(user, membership) {
    const container = document.querySelector("#membership-organizations");
    container.replaceChildren();
    const assigned = new Set(user.assignments.map(item => item.organizationId));
    const available = membership
        ? administrationOrganizations.filter(item => item.id === membership.organizationId)
        : administrationOrganizations.filter(item => !assigned.has(item.id));
    available.forEach(organization => {
        const option = document.createElement("div");
        option.className = "scope-option organization-access-option";
        const input = checkbox(organization.id, "membership-organization-input", Boolean(membership));
        input.id = `membership-organization-${organization.id}`;
        input.disabled = Boolean(membership);
        const name = document.createElement("label");
        name.htmlFor = input.id;
        name.textContent = organization.name;
        const role = document.createElement("select");
        role.className = "membership-organization-role";
        role.dataset.organizationId = organization.id;
        role.setAttribute("aria-label", `Role in ${organization.name}`);
        role.append(new Option("Limited employee", "LIMITED_EMPLOYEE"));
        role.append(new Option("Lab administrator", "LAB_ADMIN"));
        role.value = membership?.role || "LIMITED_EMPLOYEE";
        role.disabled = !input.checked;
        input.addEventListener("change", () => renderScopeOptions(
            checkedIds(".scope-lab-input"),
            checkedIds(".scope-room-input")
        ));
        input.addEventListener("change", () => role.disabled = !input.checked);
        option.append(input, name, role);
        container.append(option);
    });
    if (!available.length) container.textContent = "All available organizations are already assigned.";
}

function renderScopeOptions(selectedLabIds = [], selectedRoomIds = []) {
    const organizationIds = selectedOrganizationIds();
    const selectedLabs = new Set(selectedLabIds.map(Number));
    const selectedRooms = new Set(selectedRoomIds.map(Number));
    const container = document.querySelector("#membership-resource-tree");
    container.replaceChildren();

    organizationIds.forEach(organizationId => {
        const organization = administrationOrganizations.find(item => item.id === organizationId);
        const group = document.createElement("section");
        const heading = document.createElement("h3");
        heading.textContent = organization?.name || `Organization ${organizationId}`;
        group.append(heading);
        const labs = administrationLabs.filter(lab => lab.organizationId === organizationId);
        labs.forEach(lab => {
            const rooms = administrationRooms.filter(room => room.labId === lab.id);
            const branch = document.createElement("section");
            branch.className = "scope-tree-branch";
            const labOption = document.createElement("label");
            labOption.className = "scope-option scope-tree-lab";
            const labInput = checkbox(lab.id, "scope-lab-input", selectedLabs.has(lab.id));
            labInput.dataset.organizationId = organizationId;
            if (administrationMode === "LAB_ADMIN"
                    && administrationActorMembership.scopeType !== "ORGANIZATION"
                    && !administrationActorMembership.labIds.includes(lab.id)) {
                labInput.disabled = true;
            }
            const labIdentity = document.createElement("span");
            const labName = document.createElement("strong");
            labName.textContent = lab.name;
            const labSummary = document.createElement("small");
            labSummary.textContent = `${rooms.length} ${rooms.length === 1 ? "room" : "rooms"} · select lab for full access`;
            labIdentity.append(labName, labSummary);
            labOption.append(labInput, labIdentity);

            const roomList = document.createElement("div");
            roomList.className = "scope-tree-rooms";
            if (!rooms.length) {
                const empty = document.createElement("p");
                empty.className = "state";
                empty.textContent = "No rooms in this lab.";
                roomList.append(empty);
            }
            rooms.forEach(room => {
                const roomOption = document.createElement("label");
                roomOption.className = "scope-option scope-tree-room";
                const roomInput = checkbox(room.id, "scope-room-input", labInput.checked || selectedRooms.has(room.id));
                roomInput.dataset.organizationId = organizationId;
                roomInput.disabled = labInput.checked;
                roomInput.dataset.individuallySelected = String(selectedRooms.has(room.id));
                roomOption.append(roomInput, document.createTextNode(room.name));
                roomList.append(roomOption);
            });
            labInput.addEventListener("change", () => updateLabRoomSelection(labInput, roomList));
            branch.append(labOption, roomList);
            group.append(branch);
        });
        if (!labs.length) {
            const empty = document.createElement("p");
            empty.className = "state";
            empty.textContent = "No labs are available in this organization.";
            group.append(empty);
        }
        container.append(group);
    });
    if (!organizationIds.length) container.textContent = "Select an organization above to configure its resources.";
}

function checkbox(value, className, checked) {
    const input = document.createElement("input");
    input.type = "checkbox";
    input.value = value;
    input.className = className;
    input.checked = checked;
    return input;
}

function updateLabRoomSelection(labInput, roomList) {
    roomList.querySelectorAll(".scope-room-input").forEach(roomInput => {
        if (labInput.checked) {
            roomInput.dataset.individuallySelected = String(roomInput.checked);
            roomInput.checked = true;
            roomInput.disabled = true;
        } else {
            roomInput.disabled = false;
            roomInput.checked = roomInput.dataset.individuallySelected === "true";
        }
    });
}

function updateScopeVisibility() {
    document.querySelector("#specific-scope-fields").classList.toggle("hidden", membershipScope.value !== "SPECIFIC");
}

membershipForm.addEventListener("submit", async event => {
    event.preventDefault();
    const id = document.querySelector("#membership-id").value;
    const organizationIds = selectedOrganizationIds();
    if (!organizationIds.length) {
        showMembershipError("Select at least one organization.");
        return;
    }
    try {
        if (administrationMode === "LAB_ADMIN") {
            const userId = Number(document.querySelector("#membership-user-id").value);
            await apiRequest(`/api/team-access/organizations/${administrationOrganization.id}/users/${userId}/scope`, {
                method: "PUT", body: JSON.stringify(scopeForOrganization(administrationOrganization.id))
            });
            closeMembershipForm();
            await refreshUsers();
            showAdminMessage("Team assignment saved.");
            return;
        }
        if (id) {
            const organizationId = organizationIds[0];
            const payload = {role: roleForOrganization(organizationId), scope: scopeForOrganization(organizationId)};
            await apiRequest(`/api/memberships/${id}`, {method: "PUT", body: JSON.stringify(payload)});
        } else {
            const userId = Number(document.querySelector("#membership-user-id").value);
            for (const organizationId of organizationIds) {
                const payload = {userId, organizationId, role: roleForOrganization(organizationId), scope: scopeForOrganization(organizationId)};
                await apiRequest("/api/memberships", {method: "POST", body: JSON.stringify(payload)});
            }
        }
        closeMembershipForm();
        await refreshUsers();
        showAdminMessage(id ? "Organization access saved." : `Access added to ${organizationIds.length} ${organizationIds.length === 1 ? "organization" : "organizations"}.`);
    } catch (error) {
        showMembershipError(error.message);
    }
});

function roleForOrganization(organizationId) {
    return document.querySelector(`.membership-organization-role[data-organization-id="${organizationId}"]`).value;
}

function scopeForOrganization(organizationId) {
    const scope = {type: membershipScope.value, labIds: [], roomIds: []};
    if (scope.type === "SPECIFIC") {
        scope.labIds = checkedIds(`.scope-lab-input[data-organization-id="${organizationId}"]`);
        scope.roomIds = checkedIds(`.scope-room-input[data-organization-id="${organizationId}"]`);
    }
    return scope;
}

function showMembershipError(error) {
    const message = document.querySelector("#membership-form-error");
    message.textContent = error;
    message.classList.remove("hidden");
}

async function deleteMembership(membership) {
    if (!window.confirm(`Remove access to ${membership.organizationName}?`)) return;
    try {
        await apiRequest(`/api/memberships/${membership.membershipId}`, {method: "DELETE"});
        await refreshUsers();
        showAdminMessage("Organization access removed.");
    } catch (error) { showAdminMessage(error.message, true); }
}

async function refreshUsers() {
    if (administrationMode === "LAB_ADMIN") {
        const members = await apiRequest(`/api/team-access/organizations/${administrationOrganization.id}`);
        administrationUsers = members;
        renderTeamUsers(administrationUsers);
    } else {
        administrationUsers = await apiRequest("/api/users/managed");
        renderUsers(administrationUsers, currentAdministratorId);
    }
}

function checkedIds(selector) {
    return [...document.querySelectorAll(`${selector}:checked:not(:disabled)`)].map(input => Number(input.value));
}

function selectedOrganizationIds() {
    return [...document.querySelectorAll(".membership-organization-input:checked")]
        .map(input => Number(input.value));
}

function closeMembershipForm() {
    membershipFormPanel.classList.add("hidden");
}

async function changeStatus(user) {
    try {
        await apiRequest(`/api/users/${user.id}/status`, {method: "PUT", body: JSON.stringify({status: user.status === "ACTIVE" ? "DISABLED" : "ACTIVE"})});
        await refreshUsers();
        return true;
    } catch (error) {
        showAdminMessage(error.message, true);
        return false;
    }
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
function formatScope(item) {
    if (item.scopeType === "ORGANIZATION") return "all resources";
    const parts = [];
    const labNames = (item.labIds || []).map(id => administrationLabs.find(lab => Number(lab.id) === Number(id))?.name || `Lab ${id}`);
    const roomNames = (item.roomIds || []).map(id => administrationRooms.find(room => Number(room.id) === Number(id))?.name || `Room ${id}`);
    if (labNames.length) parts.push(`Lab: ${labNames.join(", ")} (all rooms)`);
    if (roomNames.length) parts.push(`Room: ${roomNames.join(", ")}`);
    return parts.join(", ");
}
function showAdminMessage(message, error = false) { adminMessage.textContent = message; adminMessage.className = `message ${error ? "message-error" : "message-success"}`; }
function closeForm() { userFormPanel.classList.add("hidden"); }
document.querySelector("#show-user-form").addEventListener("click", () => userFormPanel.classList.remove("hidden"));
document.querySelector("#close-user-form").addEventListener("click", closeForm);
document.querySelector("#cancel-user-form").addEventListener("click", closeForm);
roleInput.addEventListener("change", () => document.querySelector("#organization-field").classList.toggle("hidden", roleInput.value === "SUPER_ADMIN"));
membershipScope.addEventListener("change", updateScopeVisibility);
document.querySelector("#close-membership-form").addEventListener("click", closeMembershipForm);
document.querySelector("#cancel-membership-form").addEventListener("click", closeMembershipForm);
if (window.labMonitorOrganizationContext) initializeAdministration(window.labMonitorOrganizationContext.selected);
else document.addEventListener("labmonitor:organization-ready", event => initializeAdministration(event.detail), {once: true});

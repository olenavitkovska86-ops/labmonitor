const userSelect = document.querySelector("#prototype-user");
const organizationSelect = document.querySelector("#prototype-organization");
const roleSelect = document.querySelector("#prototype-role");
const organizationWide = document.querySelector("#organization-wide");
const tree = document.querySelector("#responsibility-tree");
const summary = document.querySelector("#responsibility-summary");
const responsibilityMessage = document.querySelector("#responsibility-message");
const membershipMessage = document.querySelector("#membership-message");
const administrationMessage = document.querySelector("#administration-message");
const userFormPanel = document.querySelector("#user-form-panel");
const userForm = document.querySelector("#user-form");
const userFormError = document.querySelector("#user-form-error");
const membershipFormPanel = document.querySelector("#membership-form-panel");
const membershipForm = document.querySelector("#membership-form");
const membershipFormError = document.querySelector("#membership-form-error");

let users = [];
let organizations = [];
let selectedUser;
let selectedMembership;
let responsibilityState = {organizationWide: false, labs: []};
let initialResponsibilityState = JSON.stringify(responsibilityState);

async function request(url, options = {}) {
    const token = localStorage.getItem("token");
    const response = await fetch(url, {
        ...options,
        headers: {
            ...(options.body ? {"Content-Type": "application/json"} : {}),
            ...(options.headers || {}),
            ...(token ? {Authorization: `Bearer ${token}`} : {})
        }
    });

    if (response.status === 401) {
        localStorage.removeItem("token");
        window.location.href = "/login.html";
        throw new Error("Authentication is required");
    }
    if (response.status === 403) throw new Error("SUPER_ADMIN access is required.");
    if (response.ok) return response.status === 204 ? null : response.json();

    const error = await response.json().catch(() => ({}));
    const details = error.details?.length ? `: ${error.details.join(", ")}` : "";
    throw new Error(`${error.message || error.error || `Request failed with status ${response.status}`}${details}`);
}

async function initialize() {
    setControlsDisabled(true);
    showMessage(administrationMessage, "Loading users and organizations...");
    try {
        [users, organizations] = await Promise.all([request("/api/users"), request("/api/organizations")]);
        populateCreateOrganizations();
        renderUsers();
        hideMessage(administrationMessage);
        if (users.length) await loadUser();
        else showMessage(administrationMessage, "No users found. Add a user to begin.");
    } catch (error) {
        showMessage(administrationMessage, error.message, true);
    }
}

function renderUsers(preferredUserId) {
    userSelect.replaceChildren(...users.map(user =>
        new Option(`${user.firstName} ${user.lastName} (${user.email})`, user.id)));
    if (preferredUserId && users.some(user => user.id === preferredUserId)) userSelect.value = preferredUserId;
}

async function loadUser() {
    selectedUser = users.find(user => user.id === Number(userSelect.value));
    const memberships = selectedUser?.memberships || [];
    organizationSelect.replaceChildren(...memberships.map(membership =>
        new Option(membership.organizationName, membership.organizationId)));

    if (!memberships.length) {
        selectedMembership = null;
        roleSelect.value = "LIMITED_EMPLOYEE";
        responsibilityState = {organizationWide: false, labs: []};
        renderTree();
        updateSummary();
        setControlsDisabled(true);
        document.querySelector("#show-membership-form").disabled = !selectedUser || selectedUser.globalRole === "SUPER_ADMIN";
        document.querySelector("#remove-membership").disabled = true;
        showMessage(membershipMessage, selectedUser?.globalRole === "SUPER_ADMIN"
            ? "This user is a global SUPER_ADMIN and has no organization membership."
            : "This user has no organization membership.");
        return;
    }

    setControlsDisabled(false);
    document.querySelector("#show-membership-form").disabled = false;
    document.querySelector("#remove-membership").disabled = false;
    hideMessage(membershipMessage);
    await loadMembership();
}

async function loadMembership() {
    selectedMembership = selectedUser.memberships.find(membership =>
        membership.organizationId === Number(organizationSelect.value));
    roleSelect.value = selectedMembership.role;
    await loadOrganizationStructure(selectedMembership.organizationId);
}

async function loadOrganizationStructure(organizationId) {
    tree.replaceChildren();
    summary.textContent = "Loading responsibility structure...";
    try {
        const labs = await request(`/api/labs?organizationId=${organizationId}`);
        const roomsByLab = await Promise.all(labs.map(async lab =>
            [lab.id, await request(`/api/rooms?labId=${lab.id}`)]));
        responsibilityState = {
            organizationWide: false,
            labs: labs.map(lab => ({
                id: lab.id,
                name: lab.name,
                whole: false,
                rooms: (roomsByLab.find(([labId]) => labId === lab.id)?.[1] || [])
                    .map(room => ({id: room.id, name: room.name, selected: false}))
            }))
        };
        initialResponsibilityState = JSON.stringify(responsibilityState);
        organizationWide.checked = false;
        renderTree();
        updateSummary();
        hideMessage(responsibilityMessage);
    } catch (error) {
        responsibilityState = {organizationWide: false, labs: []};
        renderTree();
        updateSummary();
        showMessage(responsibilityMessage, error.message, true);
    }
}

function renderTree() {
    tree.replaceChildren();
    for (const lab of responsibilityState.labs) {
        const group = document.createElement("section");
        group.className = "responsibility-lab";
        const labOption = scopeOption(lab.name, "Whole lab", lab.whole, checked => {
            lab.whole = checked;
            if (checked) lab.rooms.forEach(room => room.selected = false);
            renderTree();
            updateSummary();
        });
        labOption.classList.add("responsibility-lab-option");
        const rooms = document.createElement("div");
        rooms.className = "responsibility-rooms";
        for (const room of lab.rooms) {
            const option = scopeOption(room.name, "Room", room.selected, checked => {
                room.selected = checked;
                updateSummary();
            });
            option.querySelector("input").disabled = responsibilityState.organizationWide || lab.whole;
            rooms.append(option);
        }
        labOption.querySelector("input").disabled = responsibilityState.organizationWide;
        group.append(labOption, rooms);
        tree.append(group);
    }
}

function scopeOption(title, description, checked, onChange) {
    const label = document.createElement("label");
    label.className = "scope-option";
    const input = document.createElement("input");
    input.type = "checkbox";
    input.checked = checked;
    input.addEventListener("change", () => onChange(input.checked));
    const text = document.createElement("span");
    const strong = document.createElement("strong");
    strong.textContent = title;
    const small = document.createElement("small");
    small.textContent = description;
    text.append(strong, small);
    label.append(input, text);
    return label;
}

function updateOrganizationWide() {
    responsibilityState.organizationWide = organizationWide.checked;
    if (responsibilityState.organizationWide) responsibilityState.labs.forEach(lab => {
        lab.whole = false;
        lab.rooms.forEach(room => room.selected = false);
    });
    renderTree();
    updateSummary();
    hideMessage(responsibilityMessage);
}

function updateSummary() {
    if (!selectedMembership) {
        summary.textContent = "No organization membership";
        return;
    }
    if (responsibilityState.organizationWide) {
        summary.textContent = `${selectedMembership.organizationName} — whole organization`;
        return;
    }
    const scopes = responsibilityState.labs.flatMap(lab => lab.whole
        ? [lab.name]
        : lab.rooms.filter(room => room.selected).map(room => `${lab.name} / ${room.name}`));
    summary.textContent = scopes.length ? scopes.join(" · ") : "No responsibility selected";
}

async function saveMembershipRole() {
    if (!selectedMembership) return;
    try {
        const updated = await request(
            `/api/memberships/organizations/${selectedMembership.organizationId}/users/${selectedUser.id}/role`,
            {method: "PATCH", body: JSON.stringify({role: roleSelect.value})}
        );
        selectedMembership.role = updated.role;
        showMessage(membershipMessage, "Membership role updated.");
    } catch (error) {
        roleSelect.value = selectedMembership.role;
        showMessage(membershipMessage, error.message, true);
    }
}

function availableMembershipOrganizations() {
    const assignedIds = new Set((selectedUser?.memberships || []).map(membership => membership.organizationId));
    return organizations.filter(organization => !assignedIds.has(organization.id));
}

function openMembershipForm() {
    hideMessage(membershipFormError);
    const available = availableMembershipOrganizations();
    const membershipOrganization = document.querySelector("#membership-organization");
    membershipOrganization.replaceChildren(...available.map(organization =>
        new Option(organization.name, organization.id)));

    if (!available.length) {
        showMessage(membershipMessage, "This user already belongs to every organization.");
        return;
    }

    membershipForm.reset();
    membershipOrganization.value = String(available[0].id);
    membershipFormPanel.classList.remove("hidden");
    membershipOrganization.focus();
}

function closeMembershipForm() {
    membershipFormPanel.classList.add("hidden");
    membershipForm.reset();
    hideMessage(membershipFormError);
}

async function addMembership(event) {
    event.preventDefault();
    hideMessage(membershipFormError);
    const body = {
        userId: selectedUser.id,
        organizationId: Number(document.querySelector("#membership-organization").value),
        role: document.querySelector("#membership-role").value
    };

    try {
        await request("/api/memberships", {method: "POST", body: JSON.stringify(body)});
        closeMembershipForm();
        await refreshUsers(selectedUser.id, body.organizationId);
        showMessage(membershipMessage, "Organization membership added.");
    } catch (error) {
        showMessage(membershipFormError, error.message, true);
    }
}

async function removeMembership() {
    if (!selectedUser || !selectedMembership) return;
    const confirmed = window.confirm(
        `Remove ${selectedUser.firstName} ${selectedUser.lastName} from ${selectedMembership.organizationName}?`
    );
    if (!confirmed) return;

    try {
        await request(
            `/api/memberships/organizations/${selectedMembership.organizationId}/users/${selectedUser.id}`,
            {method: "DELETE"}
        );
        await refreshUsers(selectedUser.id);
        showMessage(membershipMessage, "Organization membership removed.");
    } catch (error) {
        showMessage(membershipMessage, error.message, true);
    }
}

async function refreshUsers(userId, organizationId) {
    users = await request("/api/users");
    renderUsers(userId);
    await loadUser();
    if (organizationId && selectedUser.memberships.some(membership => membership.organizationId === organizationId)) {
        organizationSelect.value = String(organizationId);
        await loadMembership();
    }
}

function saveResponsibilityPreview() {
    initialResponsibilityState = JSON.stringify(responsibilityState);
    showMessage(responsibilityMessage,
        "Responsibility selection saved in browser memory only. Assignment API is not available yet.");
}

function resetResponsibilityPreview() {
    responsibilityState = JSON.parse(initialResponsibilityState);
    organizationWide.checked = responsibilityState.organizationWide;
    renderTree();
    updateSummary();
    hideMessage(responsibilityMessage);
}

async function createUser(event) {
    event.preventDefault();
    hideMessage(userFormError);
    const body = {
        firstName: document.querySelector("#user-first-name").value.trim(),
        lastName: document.querySelector("#user-last-name").value.trim(),
        email: document.querySelector("#user-email").value.trim(),
        password: document.querySelector("#user-password").value,
        organization: Number(document.querySelector("#user-organization").value),
        role: document.querySelector("#user-role").value
    };
    try {
        const created = await request("/api/users", {method: "POST", body: JSON.stringify(body)});
        users = await request("/api/users");
        renderUsers(created.id);
        closeUserForm();
        await loadUser();
        showMessage(administrationMessage, "User created.");
    } catch (error) {
        showMessage(userFormError, error.message, true);
    }
}

function populateCreateOrganizations() {
    document.querySelector("#user-organization").replaceChildren(...organizations.map(organization =>
        new Option(organization.name, organization.id)));
    document.querySelector("#show-user-form").disabled = organizations.length === 0;
}

function openUserForm() {
    userForm.reset();
    hideMessage(userFormError);
    userFormPanel.classList.remove("hidden");
    document.querySelector("#user-first-name").focus();
}

function closeUserForm() {
    userFormPanel.classList.add("hidden");
    userForm.reset();
    hideMessage(userFormError);
}

function setControlsDisabled(disabled) {
    organizationSelect.disabled = disabled;
    roleSelect.disabled = disabled;
    organizationWide.disabled = disabled;
    document.querySelector("#save-membership-role").disabled = disabled;
    document.querySelector("#save-responsibility").disabled = disabled;
    document.querySelector("#reset-responsibility").disabled = disabled;
    document.querySelector("#show-membership-form").disabled = disabled;
    document.querySelector("#remove-membership").disabled = disabled;
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

userSelect.addEventListener("change", loadUser);
organizationSelect.addEventListener("change", loadMembership);
organizationWide.addEventListener("change", updateOrganizationWide);
document.querySelector("#save-membership-role").addEventListener("click", saveMembershipRole);
document.querySelector("#show-membership-form").addEventListener("click", openMembershipForm);
document.querySelector("#remove-membership").addEventListener("click", removeMembership);
document.querySelector("#close-membership-form").addEventListener("click", closeMembershipForm);
document.querySelector("#cancel-membership-form").addEventListener("click", closeMembershipForm);
membershipForm.addEventListener("submit", addMembership);
document.querySelector("#save-responsibility").addEventListener("click", saveResponsibilityPreview);
document.querySelector("#reset-responsibility").addEventListener("click", resetResponsibilityPreview);
document.querySelector("#show-user-form").addEventListener("click", openUserForm);
document.querySelector("#close-user-form").addEventListener("click", closeUserForm);
document.querySelector("#cancel-user-form").addEventListener("click", closeUserForm);
userForm.addEventListener("submit", createUser);

initialize();

const profileForm = document.querySelector("#profile-form");
const passwordForm = document.querySelector("#password-form");
const profileMessage = document.querySelector("#profile-message");

labMonitorAuthReady.then(auth => renderProfile(auth.user)).catch(error => showProfileMessage(error.message, true));

function renderProfile(user) {
    document.querySelector("#profile-email").value = user.email;
    document.querySelector("#profile-first-name").value = user.firstName || "";
    document.querySelector("#profile-last-name").value = user.lastName || "";
    document.querySelector("#profile-phone").value = user.phone || "";
    document.querySelector("#global-access").textContent = user.globalRole === "SUPER_ADMIN"
        ? "Global role: Super administrator"
        : "Global role: Standard user";

    const list = document.querySelector("#membership-list");
    list.replaceChildren();
    if (!user.memberships?.length) {
        list.textContent = "No organization access has been assigned.";
        return;
    }
    user.memberships.forEach(membership => {
        const item = document.createElement("article");
        item.className = "membership-card";
        const title = document.createElement("h3");
        title.textContent = membership.organizationName;
        const details = document.createElement("p");
        const scope = membership.scopeType === "ORGANIZATION"
            ? "All labs and rooms"
            : `${membership.labIds.length} lab(s), ${membership.roomIds.length} room(s)`;
        details.textContent = `${formatRole(membership.role)} · ${scope}`;
        item.append(title, details);
        list.append(item);
    });
}

profileForm.addEventListener("submit", async event => {
    event.preventDefault();
    try {
        const user = await apiRequest("/api/users/me", {
            method: "PUT",
            body: JSON.stringify({
                firstName: document.querySelector("#profile-first-name").value.trim(),
                lastName: document.querySelector("#profile-last-name").value.trim(),
                phone: document.querySelector("#profile-phone").value.trim() || null
            })
        });
        renderProfile(user);
        showProfileMessage("Profile updated.");
    } catch (error) {
        showProfileMessage(error.message, true);
    }
});

passwordForm.addEventListener("submit", async event => {
    event.preventDefault();
    try {
        await apiRequest("/auth/change-password", {
            method: "POST",
            body: JSON.stringify({
                oldPassword: document.querySelector("#old-password").value,
                newPassword: document.querySelector("#new-password").value
            })
        });
        passwordForm.reset();
        showProfileMessage("Password changed.");
    } catch (error) {
        showProfileMessage(error.message, true);
    }
});

function formatRole(role) {
    return role === "LAB_ADMIN" ? "Lab administrator" : "Limited employee";
}

function showProfileMessage(text, error = false) {
    profileMessage.textContent = text;
    profileMessage.className = `message ${error ? "message-error" : "message-success"}`;
}


const labMonitorAuthReady = apiRequest("/api/users/me").then(user => {
    const globalPermissions = new Set(user.permissions || []);

    const auth = {
        user,
        has(permission) {
            return globalPermissions.has(permission);
        },
        membership(organizationId) {
            return user.memberships?.find(item => item.organizationId === Number(organizationId)) || null;
        },
        hasForOrganization(permission, organizationId) {
            if (globalPermissions.has(permission)) return true;
            return this.membership(organizationId)?.permissions?.includes(permission) || false;
        }
    };

    window.labMonitorAuth = auth;
    renderAccountMenu(user);
    applyStaticPermissions(auth);
    document.dispatchEvent(new CustomEvent("labmonitor:auth-ready", {detail: auth}));
    return auth;
});

function applyStaticPermissions(auth) {
    document.querySelectorAll("[data-permission]").forEach(element => {
        const allowed = auth.has(element.dataset.permission);
        if (!allowed) element.classList.add("hidden");
        if (allowed && element.hasAttribute("data-permission-reveal")) element.classList.remove("hidden");
    });
}

function renderAccountMenu(user) {
    const topbar = document.querySelector(".topbar");
    if (!topbar || topbar.querySelector(".account-menu")) return;

    const menu = document.createElement("div");
    menu.className = "account-menu";

    const profile = document.createElement("a");
    profile.className = "account-link";
    profile.href = "/profile.html";
    profile.textContent = [user.firstName, user.lastName].filter(Boolean).join(" ") || user.email;

    const logout = document.createElement("button");
    logout.className = "account-logout";
    logout.type = "button";
    logout.textContent = "Log out";
    logout.addEventListener("click", async () => {
        logout.disabled = true;
        try {
            await apiFetch("/auth/logout", {method: "POST"});
        } catch (error) {
            notifications.title = error.message;
        } finally {
            window.location.href = "/login.html";
        }
    });

    const notifications = document.createElement("button");
    notifications.className = "notification-toggle";
    notifications.type = "button";
    renderNotificationToggle(notifications, user.alertNotificationsEnabled);
    notifications.addEventListener("click", async () => {
        notifications.disabled = true;
        try {
            const updated = await apiRequest("/api/users/me/preferences/notifications", {
                method: "PUT",
                body: JSON.stringify({alertNotificationsEnabled: !user.alertNotificationsEnabled})
            });
            user.alertNotificationsEnabled = updated.alertNotificationsEnabled;
            renderNotificationToggle(notifications, user.alertNotificationsEnabled);
            showNotificationPreferenceStatus(user.alertNotificationsEnabled);
            document.dispatchEvent(new CustomEvent("labmonitor:notifications-changed", {
                detail: {enabled: user.alertNotificationsEnabled}
            }));
        } finally {
            notifications.disabled = false;
        }
    });

    menu.append(notifications, profile, logout);
    topbar.append(menu);
}

function renderNotificationToggle(button, enabled) {
    button.replaceChildren();
    const icon = document.createElement("span");
    icon.className = "notification-toggle-icon";
    icon.setAttribute("aria-hidden", "true");
    icon.textContent = enabled ? "🔔" : "🔕";
    const label = document.createElement("span");
    label.className = "notification-toggle-label";
    label.textContent = enabled ? "Notifications on" : "Notifications muted";
    button.append(icon, label);
    button.title = enabled ? "Mute alert notifications" : "Enable alert notifications";
    button.setAttribute("aria-label", button.title);
    button.setAttribute("aria-pressed", String(!enabled));
}

function showNotificationPreferenceStatus(enabled) {
    document.querySelector(".notification-preference-status")?.remove();
    const status = document.createElement("div");
    status.className = "notification-preference-status";
    status.setAttribute("role", "status");
    status.textContent = enabled
        ? "Popup notifications enabled."
        : "Popup notifications muted. Alerts remain available in the Alerts section.";
    document.body.append(status);
    window.setTimeout(() => status.remove(), 4000);
}

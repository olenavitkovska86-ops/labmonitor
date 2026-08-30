(() => {
    const topbar = document.querySelector(".topbar");
    if (!topbar || document.querySelector(".app-sidebar")) return;
    const currentPath = window.location.pathname === "/index.html" ? "/" : window.location.pathname;
    const items = [
        {label: "Overview", icon: "⌂", href: "/analytics.html", paths: ["/", "/analytics.html"]},
        {label: "Monitor", icon: "▦", href: "/monitor.html", paths: ["/monitor.html", "/sensor-readings.html"]},
        {label: "Alerts", icon: "△", href: "/alerts.html", paths: ["/alerts.html"]},
        {label: "Sessions", icon: "◷", href: "/monitoring-sessions.html", paths: ["/monitoring-sessions.html"]},
        {label: "History & exports", icon: "⇩", href: "/history.html", paths: ["/history.html"]}
    ];

    const sidebar = document.createElement("aside");
    sidebar.className = "app-sidebar";
    const brand = document.createElement("a");
    brand.className = "app-sidebar-brand";
    brand.href = "/analytics.html";
    brand.innerHTML = '<span class="app-sidebar-mark" aria-hidden="true">L</span><span>LabMonitor</span>';
    const navigation = document.createElement("nav");
    navigation.className = "app-sidebar-navigation";
    navigation.setAttribute("aria-label", "Primary navigation");
    navigation.append(createLink(items[0]), createLabel("Monitoring"));
    items.slice(1).forEach(item => navigation.append(createLink(item)));

    const administrationLabel = createLabel("Administration");
    administrationLabel.classList.add("hidden");
    const organizationAdministrationLink = createLink({label: "Organizations", icon: "◇", href: "/organizations.html", paths: ["/organizations.html"]});
    organizationAdministrationLink.classList.add("hidden");
    const labAdministrationLink = createLink({label: "Laboratories", icon: "⌂", href: "/labs.html", paths: ["/labs.html"]});
    labAdministrationLink.classList.add("hidden");
    const roomAdministrationLink = createLink({label: "Rooms", icon: "□", href: "/rooms.html", paths: ["/rooms.html"]});
    roomAdministrationLink.classList.add("hidden");
    const sensorAdministrationLink = createLink({label: "Sensors", icon: "⌁", href: "/sensors.html", paths: ["/sensors.html", "/sensor-client.html", "/iphone-sensor.html"]});
    sensorAdministrationLink.classList.add("hidden");
    const administrationLink = createLink({label: "Users & access", icon: "♙", href: "/administration.html", paths: ["/administration.html"]});
    administrationLink.classList.add("hidden");
    sidebar.append(brand, navigation, administrationLabel, organizationAdministrationLink,
        labAdministrationLink, roomAdministrationLink, sensorAdministrationLink, administrationLink);
    topbar.insertAdjacentElement("afterend", sidebar);
    document.body.classList.add("has-app-sidebar");

    const organizationScopedPaths = new Set([
        "/analytics.html", "/monitor.html", "/alerts.html", "/monitoring-sessions.html", "/history.html",
        "/administration.html"
    ]);
    const context = organizationScopedPaths.has(currentPath) ? document.createElement("label") : null;
    if (context) {
        context.className = "topbar-organization";
        context.htmlFor = "shell-organization-select";
        context.innerHTML = '<span>Organization</span><select id="shell-organization-select" disabled><option>Loading…</option></select>';
        topbar.insertBefore(context, topbar.firstChild);
    }

    labMonitorAuthReady.then(async auth => {
        const canManageOrganizations = auth.has("organizations.manage");
        const canManageLabs = auth.has("labs.manage");
        const canManageRooms = auth.has("rooms.manage");
        const canManageUsers = auth.has("users.manage");
        const canManageTeam = auth.user.memberships?.some(item => item.permissions?.includes("team.access.manage"));
        const canAdministerSensors = auth.has("sensors.manage")
            || auth.user.memberships?.some(membership => membership.permissions?.includes("sensors.settings.update"));
        const canAdminister = canManageOrganizations || canManageLabs || canManageRooms
            || canAdministerSensors || canManageUsers;
        administrationLabel.classList.toggle("hidden", !canAdminister);
        organizationAdministrationLink.classList.toggle("hidden", !canManageOrganizations);
        labAdministrationLink.classList.toggle("hidden", !canManageLabs);
        roomAdministrationLink.classList.toggle("hidden", !canManageRooms);
        sensorAdministrationLink.classList.toggle("hidden", !canAdministerSensors);
        administrationLink.classList.toggle("hidden", !canManageUsers && !canManageTeam);
        if (!context) return;
        const organizations = await apiRequest("/api/organizations");
        const select = context.querySelector("select");
        select.replaceChildren();
        if (!organizations.length) {
            select.append(new Option("No organization access", ""));
            window.labMonitorOrganizationContext = {selected: null, organizations};
            document.dispatchEvent(new CustomEvent("labmonitor:organization-ready", {detail: null}));
            return;
        }
        organizations.forEach(organization => {
            const membership = auth.membership(organization.id);
            const role = auth.user.globalRole === "SUPER_ADMIN"
                ? "Super admin"
                : formatOrganizationRole(membership?.role);
            select.append(new Option(role ? `${organization.name} — ${role}` : organization.name, organization.id));
        });
        const storageKey = `labmonitor.organization.${auth.user.id}`;
        const requestedId = new URLSearchParams(window.location.search).get("organizationId");
        const rememberedId = localStorage.getItem(storageKey);
        const selected = organizations.find(item => String(item.id) === requestedId)
            || organizations.find(item => String(item.id) === rememberedId)
            || organizations[0];
        if (canManageUsers && currentPath === "/administration.html") context.classList.add("hidden");
        administrationLink.classList.toggle("hidden", !canManageUsers
            && !auth.hasForOrganization("team.access.manage", selected.id));
        select.value = selected.id;
        select.disabled = organizations.length === 1;
        localStorage.setItem(storageKey, selected.id);
        window.labMonitorOrganizationContext = {selected, organizations};
        preserveOrganizationInNavigation(selected.id);
        document.dispatchEvent(new CustomEvent("labmonitor:organization-ready", {detail: selected}));
        select.addEventListener("change", () => {
            localStorage.setItem(storageKey, select.value);
            const url = new URL(window.location.href);
            url.searchParams.set("organizationId", select.value);
            url.searchParams.delete("labId");
            url.searchParams.delete("roomId");
            url.searchParams.delete("sensorId");
            url.searchParams.delete("alertId");
            url.searchParams.delete("sessionId");
            window.location.assign(url);
        });
    }).catch(() => {
        administrationLabel.classList.add("hidden");
        organizationAdministrationLink.classList.add("hidden");
        labAdministrationLink.classList.add("hidden");
        roomAdministrationLink.classList.add("hidden");
        administrationLink.classList.add("hidden");
        sensorAdministrationLink.classList.add("hidden");
    });

    function createLabel(text) {
        const label = document.createElement("p");
        label.className = "app-sidebar-label";
        label.textContent = text;
        return label;
    }

    function formatOrganizationRole(role) {
        if (role === "LAB_ADMIN") return "Lab admin";
        if (role === "LIMITED_EMPLOYEE") return "Limited employee";
        return "";
    }

    function createLink(item) {
        const link = document.createElement("a");
        link.className = "app-sidebar-link";
        link.href = item.href;
        link.innerHTML = `<span class="app-sidebar-icon" aria-hidden="true">${item.icon}</span><span>${item.label}</span>`;
        if (item.paths.includes(currentPath)) {
            link.classList.add("app-sidebar-link-active");
            link.setAttribute("aria-current", "page");
        }
        return link;
    }

    function preserveOrganizationInNavigation(organizationId) {
        sidebar.querySelectorAll("a").forEach(link => {
            const url = new URL(link.href, window.location.origin);
            if (!organizationScopedPaths.has(url.pathname)) return;
            url.searchParams.set("organizationId", organizationId);
            link.href = `${url.pathname}?${url.searchParams}`;
        });
        const brandUrl = new URL(brand.href, window.location.origin);
        brandUrl.searchParams.set("organizationId", organizationId);
        brand.href = `${brandUrl.pathname}?${brandUrl.searchParams}`;
    }
})();

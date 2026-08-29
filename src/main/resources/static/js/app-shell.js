(() => {
    const topbar = document.querySelector(".topbar");
    if (!topbar || document.querySelector(".app-sidebar")) return;
    const currentPath = window.location.pathname === "/index.html" ? "/" : window.location.pathname;
    const items = [
        {label: "Overview", icon: "⌂", href: "/analytics.html", paths: ["/", "/analytics.html"]},
        {label: "Monitor", icon: "▦", href: "/monitor.html", paths: ["/monitor.html", "/organizations.html", "/labs.html", "/rooms.html", "/sensors.html", "/sensor-readings.html"]},
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
    const administrationLink = createLink({label: "Users & access", icon: "♙", href: "/administration.html", paths: ["/administration.html"]});
    administrationLink.classList.add("hidden");
    sidebar.append(brand, navigation, administrationLabel, administrationLink);
    topbar.insertAdjacentElement("afterend", sidebar);
    document.body.classList.add("has-app-sidebar");

    const organizationScopedPaths = new Set(["/analytics.html", "/monitor.html", "/alerts.html"]);
    const context = organizationScopedPaths.has(currentPath) ? document.createElement("label") : null;
    if (context) {
        context.className = "topbar-organization";
        context.htmlFor = "shell-organization-select";
        context.innerHTML = '<span>Organization</span><select id="shell-organization-select" disabled><option>Loading…</option></select>';
        topbar.insertBefore(context, topbar.firstChild);
    }

    labMonitorAuthReady.then(async auth => {
        const hidden = !auth.has("users.manage");
        administrationLabel.classList.toggle("hidden", hidden);
        administrationLink.classList.toggle("hidden", hidden);
        if (!context) return;
        const organizations = await apiRequest("/api/organizations");
        const select = context.querySelector("select");
        select.replaceChildren();
        if (!organizations.length) {
            select.append(new Option("No organization access", ""));
            return;
        }
        organizations.forEach(organization => select.append(new Option(organization.name, organization.id)));
        const requestedId = new URLSearchParams(window.location.search).get("organizationId");
        const selected = organizations.find(item => String(item.id) === requestedId) || organizations[0];
        select.value = selected.id;
        select.disabled = organizations.length === 1;
        document.dispatchEvent(new CustomEvent("labmonitor:organization-ready", {detail: selected}));
        select.addEventListener("change", () => {
            const url = new URL(window.location.href);
            url.searchParams.set("organizationId", select.value);
            url.searchParams.delete("labId");
            url.searchParams.delete("roomId");
            url.searchParams.delete("sensorId");
            window.location.assign(url);
        });
    }).catch(() => {
        administrationLabel.classList.add("hidden");
        administrationLink.classList.add("hidden");
    });

    function createLabel(text) {
        const label = document.createElement("p");
        label.className = "app-sidebar-label";
        label.textContent = text;
        return label;
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
})();

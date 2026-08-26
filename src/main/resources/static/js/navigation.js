(() => {
    if (!document.querySelector(".topbar")) return;

    const items = [
        {label: "Overview", href: "/analytics.html", pages: ["/analytics.html"]},
        {
            label: "Monitor",
            href: "/organizations.html",
            pages: [
                "/organizations.html",
                "/labs.html",
                "/rooms.html",
                "/sensors.html",
                "/sensor-readings.html"
            ]
        },
        {label: "History & exports", href: "/history.html", pages: ["/history.html"]},
        {label: "Alerts", href: "/alerts.html", pages: ["/alerts.html"]},
        {label: "Sessions", href: "/monitoring-sessions.html", pages: ["/monitoring-sessions.html"]}
    ];

    const currentPath = window.location.pathname === "/index.html" ? "/" : window.location.pathname;
    const sidebar = document.createElement("aside");
    sidebar.className = "app-sidebar";
    const sectionLabel = document.createElement("p");
    sectionLabel.className = "app-sidebar-label";
    sectionLabel.textContent = "Workspace";

    const navigation = document.createElement("nav");
    navigation.className = "app-sidebar-navigation";
    navigation.setAttribute("aria-label", "Primary navigation");

    for (const item of items) {
        const link = document.createElement("a");
        link.className = "app-sidebar-link";
        link.href = item.href;
        link.textContent = item.label;
        if (item.pages.includes(currentPath)) {
            link.classList.add("app-sidebar-link-active");
            link.setAttribute("aria-current", "page");
        }
        navigation.append(link);
    }

    sidebar.append(sectionLabel, navigation);
    const previewRole = new URLSearchParams(window.location.search).get("overviewRole")
        || sessionStorage.getItem("overviewPreviewRole");
    const administrationLabel = document.createElement("p");
    administrationLabel.className = "app-sidebar-label app-sidebar-secondary-label";
    administrationLabel.textContent = "Administration";
    const administrationLink = document.createElement("a");
    administrationLink.className = "app-sidebar-link";
    administrationLink.href = "/administration.html";
    administrationLink.textContent = "Users & responsibility";
    if (currentPath === "/administration.html") {
        administrationLink.classList.add("app-sidebar-link-active");
        administrationLink.setAttribute("aria-current", "page");
    }
    const toggleAdministration = role => {
        const hidden = role !== "SUPER_ADMIN" && currentPath !== "/administration.html";
        administrationLabel.classList.toggle("hidden", hidden);
        administrationLink.classList.toggle("hidden", hidden);
    };
    toggleAdministration(previewRole);
    document.addEventListener("labmonitor:preview-role-change", event => toggleAdministration(event.detail));
    sidebar.append(administrationLabel, administrationLink);
    document.body.classList.add("has-app-sidebar");
    document.querySelector(".topbar").insertAdjacentElement("afterend", sidebar);
})();

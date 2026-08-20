async function loadAlertCount() {
    const token = localStorage.getItem("token");
    if (!token) return;

    try {
        const response = await fetch("/api/alerts/unresolved-count", {
            headers: {Authorization: `Bearer ${token}`}
        });
        if (!response.ok) return;

        const result = await response.json();
        const badge = document.querySelector("#alert-count");
        badge.textContent = result.unresolvedAlerts > 99 ? "99+" : result.unresolvedAlerts;
        badge.setAttribute(
            "aria-label",
            `${result.unresolvedAlerts} unresolved ${result.unresolvedAlerts === 1 ? "alert" : "alerts"}`
        );
        badge.classList.toggle("alert-count-zero", result.unresolvedAlerts === 0);
        badge.classList.remove("hidden");
    } catch {
        // The home page remains usable when the optional counter cannot be loaded.
    }
}

loadAlertCount();

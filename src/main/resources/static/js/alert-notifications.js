const dismissedHighPriorityAlertIds = new Set();
const alertNotificationContainer = document.createElement("div");
alertNotificationContainer.className = "alert-notifications";
alertNotificationContainer.setAttribute("aria-live", "assertive");
document.body.append(alertNotificationContainer);

function readDismissedAlertIds() {
    return dismissedHighPriorityAlertIds;
}

function rememberDismissedAlertId(id) {
    dismissedHighPriorityAlertIds.add(id);
}

async function checkHighPriorityAlerts() {
    if (document.visibilityState !== "visible") return;

    try {
        const auth = await labMonitorAuthReady;
        if (!auth.user.alertNotificationsEnabled) {
            alertNotificationContainer.replaceChildren();
            return;
        }
        const responses = await Promise.all([
            apiFetch("/api/alerts?status=ACTIVE&severity=HIGH"),
            apiFetch("/api/alerts?status=ACTIVE&severity=CRITICAL")
        ]);
        if (responses.some(response => response.status === 401 || response.status === 403)) return;
        if (responses.some(response => !response.ok)) return;

        const alerts = (await Promise.all(responses.map(response => response.json())))
            .flat()
            .sort((left, right) => new Date(left.createdAt) - new Date(right.createdAt));
        const activeIds = new Set(alerts.map(alert => String(alert.id)));
        alertNotificationContainer.querySelectorAll("[data-alert-id]").forEach(toast => {
            if (!activeIds.has(toast.dataset.alertId)) toast.remove();
        });
        alerts.forEach(alert => {
            const existing = alertNotificationContainer.querySelector(`[data-alert-id="${alert.id}"]`);
            if (existing) renderAlertNotification(existing, alert);
        });
        const dismissed = readDismissedAlertIds();
        const activeAlertIds = new Set(alerts.map(alert => alert.id));
        const activeDismissals = new Set([...dismissed].filter(id => activeAlertIds.has(id)));
        dismissedHighPriorityAlertIds.clear();
        activeDismissals.forEach(id => dismissedHighPriorityAlertIds.add(id));
        const visibleIds = new Set(
            [...alertNotificationContainer.querySelectorAll("[data-alert-id]")]
                .map(toast => toast.dataset.alertId)
        );
        const alertsToShow = alerts.filter(alert =>
            !activeDismissals.has(alert.id) && !visibleIds.has(String(alert.id))
        );
        alertsToShow.slice(-3).forEach(showAlertNotification);
    } catch {
        // Notifications are optional; the current page remains usable if polling fails.
    }
}

function showAlertNotification(alert) {
    const toast = document.createElement("section");
    toast.dataset.alertId = alert.id;
    toast.setAttribute("role", "alert");

    const heading = document.createElement("strong");
    heading.className = "alert-notification-heading";
    const message = document.createElement("p");
    message.className = "alert-notification-message";
    const context = document.createElement("span");
    context.className = "alert-notification-context";

    const actions = document.createElement("div");
    actions.className = "alert-notification-actions";
    const view = document.createElement("a");
    view.className = "button button-primary button-small";
    view.href = `/alerts.html?alertId=${alert.id}`;
    view.textContent = "View alert";
    const dismiss = document.createElement("button");
    dismiss.className = "button button-link button-small";
    dismiss.type = "button";
    dismiss.textContent = "Dismiss";
    dismiss.addEventListener("click", () => {
        rememberDismissedAlertId(alert.id);
        toast.remove();
    });
    actions.append(view, dismiss);
    toast.append(heading, message, context, actions);
    renderAlertNotification(toast, alert);
    alertNotificationContainer.append(toast);
}

function renderAlertNotification(toast, alert) {
    toast.className = `alert-notification alert-notification-${alert.severity.toLowerCase()}`;
    toast.querySelector(".alert-notification-heading").textContent = alert.severity === "CRITICAL"
        ? "Critical alert"
        : "High-priority alert";
    toast.querySelector(".alert-notification-message").textContent = alert.latestValue == null
        ? (alert.message || alert.title)
        : `${alert.title}: latest ${alert.latestValue} · most extreme ${alert.mostExtremeValue}`;
    toast.querySelector(".alert-notification-context").textContent =
        `Room ${alert.roomId} · Updated ${formatNotificationTime(new Date())}`;
}

function formatNotificationTime(value) {
    return new Intl.DateTimeFormat(undefined, {timeStyle: "medium"}).format(new Date(value));
}

function dispatchMonitoringRefresh() {
    if (document.visibilityState === "visible") {
        document.dispatchEvent(new Event("labmonitor:refresh"));
    }
}

document.addEventListener("labmonitor:refresh", checkHighPriorityAlerts);
labMonitorAuthReady.then(checkHighPriorityAlerts).catch(() => {});
setInterval(dispatchMonitoringRefresh, 5000);
document.addEventListener("visibilitychange", dispatchMonitoringRefresh);
document.addEventListener("labmonitor:notifications-changed", event => {
    if (!event.detail.enabled) alertNotificationContainer.replaceChildren();
    else checkHighPriorityAlerts();
});

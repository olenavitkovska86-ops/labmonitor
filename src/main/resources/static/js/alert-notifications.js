const alertNotificationStorageKey = "seenHighPriorityAlertIds";
const alertNotificationContainer = document.createElement("div");
alertNotificationContainer.className = "alert-notifications";
alertNotificationContainer.setAttribute("aria-live", "assertive");
document.body.append(alertNotificationContainer);

function readSeenAlertIds() {
    try {
        return new Set(JSON.parse(sessionStorage.getItem(alertNotificationStorageKey) || "[]"));
    } catch {
        return new Set();
    }
}

function rememberAlertIds(ids) {
    const seen = readSeenAlertIds();
    ids.forEach(id => seen.add(id));
    sessionStorage.setItem(alertNotificationStorageKey, JSON.stringify([...seen].slice(-500)));
}

async function checkHighPriorityAlerts() {
    const token = localStorage.getItem("token");
    if (!token || document.visibilityState !== "visible") return;

    try {
        const headers = {Authorization: `Bearer ${token}`};
        const responses = await Promise.all([
            fetch("/api/alerts?status=ACTIVE&severity=HIGH", {headers}),
            fetch("/api/alerts?status=ACTIVE&severity=CRITICAL", {headers})
        ]);
        if (responses.some(response => response.status === 401 || response.status === 403)) return;
        if (responses.some(response => !response.ok)) return;

        const alerts = (await Promise.all(responses.map(response => response.json())))
            .flat()
            .sort((left, right) => new Date(left.createdAt) - new Date(right.createdAt));
        const seen = readSeenAlertIds();
        const unseen = alerts.filter(alert => !seen.has(alert.id));
        rememberAlertIds(alerts.map(alert => alert.id));
        unseen.slice(-3).forEach(showAlertNotification);
    } catch {
        // Notifications are optional; the current page remains usable if polling fails.
    }
}

function showAlertNotification(alert) {
    const toast = document.createElement("section");
    toast.className = `alert-notification alert-notification-${alert.severity.toLowerCase()}`;
    toast.setAttribute("role", "alert");

    const heading = document.createElement("strong");
    heading.textContent = alert.severity === "CRITICAL" ? "Critical alert" : "High-priority alert";
    const message = document.createElement("p");
    message.textContent = alert.message || alert.title;
    const context = document.createElement("span");
    context.className = "alert-notification-context";
    context.textContent = `Room ${alert.roomId} · ${formatNotificationTime(alert.createdAt)}`;

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
    dismiss.addEventListener("click", () => toast.remove());
    actions.append(view, dismiss);
    toast.append(heading, message, context, actions);
    alertNotificationContainer.append(toast);
}

function formatNotificationTime(value) {
    return new Intl.DateTimeFormat(undefined, {timeStyle: "medium"}).format(new Date(value));
}

checkHighPriorityAlerts();
setInterval(checkHighPriorityAlerts, 5000);
document.addEventListener("visibilitychange", checkHighPriorityAlerts);

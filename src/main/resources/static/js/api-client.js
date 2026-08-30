async function apiFetch(url, options = {}) {
    const method = (options.method || "GET").toUpperCase();
    const headers = new Headers(options.headers || {});
    if (!["GET", "HEAD", "OPTIONS", "TRACE"].includes(method)) {
        const csrfResponse = await fetch("/api/csrf", {cache: "no-store"});
        if (csrfResponse.status === 401) {
            if (window.location.pathname !== "/login.html") window.location.href = "/login.html";
            throw new Error("Your session has expired. Please sign in again.");
        }
        if (!csrfResponse.ok) throw new Error("Unable to initialize request security.");
        const csrf = await csrfResponse.json();
        headers.set(csrf.headerName, csrf.token);
    }

    const response = await fetch(url, {...options, headers});
    if (response.status === 401) {
        if (window.location.pathname !== "/login.html") window.location.href = "/login.html";
        throw new Error("Authentication is required.");
    }
    return response;
}

async function apiRequest(url, options = {}) {
    const headers = new Headers(options.headers || {});
    if (options.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
    const response = await apiFetch(url, {...options, headers});
    if (response.ok) return response.status === 204 ? null : response.json();

    const error = await response.json().catch(() => ({}));
    const details = error.details?.length ? `: ${error.details.join(", ")}` : "";
    const message = response.status === 403
        ? "You do not have permission to perform this action."
        : error.message || error.error || `Request failed with status ${response.status}`;
    throw new Error(`${message}${details}`);
}

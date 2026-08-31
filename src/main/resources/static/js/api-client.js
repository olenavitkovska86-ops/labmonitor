let csrfTokenPromise;
let sessionRefreshPromise;

async function csrfToken(force = false) {
    if (force) csrfTokenPromise = null;
    if (!csrfTokenPromise) {
        csrfTokenPromise = authenticatedFetch("/api/csrf", {cache: "no-store"}).then(async response => {
            if (!response.ok) throw new Error("Unable to initialize request security.");
            return response.json();
        }).catch(error => {
            csrfTokenPromise = null;
            throw error;
        });
    }
    return csrfTokenPromise;
}

async function apiFetch(url, options = {}) {
    const method = (options.method || "GET").toUpperCase();
    const headers = new Headers(options.headers || {});
    if (!["GET", "HEAD", "OPTIONS", "TRACE"].includes(method)) {
        const csrf = await csrfToken();
        headers.set(csrf.headerName, csrf.token);
    }

    let response = await authenticatedFetch(url, {...options, headers});
    const requestMethod = (options.method || "GET").toUpperCase();
    if (response.status === 403 && ["PUT", "PATCH", "DELETE"].includes(requestMethod)) {
        const freshCsrf = await csrfToken(true);
        headers.set(freshCsrf.headerName, freshCsrf.token);
        response = await authenticatedFetch(url, {...options, headers});
    }
    return response;
}

async function authenticatedFetch(url, options = {}) {
    let response = await fetch(url, options);
    if (response.status === 401) {
        csrfTokenPromise = null;
        const refreshed = await refreshSession();
        if (refreshed) response = await fetch(url, options);
    }
    if (response.status === 401) {
        redirectToLogin();
        throw new Error("Your session has expired. Please sign in again.");
    }
    return response;
}

async function refreshSession() {
    if (!sessionRefreshPromise) {
        sessionRefreshPromise = fetch("/auth/refresh", {method: "POST"})
            .then(response => response.ok)
            .catch(() => false)
            .finally(() => { sessionRefreshPromise = null; });
    }
    return sessionRefreshPromise;
}

async function apiRequest(url, options = {}) {
    const headers = new Headers(options.headers || {});
    if (options.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json");
    const response = await apiFetch(url, {...options, headers});
    if (response.ok) return response.status === 204 ? null : response.json();

    const error = await readApiError(response);
    const details = error.details?.length ? `: ${error.details.join(", ")}` : "";
    const message = response.status === 403
        ? "You do not have permission to perform this action."
        : error.message || error.error || `Request failed with status ${response.status}`;
    throw new Error(`${message}${details}`);
}

async function readApiError(response) {
    return response.json().catch(() => ({}));
}

function redirectToLogin() {
    if (!["/login", "/login.html"].includes(window.location.pathname)) {
        window.location.assign("/login.html");
    }
}

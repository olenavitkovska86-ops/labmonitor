let csrfTokenPromise;

async function csrfToken() {
    if (!csrfTokenPromise) {
        csrfTokenPromise = fetch("/api/csrf", {cache: "no-store"}).then(async response => {
            if (response.status === 401) {
                redirectToLogin();
                throw new Error("Your session has expired. Please sign in again.");
            }
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

    const response = await fetch(url, {...options, headers});
    if (response.status === 401) {
        csrfTokenPromise = null;
        redirectToLogin();
        throw new Error("Authentication is required.");
    }
    return response;
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

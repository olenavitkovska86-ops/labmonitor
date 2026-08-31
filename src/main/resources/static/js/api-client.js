let csrfTokenPromise;
let csrfCookieSnapshot;
let sessionRefreshPromise;

async function csrfToken(force = false) {
    if (force) { csrfTokenPromise = null; csrfCookieSnapshot = null; }
    const currentCookie = readXsrfCookie();
    if (csrfTokenPromise && currentCookie !== csrfCookieSnapshot) {
        csrfTokenPromise = null;
        csrfCookieSnapshot = null;
    }
    if (!csrfTokenPromise) {
        const requestedCookie = currentCookie;
        csrfTokenPromise = authenticatedFetch("/api/csrf", {cache: "no-store"}).then(async response => {
            if (!response.ok) throw new Error("Unable to initialize request security.");
            const value = await response.json();
            csrfCookieSnapshot = readXsrfCookie() || requestedCookie;
            return value;
        }).catch(error => {
            csrfTokenPromise = null;
            throw error;
        });
    }
    return csrfTokenPromise;
}

async function apiFetch(url, options = {}) {
    const method = (options.method || "GET").toUpperCase();
    const unsafe = !["GET", "HEAD", "OPTIONS", "TRACE"].includes(method);
    let headers;
    try { headers = await requestHeaders(options, unsafe); }
    catch (error) {
        if (unsafe && await refreshSession()) { csrfTokenPromise = null; headers = await requestHeaders(options, unsafe); }
        else { redirectToLogin(); throw error; }
    }
    let response = await authenticatedFetch(url, {...options, headers});
    if (response.status === 401) {
        const refreshed = await refreshSession();
        if (refreshed) {
            csrfTokenPromise = null;
            response = await authenticatedFetch(url, {...options, headers: await requestHeaders(options, unsafe)});
        } else { redirectToLogin(); return response; }
    }
    if (response.status === 401) {
        redirectToLogin();
    }
    const requestMethod = (options.method || "GET").toUpperCase();
    const errorPayload = response.status === 403 ? await response.clone().json().catch(() => ({})) : {};
    if (response.status === 403 && errorPayload.code === "CSRF_FAILURE" && ["PUT", "PATCH", "DELETE"].includes(requestMethod)) {
        const freshCsrf = await csrfToken(true);
        const headers = new Headers(options.headers || {});
        headers.set(freshCsrf.headerName, freshCsrf.token);
        response = await authenticatedFetch(url, {...options, headers}, true);
    }
    return response;
}

async function requestHeaders(options, unsafe) {
    const headers = new Headers(options.headers || {});
    if (unsafe) {
        const csrf = await csrfToken();
        headers.set(csrf.headerName, csrf.token);
    }
    return headers;
}

function readXsrfCookie() {
    const entry = document.cookie.split("; ").find(item => item.startsWith("XSRF-TOKEN="));
    return entry ? decodeURIComponent(entry.substring("XSRF-TOKEN=".length)) : null;
}

async function authenticatedFetch(url, options = {}) {
    return fetch(url, options);
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

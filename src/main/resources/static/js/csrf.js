let csrfTokenPromise;

async function csrfFetch(url, options = {}) {
    const method = (options.method || "GET").toUpperCase();
    if (["GET", "HEAD", "OPTIONS", "TRACE"].includes(method)) {
        return fetch(url, options);
    }

    if (!csrfTokenPromise) {
        csrfTokenPromise = fetch("/api/csrf")
            .then(response => {
                if (!response.ok) {
                    throw new Error("Unable to obtain CSRF token");
                }
                return response.json();
            });
    }

    const csrf = await csrfTokenPromise;
    const headers = new Headers(options.headers || {});
    headers.set(csrf.headerName, csrf.token);

    return fetch(url, {...options, headers});
}

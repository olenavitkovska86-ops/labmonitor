function renderBreadcrumbs(items) {
    const breadcrumbs = document.querySelector("#breadcrumbs");
    breadcrumbs.replaceChildren();

    items.forEach((item, index) => {
        if (index > 0) {
            const separator = document.createElement("span");
            separator.className = "breadcrumb-separator";
            separator.textContent = "›";
            separator.setAttribute("aria-hidden", "true");
            breadcrumbs.append(separator);
        }

        if (item.href) {
            const link = document.createElement("a");
            link.className = "breadcrumb-link";
            link.href = item.href;
            link.textContent = item.label;
            breadcrumbs.append(link);
            return;
        }

        const current = document.createElement("span");
        current.className = "breadcrumb-current";
        current.textContent = item.label;
        current.setAttribute("aria-current", "page");
        breadcrumbs.append(current);
    });
}

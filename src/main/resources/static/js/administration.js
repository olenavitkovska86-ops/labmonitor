const demoUsers = [
    {id: 1, name: "Anna Lind", organization: "Lund Research Center", role: "LIMITED_EMPLOYEE", organizationWide: false,
        labs: [{id: 11, name: "Chemistry Lab", whole: false, rooms: [{id: 111, name: "Room 101", selected: true}, {id: 112, name: "Room 102", selected: true}, {id: 113, name: "Storage", selected: false}]},
            {id: 12, name: "Biology Lab", whole: false, rooms: [{id: 121, name: "Freezer Room", selected: false}, {id: 122, name: "Preparation Room", selected: false}]}]},
    {id: 2, name: "Maria Berg", organization: "Lund Research Center", role: "LAB_ADMIN", organizationWide: false,
        labs: [{id: 11, name: "Chemistry Lab", whole: true, rooms: [{id: 111, name: "Room 101", selected: false}, {id: 112, name: "Room 102", selected: false}, {id: 113, name: "Storage", selected: false}]},
            {id: 12, name: "Biology Lab", whole: false, rooms: [{id: 121, name: "Freezer Room", selected: false}, {id: 122, name: "Preparation Room", selected: false}]}]},
    {id: 3, name: "Erik Nilsson", organization: "Malmö Lab Group", role: "LIMITED_EMPLOYEE", organizationWide: false,
        labs: [{id: 21, name: "Biology Lab", whole: false, rooms: [{id: 211, name: "Freezer Room", selected: true}, {id: 212, name: "Culture Room", selected: false}]}]}
];

const organizationTemplates = new Map();
for (const user of demoUsers) {
    if (!organizationTemplates.has(user.organization)) {
        organizationTemplates.set(user.organization, structuredClone(user.labs));
    }
}

const userSelect = document.querySelector("#prototype-user");
const organizationSelect = document.querySelector("#prototype-organization");
const roleSelect = document.querySelector("#prototype-role");
const organizationWide = document.querySelector("#organization-wide");
const tree = document.querySelector("#responsibility-tree");
const summary = document.querySelector("#responsibility-summary");
const message = document.querySelector("#responsibility-message");
let selectedUser;
let initialState;

for (const user of demoUsers) userSelect.append(new Option(user.name, user.id));
userSelect.addEventListener("change", loadUser);
organizationSelect.addEventListener("change", changeOrganization);
roleSelect.addEventListener("change", updateModel);
organizationWide.addEventListener("change", updateModel);
document.querySelector("#save-responsibility").addEventListener("click", savePreview);
document.querySelector("#reset-responsibility").addEventListener("click", resetPreview);

function loadUser() {
    selectedUser = demoUsers.find(user => user.id === Number(userSelect.value));
    initialState = JSON.stringify(selectedUser);
    organizationSelect.replaceChildren(...[...organizationTemplates.keys()].map(name => new Option(name, name)));
    organizationSelect.value = selectedUser.organization;
    roleSelect.value = selectedUser.role;
    organizationWide.checked = selectedUser.organizationWide;
    renderTree();
    updateSummary();
    message.classList.add("hidden");
}

function changeOrganization() {
    selectedUser.organization = organizationSelect.value;
    selectedUser.organizationWide = false;
    selectedUser.labs = structuredClone(organizationTemplates.get(selectedUser.organization) || []);
    selectedUser.labs.forEach(lab => {
        lab.whole = false;
        lab.rooms.forEach(room => room.selected = false);
    });
    organizationWide.checked = false;
    initialState = JSON.stringify(selectedUser);
    renderTree();
    updateSummary();
    message.textContent = "Organization changed. Select responsibility for this membership.";
    message.classList.remove("message-error");
    message.classList.remove("hidden");
}

function renderTree() {
    tree.replaceChildren();
    for (const lab of selectedUser.labs) {
        const group = document.createElement("section");
        group.className = "responsibility-lab";
        const labOption = scopeOption(lab.name, "Whole lab", lab.whole, checked => {
            lab.whole = checked;
            if (checked) lab.rooms.forEach(room => room.selected = false);
            renderTree(); updateSummary();
        });
        labOption.classList.add("responsibility-lab-option");
        const rooms = document.createElement("div");
        rooms.className = "responsibility-rooms";
        for (const room of lab.rooms) {
            const option = scopeOption(room.name, "Room", room.selected, checked => {
                room.selected = checked; updateSummary();
            });
            option.querySelector("input").disabled = organizationWide.checked || lab.whole;
            rooms.append(option);
        }
        labOption.querySelector("input").disabled = organizationWide.checked;
        group.append(labOption, rooms);
        tree.append(group);
    }
}

function scopeOption(title, description, checked, onChange) {
    const label = document.createElement("label");
    label.className = "scope-option";
    const input = document.createElement("input");
    input.type = "checkbox"; input.checked = checked;
    input.addEventListener("change", () => onChange(input.checked));
    const text = document.createElement("span");
    const strong = document.createElement("strong"); strong.textContent = title;
    const small = document.createElement("small"); small.textContent = description;
    text.append(strong, small); label.append(input, text); return label;
}

function updateModel() {
    selectedUser.role = roleSelect.value;
    selectedUser.organizationWide = organizationWide.checked;
    if (selectedUser.organizationWide) selectedUser.labs.forEach(lab => {
        lab.whole = false; lab.rooms.forEach(room => room.selected = false);
    });
    renderTree(); updateSummary(); message.classList.add("hidden");
}

function updateSummary() {
    if (selectedUser.organizationWide) {
        summary.textContent = `${selectedUser.organization} — whole organization`;
        return;
    }
    const scopes = selectedUser.labs.flatMap(lab => lab.whole ? [lab.name] : lab.rooms.filter(room => room.selected).map(room => `${lab.name} / ${room.name}`));
    summary.textContent = scopes.length ? scopes.join(" · ") : "No responsibility selected";
}

function savePreview() {
    initialState = JSON.stringify(selectedUser);
    message.textContent = "Responsibility updated in browser memory. Backend persistence is not connected.";
    message.classList.remove("hidden");
}

function resetPreview() {
    Object.assign(selectedUser, JSON.parse(initialState));
    roleSelect.value = selectedUser.role;
    organizationWide.checked = selectedUser.organizationWide;
    renderTree(); updateSummary(); message.classList.add("hidden");
}

loadUser();

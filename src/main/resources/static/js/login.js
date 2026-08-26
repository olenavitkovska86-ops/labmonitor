const loginForm = document.querySelector("#login-form");

loginForm.addEventListener("submit", async event => {
    event.preventDefault();

    const email = document.querySelector("#email").value;
    const password = document.querySelector("#password").value;

    try {
        const response = await fetch("/auth/login", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({email, password})
        });

        if (response.ok) {
            window.location.href = "/";
        } else {
            const error = await response.json();
            alert(error.message || "Login failed");
        }
    } catch (error) {
        alert("Login failed. Please try again.");
    }
});

const GRID_MAX_X = 30;
const GRID_MAX_Y = 15;
const POLL_INTERVAL_MS = 1000;

const gridEl = document.getElementById("grid");
const fleetBodyEl = document.getElementById("fleet-body");
const vehicleSelectEl = document.getElementById("vehicle-select");
const dispatchForm = document.getElementById("dispatch-form");
const xInput = document.getElementById("x-input");
const yInput = document.getElementById("y-input");
const errorEl = document.getElementById("error");

let knownIds = [];

async function poll() {
    try {
        const response = await fetch("/api/vehicles");
        const vehicles = await response.json();
        render(vehicles);
    } catch (e) {
        errorEl.textContent = "Could not reach the server.";
    }
}

function render(vehicles) {
    const ids = Object.keys(vehicles).sort();

    if (ids.join(",") !== knownIds.join(",")) {
        knownIds = ids;
        vehicleSelectEl.innerHTML = "";
        ids.forEach((id) => {
            const option = document.createElement("option");
            option.value = id;
            option.textContent = id;
            vehicleSelectEl.appendChild(option);
        });
    }

    fleetBodyEl.innerHTML = "";
    ids.forEach((id) => {
        const { x, y } = vehicles[id];
        const row = document.createElement("tr");
        row.innerHTML = `<td>${id}</td><td>${x}</td><td>${y}</td>`;
        fleetBodyEl.appendChild(row);
    });

    renderGrid(vehicles, ids);
}

function renderGrid(vehicles, ids) {
    const existingDots = new Map(
        [...gridEl.querySelectorAll(".vehicle-dot")].map((el) => [el.dataset.id, el])
    );

    ids.forEach((id) => {
        const { x, y } = vehicles[id];
        let dot = existingDots.get(id);

        if (!dot) {
            dot = document.createElement("div");
            dot.className = "vehicle-dot";
            dot.dataset.id = id;
            dot.textContent = id;
            gridEl.appendChild(dot);
        } else {
            existingDots.delete(id);
        }

        dot.style.left = `${(x / GRID_MAX_X) * 100}%`;
        dot.style.top = `${(1 - y / GRID_MAX_Y) * 100}%`;
    });

    existingDots.forEach((dot) => dot.remove());
}

async function dispatchCorrection(event) {
    event.preventDefault();
    errorEl.textContent = "";

    const id = vehicleSelectEl.value;
    const x = Number(xInput.value);
    const y = Number(yInput.value);

    const response = await fetch(`/api/vehicles/${encodeURIComponent(id)}/location`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ x, y }),
    });

    if (!response.ok) {
        const problem = await response.json().catch(() => ({}));
        errorEl.textContent = problem.detail || "Could not dispatch correction.";
        return;
    }

    xInput.value = "";
    yInput.value = "";
    poll();
}

dispatchForm.addEventListener("submit", dispatchCorrection);

poll();
setInterval(poll, POLL_INTERVAL_MS);

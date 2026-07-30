const GRID_MAX_X = 30;
const GRID_MAX_Y = 15;
const POLL_INTERVAL_MS = 1000;

// Maps the tracker's abstract (x, y) grid onto a real bounding box over
// Lagos, so the fleet moves over real streets instead of a blank grid.
// x=0..30 -> west..east (Mainland to Lekki), y=0..15 -> south..north (Island to Ikeja).
const LAGOS_BOUNDS = {
    lngMin: 3.31, lngMax: 3.47,
    latMin: 6.42, latMax: 6.62,
};

function gridToLngLat(x, y) {
    const lng = LAGOS_BOUNDS.lngMin + (x / GRID_MAX_X) * (LAGOS_BOUNDS.lngMax - LAGOS_BOUNDS.lngMin);
    const lat = LAGOS_BOUNDS.latMin + (y / GRID_MAX_Y) * (LAGOS_BOUNDS.latMax - LAGOS_BOUNDS.latMin);
    return [lng, lat];
}

const fleetBodyEl = document.getElementById("fleet-body");
const vehicleSelectEl = document.getElementById("vehicle-select");
const dispatchForm = document.getElementById("dispatch-form");
const xInput = document.getElementById("x-input");
const yInput = document.getElementById("y-input");
const errorEl = document.getElementById("error");
const liveDotEl = document.getElementById("live-dot");
const lastUpdateEl = document.getElementById("last-update");
const dockEl = document.getElementById("dock");
const dockToggleEl = document.getElementById("dock-toggle");

// Wire UI chrome that doesn't depend on Mapbox first, so a map/network
// failure below can never prevent these from working.
dockToggleEl.addEventListener("click", () => {
    const collapsed = dockEl.classList.toggle("collapsed");
    dockToggleEl.setAttribute("aria-expanded", String(!collapsed));
    dockToggleEl.textContent = collapsed ? "Fleet ▸" : "Fleet ▾";
});

document.querySelectorAll(".style-switch button").forEach((button) => {
    button.addEventListener("click", () => {
        document.querySelectorAll(".style-switch button").forEach((b) => b.classList.remove("active"));
        button.classList.add("active");
        if (map) {
            map.setStyle(`mapbox://styles/mapbox/${button.dataset.style}`);
        }
    });
});

dispatchForm.addEventListener("submit", dispatchCorrection);

let map = null;
const markers = new Map();

async function initMap() {
    try {
        const configResponse = await fetch("api/config");
        const config = await configResponse.json();

        if (!config.mapboxToken) {
            errorEl.textContent = "Map disabled: MAPBOX_TOKEN is not set on the server.";
            poll();
            setInterval(poll, POLL_INTERVAL_MS);
            return;
        }

        mapboxgl.accessToken = config.mapboxToken;

        map = new mapboxgl.Map({
            container: "map",
            style: "mapbox://styles/mapbox/dark-v11",
            center: gridToLngLat(15, 7.5),
            zoom: 11.2,
            attributionControl: false,
        });
        map.addControl(new mapboxgl.AttributionControl({ compact: true }));
        map.addControl(new mapboxgl.NavigationControl({ showCompass: false }), "top-right");
        map.on("load", () => {
            poll();
            setInterval(poll, POLL_INTERVAL_MS);
        });
    } catch (e) {
        errorEl.textContent = "Map failed to load: " + e.message;
        poll();
        setInterval(poll, POLL_INTERVAL_MS);
    }
}

initMap();

let knownIds = [];

async function poll() {
    try {
        const response = await fetch("api/vehicles");
        const vehicles = await response.json();
        render(vehicles);
        lastUpdateEl.textContent = new Date().toLocaleTimeString();
        errorEl.textContent = "";
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

    if (map) {
        renderMarkers(vehicles, ids);
    }
}

function renderMarkers(vehicles, ids) {
    ids.forEach((id) => {
        const { x, y } = vehicles[id];
        const [lng, lat] = gridToLngLat(x, y);

        let marker = markers.get(id);
        if (!marker) {
            const el = document.createElement("div");
            el.className = "vehicle-marker";
            el.innerHTML = `<div class="pip"></div><div class="label">${id}</div>`;

            marker = new mapboxgl.Marker({ element: el })
                .setLngLat([lng, lat])
                .setPopup(new mapboxgl.Popup({ offset: 18 }).setHTML(
                    `<strong>${id}</strong><br>grid (${x}, ${y})`
                ))
                .addTo(map);
            markers.set(id, marker);
        } else {
            marker.setLngLat([lng, lat]);
            const popup = marker.getPopup();
            if (popup) popup.setHTML(`<strong>${id}</strong><br>grid (${x}, ${y})`);
        }
    });

    for (const [id, marker] of markers) {
        if (!ids.includes(id)) {
            marker.remove();
            markers.delete(id);
        }
    }
}

async function dispatchCorrection(event) {
    event.preventDefault();
    errorEl.textContent = "";

    const id = vehicleSelectEl.value;
    const x = Number(xInput.value);
    const y = Number(yInput.value);

    const response = await fetch(`api/vehicles/${encodeURIComponent(id)}/location`, {
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

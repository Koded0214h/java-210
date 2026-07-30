const clockEl = document.getElementById("clock");

function tick() {
    clockEl.textContent = new Date().toLocaleTimeString("en-GB", { hour12: false });
}

tick();
setInterval(tick, 1000);

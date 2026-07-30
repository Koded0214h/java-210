function renderChart(result, chartElId, tableElId) {
    const chartEl = document.getElementById(chartElId);
    const tableEl = document.getElementById(tableElId);
    const maxCount = Math.max(...result.rankedTags.map((t) => t.documentCount), 1);

    chartEl.innerHTML = "";
    result.rankedTags.forEach((entry, index) => {
        const row = document.createElement("div");
        row.className = "bar-row";

        const rank = document.createElement("div");
        rank.className = "bar-rank";
        rank.textContent = String(index + 1).padStart(2, "0");
        row.appendChild(rank);

        const label = document.createElement("div");
        label.className = "bar-label";
        label.textContent = entry.tag;
        row.appendChild(label);

        const track = document.createElement("div");
        track.className = "bar-track";

        const fill = document.createElement("div");
        fill.className = "bar-fill";
        fill.style.width = `${(entry.documentCount / maxCount) * 100}%`;
        track.appendChild(fill);

        const value = document.createElement("div");
        value.className = "bar-value";
        value.textContent = entry.documentCount;
        track.appendChild(value);

        row.appendChild(track);
        chartEl.appendChild(row);
    });

    tableEl.innerHTML = "";
    const table = document.createElement("table");
    table.innerHTML = `
        <thead><tr><th>Rank</th><th>Tag</th><th>Count</th></tr></thead>
        <tbody>
            ${result.rankedTags.map((e, i) => `<tr><td>${i + 1}</td><td>${e.tag}</td><td>${e.documentCount}</td></tr>`).join("")}
        </tbody>
    `;
    tableEl.appendChild(table);
}

function randomBetween(min, max) {
    return min + Math.random() * (max - min);
}

const MAX_LANES = 16;

// Renders one lane per real SERP result and lets each resolve to done/failed
// on its own randomized timer, so the reveal itself reads as concurrent
// (workers finishing in no particular order) rather than a neat sequence.
// Capped and sorted hits-first since a low-yield query can return dozens of
// results with no usable text — those still count toward resultsFailed, but
// aren't worth a lane each.
function animateLanes(lanesEl, allResults) {
    lanesEl.innerHTML = "";
    lanesEl.hidden = false;

    const sorted = [...allResults].sort((a, b) => (b.used ? 1 : 0) - (a.used ? 1 : 0));
    const results = sorted.slice(0, MAX_LANES);
    const omitted = sorted.length - results.length;

    const settlePromises = results.map((result, index) => {
        const lane = document.createElement("div");
        lane.className = "lane queued";
        lane.style.animationDelay = `${index * 25}ms`;
        lane.innerHTML = `
            <div class="lane-id">T${index + 1}</div>
            <div class="lane-title">${result.title || result.url || "(untitled)"}</div>
            <div class="lane-status">queued</div>
        `;
        lanesEl.appendChild(lane);

        const statusEl = lane.querySelector(".lane-status");
        const toWorking = randomBetween(80, 420);
        const toSettled = toWorking + randomBetween(280, 900);

        return new Promise((resolve) => {
            setTimeout(() => {
                lane.className = "lane working";
                statusEl.textContent = "fetching";
            }, toWorking);

            setTimeout(() => {
                if (result.used) {
                    lane.className = "lane done";
                    statusEl.textContent = `${result.tagCount} tag${result.tagCount === 1 ? "" : "s"}`;
                } else {
                    lane.className = "lane failed";
                    statusEl.textContent = "no match";
                }
                resolve();
            }, toSettled);
        });
    });

    if (omitted > 0) {
        const note = document.createElement("p");
        note.className = "lanes-note";
        note.textContent = `+${omitted} more result${omitted === 1 ? "" : "s"} not shown`;
        lanesEl.appendChild(note);
    }

    return Promise.all(settlePromises);
}

async function runSearch(endpoint, query, chartElId, tableElId, statusElId, lanesElId, noun) {
    const statusEl = document.getElementById(statusElId);
    const chartEl = document.getElementById(chartElId);
    const lanesEl = document.getElementById(lanesElId);

    chartEl.innerHTML = "";
    lanesEl.hidden = true;
    statusEl.textContent = `Searching ${noun}...`;

    try {
        const response = await fetch(endpoint, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ query }),
        });
        const body = await response.json();

        if (!response.ok) {
            statusEl.textContent = `Error: ${body.error || response.statusText}`;
            return;
        }

        const { resultsRequested, resultsUsed, resultsFailed, results, aggregation } = body;
        statusEl.textContent = `${resultsRequested} result${resultsRequested === 1 ? "" : "s"} found · dispatching ${resultsRequested} worker thread${resultsRequested === 1 ? "" : "s"}...`;

        await animateLanes(lanesEl, results);

        renderChart(aggregation, chartElId, tableElId);
        statusEl.textContent =
            `${resultsUsed}/${resultsRequested} results used (${resultsFailed} no match) · ` +
            `${aggregation.rankedTags.length} distinct tags across ${aggregation.documentCount} sources`;
    } catch (err) {
        statusEl.textContent = `Request failed: ${err.message}`;
    }
}

document.querySelectorAll(".table-toggle").forEach((button) => {
    button.addEventListener("click", () => {
        const target = button.dataset.target;
        const chart = document.getElementById(`${target}-chart`);
        const table = document.getElementById(`${target}-table`);
        const showingTable = !table.hidden;

        table.hidden = showingTable;
        chart.hidden = !showingTable;
        button.textContent = showingTable ? "Table" : "Chart";
    });
});

document.getElementById("crime-form").addEventListener("submit", (e) => {
    e.preventDefault();
    const query = document.getElementById("crime-query").value.trim();
    if (!query) return;
    runSearch("api/search/crime-features", query, "crime-chart", "crime-table", "crime-status", "crime-lanes", "crime-reporting papers");
});

document.getElementById("dl-form").addEventListener("submit", (e) => {
    e.preventDefault();
    const query = document.getElementById("dl-query").value.trim();
    if (!query) return;
    runSearch("api/search/dl-headings", query, "dl-chart", "dl-table", "dl-status", "dl-lanes", "deep-learning papers");
});

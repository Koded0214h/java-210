async function loadPanel(endpoint, seriesClass, chartElId, tableElId, captionElId, captionNoun) {
    const response = await fetch(endpoint);
    const result = await response.json();

    const chartEl = document.getElementById(chartElId);
    const tableEl = document.getElementById(tableElId);
    const captionEl = document.getElementById(captionElId);

    captionEl.textContent =
        `${result.documentCount} ${captionNoun} · ${result.rankedTags.length} distinct tags, ranked by document count`;

    const maxCount = Math.max(...result.rankedTags.map((t) => t.documentCount), 1);

    chartEl.innerHTML = "";
    result.rankedTags.forEach((entry) => {
        const row = document.createElement("div");
        row.className = "bar-row";

        const label = document.createElement("div");
        label.className = "bar-label";
        label.textContent = entry.tag;
        row.appendChild(label);

        const track = document.createElement("div");
        track.className = "bar-track";

        const fill = document.createElement("div");
        fill.className = `bar-fill ${seriesClass}`;
        fill.style.width = `${(entry.documentCount / maxCount) * 100}%`;
        track.appendChild(fill);

        const value = document.createElement("div");
        value.className = "bar-value";
        value.textContent = entry.documentCount;
        track.appendChild(value);

        const tooltip = document.createElement("div");
        tooltip.className = "tooltip";
        tooltip.textContent = `${entry.tag}: ${entry.documentCount}`;
        track.appendChild(tooltip);

        row.appendChild(track);
        chartEl.appendChild(row);
    });

    tableEl.innerHTML = "";
    const table = document.createElement("table");
    table.innerHTML = `
        <thead><tr><th>Tag</th><th>Document count</th></tr></thead>
        <tbody>
            ${result.rankedTags.map((e) => `<tr><td>${e.tag}</td><td>${e.documentCount}</td></tr>`).join("")}
        </tbody>
    `;
    tableEl.appendChild(table);
}

document.querySelectorAll(".table-toggle").forEach((button) => {
    button.addEventListener("click", () => {
        const target = button.dataset.target;
        const chart = document.getElementById(`${target}-chart`);
        const table = document.getElementById(`${target}-table`);
        const showingTable = !table.hidden;

        table.hidden = showingTable;
        chart.hidden = !showingTable;
        button.textContent = showingTable ? "Table view" : "Chart view";
    });
});

loadPanel("/api/crime-features", "crime", "crime-chart", "crime-table", "crime-caption", "systems");
loadPanel("/api/dl-headings", "dl", "dl-chart", "dl-table", "dl-caption", "papers");

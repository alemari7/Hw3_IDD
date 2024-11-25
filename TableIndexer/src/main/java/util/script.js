let docList = [];
let scoreList = [];
let count = 0;

// Funzione per caricare i dati dal backend
async function loadResults(query, fields) {
    docList = [];
    scoreList = [];

    const fieldsParam = Array.isArray(fields) ? fields.join(",") : fields;
    const url = `http://localhost:8000/search?query=${encodeURIComponent(query)}&fields=${encodeURIComponent(fieldsParam)}`;
    
    try {
        const response = await fetch(url);
        const data = await response.json();

        if (data.message) {
            alert(data.message);
            return;
        }

        const resultsContainer = document.getElementById("resultsContainer");

        // Pulisci i risultati precedenti
        resultsContainer.innerHTML = "";

        // Aggiungi nuovi risultati
        data.forEach((item) => {
            docList.push(item.DocId);
            scoreList.push(item.score);

            const resultDiv = document.createElement("div");
            resultDiv.classList.add("result-container");

            resultDiv.innerHTML = `
                <h2>DocId: ${item.DocId}</h2>
                <p><strong>Score:</strong> ${item.score || "N/A"}</p>
                <p><strong>Caption:</strong> ${item.caption || "N/A"}</p>
                <p><strong>Footnotes:</strong> ${item.footnotes || "N/A"}</p>
                <p><strong>References:</strong> ${item.references || "N/A"}</p>
                <div class="table-content">
                    <strong>Table Content:</strong><br>
                    ${item.table || "N/A"}
                </div>
                <p><strong>Source File:</strong> ${item.source_file || "N/A"}</p>
            `;

            resultsContainer.appendChild(resultDiv);
        });
    } catch (error) {
        console.error("Errore durante il recupero dei dati:", error);
        alert("Si è verificato un errore durante il recupero dei risultati.");
    }

    count++;
}

// Funzione per calcolare le metriche
async function submitMetrics() {
    const relevantDocsInput = document.getElementById("relevantDocsInput").value;

    const relevantDocsIndices = relevantDocsInput
        .split(",")
        .map((id) => id.trim())
        .filter((id) => !isNaN(id))
        .map((index) => docList[index]);

    if (relevantDocsIndices.length === 0) {
        alert("Per favore, inserisci almeno un indice di documento valido.");
        return;
    }

    const relevantDocsString = relevantDocsIndices.join(",");
    const docListString = docList.join(",");
    const scoreListString = scoreList.join(",");

    const url = `http://localhost:8000/metrics?relevantDocs=${encodeURIComponent(
        relevantDocsString
    )}&score=${encodeURIComponent(scoreListString)}&count=${count}&docList=${encodeURIComponent(
        docListString
    )}`;

    try {
        const response = await fetch(url);
        const data = await response.json();

        const metricsContainer = document.getElementById("metricsContainer");
        metricsContainer.innerHTML = `
            <p><strong>MRR (Mean Reciprocal Rank):</strong> ${data.mrr}</p>
            <p><strong>NDCG (Normalized Discounted Cumulative Gain):</strong> ${data.ndcg}</p>
            <p><strong>Mean MRR:</strong> ${data.mean_mrr}</p>
            <p><strong>Mean NDCG:</strong> ${data.mean_ndcg}</p>
        `;
    } catch (error) {
        console.error("Errore durante il calcolo delle metriche:", error);
        alert("Si è verificato un errore durante il calcolo delle metriche.");
    }
}

// Funzione per gestire il submit del modulo di ricerca
function submitSearch() {
    const query = document.getElementById("queryInput").value;
    const searchType = document.getElementById("searchType").value;

    if (!query.trim()) {
        alert("La query non può essere vuota.");
        return;
    }

    // Raccogli i campi selezionati
    const selectedFields = Array.from(
        document.querySelectorAll(".field-checkbox:checked")
    ).map((checkbox) => checkbox.value);

    if (selectedFields.length === 0) {
        alert("Per favore, seleziona almeno un campo per la ricerca.");
        return;
    }

    // Invia la ricerca
    loadResults(query, selectedFields);
}

// Funzione per gestire la visibilità dei campi di selezione
function toggleFieldSelection() {
    // Non nascondere i campi in nessun caso, devono essere visibili sempre
    const fieldSelection = document.getElementById("fieldSelection");
    fieldSelection.style.display = "block";
}

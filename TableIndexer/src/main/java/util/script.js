let docList = [];
let scoreList = [];
let count = 0;

// Funzione per caricare i dati dal backend
async function loadResults(query, field) {
    docList = [];
    scoreList = [];

    const url = `http://localhost:8000/search?query=${encodeURIComponent(query)}&field=${encodeURIComponent(field)}`;
    try {
        const response = await fetch(url);
        const data = await response.json();

        // Controlla se la risposta è vuota o ha errore
        if (data.message) {
            alert(data.message);
            return;
        }

        // Ottieni il container dei risultati
        const resultsContainer = document.getElementById('resultsContainer');

        // Svuota i risultati precedenti
        resultsContainer.innerHTML = '';

        // Aggiungi i risultati come div separati
        data.forEach(item => {
            docList = docList.concat(item.DocId);
            scoreList = scoreList.concat(item.score);

            const resultDiv = document.createElement('div');
            resultDiv.classList.add('result-container');

            // Crea il contenuto per ogni risultato
            resultDiv.innerHTML = `
                <h2>DocId: ${item.DocId}</h2>
                <p><strong>Score:</strong> ${item.score || 'N/A'}</p>
                <p><strong>Caption:</strong> ${item.caption || 'N/A'}</p>
                <p><strong>Footnotes:</strong> ${item.footnotes || 'N/A'}</p>
                <p><strong>References:</strong> ${item.references || 'N/A'}</p>
                <div class="table-content">
                    <strong>Table Content:</strong><br>
                    ${item.table ? item.table : 'N/A'}
                </div>
                <p><strong>Source File:</strong> ${item.source_file || 'N/A'}</p>
            `;

            // Aggiungi il risultato al container principale
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
    const relevantDocsInput = document.getElementById('relevantDocsInput').value;

    const relevantDocsInputRaw = relevantDocsInput.split(',').map(id => id.trim()).filter(id => !isNaN(id));
    if (relevantDocsInputRaw.length === 0) {
        alert('Per favore, inserisci almeno un indice di documento e un punteggio.');
        return;
    }

    const relevantDocsIndices = relevantDocsInputRaw.map(index => docList[index]);

    const relevantDocsString = relevantDocsIndices.join(',');
    const docListString = docList.join(',');
    const scoreListString = scoreList.join(',');

    console.log("relevantDocs: ", relevantDocsString);
    console.log("docList: ", docListString);
    console.log("scoreList: ", scoreListString);
    console.log("count: ", count);

    const url = `http://localhost:8000/metrics?relevantDocs=${encodeURIComponent(relevantDocsString)}&score=${encodeURIComponent(scoreListString)}&count=${count}&docList=${encodeURIComponent(docListString)}`;

    try {
        const response = await fetch(url);
        const data = await response.json();

        const metricsContainer = document.getElementById('metricsContainer');
        metricsContainer.innerHTML = '';

        const metricsHtml = `
            <p><strong>MRR (Mean Reciprocal Rank):</strong> ${data.mrr}</p>
            <p><strong>NDCG (Normalized Discounted Cumulative Gain):</strong> ${data.ndcg}</p>
            <p><strong>Mean MRR:</strong> ${data.mean_mrr}</p>
            <p><strong>Mean NDCG:</strong> ${data.mean_ndcg}</p>
        `;
        metricsContainer.innerHTML = metricsHtml;
    } catch (error) {
        console.error("Errore durante il calcolo delle metriche:", error);
        alert("Si è verificato un errore durante il calcolo delle metriche.");
    }
}

// Funzione per gestire il submit del modulo di ricerca
function submitSearch() {
    const query = document.getElementById('queryInput').value;
    const field = document.getElementById('fieldSelect').value;
    loadResults(query, field);
}

// Chiamata alla funzione di ricerca con valori di default
loadResults('Conv-TasNet', 'all');

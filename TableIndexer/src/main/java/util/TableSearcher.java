package util;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.ScoreDoc;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TableSearcher {
    private IndexSearcher searcher;
    private QueryParser captionParser;
    private QueryParser referencesParser;
    private QueryParser footnotesParser;

    // Metodo per effettuare il parsing di un documento HTML e ottenere il testo
    public static String parseHtmlToPlainText(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        // Usa Jsoup per effettuare il parsing e rimuovere i tag HTML
        return Jsoup.parse(html).text();
    }

    public TableSearcher(String indexDirectoryPath) throws Exception {
        // Inizializza il lettore dell'indice e il cercatore
        DirectoryReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexDirectoryPath)));
        searcher = new IndexSearcher(reader);

        // Crea i parser per i vari campi
        captionParser = new QueryParser("caption", new StandardAnalyzer());
        referencesParser = new QueryParser("references", new StandardAnalyzer());
        footnotesParser = new QueryParser("footnotes", new StandardAnalyzer());
    }

    // Metodo per cercare nel campo 'caption'
    public TopDocs searchCaption(String queryStr) throws Exception {
        Query query = captionParser.parse(queryStr);
        return searcher.search(query, 10);
    }

    // Metodo per cercare nel campo 'references'
    public TopDocs searchReferences(String queryStr) throws Exception {
        Query query = referencesParser.parse(queryStr);
        return searcher.search(query, 10);
    }

    // Metodo per cercare nel campo 'footnotes'
    public TopDocs searchFootnotes(String queryStr) throws Exception {
        Query query = footnotesParser.parse(queryStr);
        return searcher.search(query, 10);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            // Percorso dell'indice
            String indexDirectory = "indexDir";

            // Creazione dell'oggetto TableSearcher
            TableSearcher tableSearcher = new TableSearcher(indexDirectory);

            boolean continueSearching = true;
            while (continueSearching) {
                // Chiedi all'utente su quale campo vuole cercare
                System.out.println("Scegli il campo su cui fare la ricerca:");
                System.out.println("1. Caption");
                System.out.println("2. References");
                System.out.println("3. Footnotes");

                int choice = scanner.nextInt();
                scanner.nextLine();  // Consuma la newline lasciata da nextInt()

                // Chiedi la query all'utente
                System.out.print("Inserisci la query di ricerca: ");
                String query = scanner.nextLine();

                // Esegui la ricerca in base alla scelta dell'utente
                TopDocs results = null;
                switch (choice) {
                    case 1:
                        results = tableSearcher.searchCaption(query);
                        break;
                    case 2:
                        results = tableSearcher.searchReferences(query);
                        break;
                    case 3:
                        results = tableSearcher.searchFootnotes(query);
                        break;
                    default:
                        System.out.println("Scelta non valida.");
                        continue;  // Ritorna al ciclo per una nuova ricerca
                }

                // Stampa i risultati con il punteggio e il documento di origine
                System.out.println("Trovati " + results.totalHits + " risultati.");
                for (ScoreDoc scoreDoc : results.scoreDocs) {
                    int docId = scoreDoc.doc;

                    // Usa storedFields() per ottenere il documento
                    Document doc = tableSearcher.searcher.storedFields().document(docId);

                    float score = scoreDoc.score; // Ottieni il punteggio di pertinenza
                    String caption = doc.get("caption");
                    String footnotes = doc.get("footnotes");
                    String references = doc.get("references");
                    String table = doc.get("table");
                    String sourceFile = doc.get("source_file");

                    String parsedTable = parseHtmlToPlainText(table);       // effettua il parsing della tabella HTML

                    // Mostra solo il testo dei campi
                    System.out.println("Documento ID: " + docId + " | Score: " + score);
                    System.out.println("Caption: " + (caption != null ? caption : "N/A"));
                    System.out.println("Footnotes: " + (footnotes != null ? footnotes : "N/A"));
                    System.out.println("References: " + (references != null ? references : "N/A"));
                    System.out.println("Table: " + (parsedTable != null ? parsedTable : "N/A"));  // Mostra il testo della tabella
                    System.out.println("Fonte: " + sourceFile + "\n");
                }

                // Richiedi gli indici dei documenti rilevanti all'utente
                System.out.print("Inserisci gli indici dei documenti rilevanti (separati da virgola): ");
                String[] relevantDocIndicesInput = scanner.nextLine().split(",");
                List<Integer> relevantDocIndices = new ArrayList<>();
                for (String idx : relevantDocIndicesInput) {
                    relevantDocIndices.add(Integer.parseInt(idx.trim()));
                }

                // Valutazione delle metriche
                Evaluation.evaluateSearchResults(results, relevantDocIndices);

                // Chiedi all'utente se vuole continuare la ricerca
                System.out.print("Vuoi fare un'altra ricerca? (s/n): ");
                String continueChoice = scanner.nextLine().trim().toLowerCase();
                if (!continueChoice.equals("s")) {
                    continueSearching = false;  // Esci dal ciclo
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}

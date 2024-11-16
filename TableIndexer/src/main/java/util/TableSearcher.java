package util;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.ScoreDoc;

import java.nio.file.Paths;
import java.util.Scanner;

public class TableSearcher {
    private IndexSearcher searcher;
    private QueryParser captionParser;
    private QueryParser paragraphsParser;
    private QueryParser footnotesParser;

    public TableSearcher(String indexDirectoryPath) throws Exception {
        // Inizializza il lettore dell'indice e il cercatore
        DirectoryReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexDirectoryPath)));
        searcher = new IndexSearcher(reader);

        // Crea i parser per i vari campi
        captionParser = new QueryParser("caption", new StandardAnalyzer());
        paragraphsParser = new QueryParser("paragraphs", new StandardAnalyzer());
        footnotesParser = new QueryParser("footnotes", new StandardAnalyzer());
    }

    // Metodo per cercare nel campo 'caption'
    public TopDocs searchCaption(String queryStr) throws Exception {
        Query query = captionParser.parse(queryStr);
        return searcher.search(query, 10);
    }

    // Metodo per cercare nel campo 'paragraphs'
    public TopDocs searchParagraphs(String queryStr) throws Exception {
        Query query = paragraphsParser.parse(queryStr);
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

            // Chiedi all'utente su quale campo vuole cercare
            System.out.println("Scegli il campo su cui fare la ricerca:");
            System.out.println("1. Caption");
            System.out.println("2. Paragraphs");
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
                    results = tableSearcher.searchParagraphs(query);
                    break;
                case 3:
                    results = tableSearcher.searchFootnotes(query);
                    break;
                default:
                    System.out.println("Scelta non valida.");
                    return;
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
                String reference = doc.get("reference");
                String table = doc.get("table");
                String sourceFile = doc.get("source_file");

                // Mostra solo il testo dei campi
                System.out.println("Documento ID: " + docId + " | Score: " + score);
                System.out.println("Caption: " + (caption != null ? caption : "N/A"));
                System.out.println("Footnotes: " + (footnotes != null ? footnotes : "N/A"));
                System.out.println("Reference: " + (reference != null ? reference : "N/A"));
                System.out.println("Table: " + (table != null ? table : "N/A"));  // Mostra il testo della tabella
                System.out.println("Fonte: " + sourceFile   + "\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}

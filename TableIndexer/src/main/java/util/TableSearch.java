package util;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
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

public class TableSearch {
    private IndexSearcher searcher;
    private QueryParser captionParser;
    private QueryParser referencesParser;
    private QueryParser footnotesParser;
    private MultiFieldQueryParser multiFieldQueryParser;

    public static String parseHtmlToPlainText(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        return Jsoup.parse(html).text();
    }

    public TableSearch(String indexDirectoryPath) throws Exception {
        DirectoryReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexDirectoryPath)));
        searcher = new IndexSearcher(reader);

        captionParser = new QueryParser("caption", new StandardAnalyzer());
        referencesParser = new QueryParser("references", new StandardAnalyzer());
        footnotesParser = new QueryParser("footnotes", new StandardAnalyzer());
        
        // Inizializza il MultiFieldQueryParser per i campi desiderati
        String[] fields = {"caption", "references", "footnotes"};
        multiFieldQueryParser = new MultiFieldQueryParser(fields, new StandardAnalyzer());
    }

    public TopDocs searchCaption(String queryStr) throws Exception {
        Query query = captionParser.parse(queryStr);
        return searcher.search(query, 10);
    }

    public TopDocs searchReferences(String queryStr) throws Exception {
        Query query = referencesParser.parse(queryStr);
        return searcher.search(query, 10);
    }

    public TopDocs searchFootnotes(String queryStr) throws Exception {
        Query query = footnotesParser.parse(queryStr);
        return searcher.search(query, 10);
    }

    public TopDocs searchMultipleFields(String queryStr) throws Exception {
        Query query = multiFieldQueryParser.parse(queryStr);
        return searcher.search(query, 10);
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            String indexDirectory = "indexDir";
            TableSearch tableSearcher = new TableSearch(indexDirectory);

            double mean_mmr = 0.0;
            double mean_ndcg = 0.0;
            int count = 0;

            boolean continueSearching = true;
            while (continueSearching) {
                System.out.println("Scegli il campo su cui fare la ricerca:");
                System.out.println("1. Caption");
                System.out.println("2. References");
                System.out.println("3. Footnotes");
                System.out.println("4. Ricerca su più campi");

                int choice = scanner.nextInt();
                scanner.nextLine();

                System.out.print("Inserisci la query di ricerca: ");
                String query = scanner.nextLine();

                long startTime = System.currentTimeMillis();
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
                    case 4:
                        results = tableSearcher.searchMultipleFields(query);
                        break;
                    default:
                        System.out.println("Scelta non valida.");
                        continue;
                }

                long endTime = System.currentTimeMillis();
                long responseTime = endTime - startTime;

                List<Integer> docIdQueue = new ArrayList<>();

                System.out.println("Trovati " + results.totalHits + " risultati. Tempo di risposta: " + responseTime + " ms.\n");
                for (ScoreDoc scoreDoc : results.scoreDocs) {
                    int docId = scoreDoc.doc;
                    docIdQueue.add(docId);

                    Document doc = tableSearcher.searcher.storedFields().document(docId);

                    float score = scoreDoc.score;
                    String caption = doc.get("caption");
                    String footnotes = doc.get("footnotes");
                    String references = doc.get("references");
                    String table = doc.get("table");
                    String sourceFile = doc.get("source_file");

                    String parsedTable = parseHtmlToPlainText(table);

                    System.out.println("Documento ID: " + docId + " | Score: " + score);
                    System.out.println("Caption: " + (caption != null ? caption : "N/A"));
                    System.out.println("Footnotes: " + (footnotes != null ? footnotes : "N/A"));
                    System.out.println("References: " + (references != null ? references : "N/A"));
                    System.out.println("Table: " + (parsedTable != null ? parsedTable : "N/A"));
                    System.out.println("Fonte: " + sourceFile + "\n");
                }

                System.out.print("Inserisci gli indici dei documenti rilevanti (separati da virgola): ");
                String[] relevantDocIndicesInput = scanner.nextLine().split(",");
                List<Integer> relevantDocIndices = new ArrayList<>();
                for (String idx : relevantDocIndicesInput) {
                    relevantDocIndices.add(docIdQueue.get(Integer.parseInt(idx.trim())));
                }

                double[] evaluationResults = Eval.evaluateSearchResults(results, relevantDocIndices);
                double mrr = evaluationResults[0];
                double ndcg = evaluationResults[1];

                System.out.println("MRR: " + mrr);
                System.out.println("NDCG: " + ndcg);

                if (count == 0) {
                    mean_mmr = mrr;
                    mean_ndcg = ndcg;
                } else {
                    mean_mmr = (mean_mmr * count + mrr) / (count + 1);
                    mean_ndcg = (mean_ndcg * count + ndcg) / (count + 1);
                }
                count++;

                System.out.println("Media MRR: " + mean_mmr);
                System.out.println("Media NDCG: " + mean_ndcg);
                System.out.println();

                System.out.print("Vuoi fare un'altra ricerca? (s/n): ");
                String continueChoice = scanner.nextLine().trim().toLowerCase();
                if (!continueChoice.equals("s")) {
                    continueSearching = false;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
}


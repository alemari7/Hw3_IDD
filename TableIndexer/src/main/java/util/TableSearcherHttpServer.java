package util;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.ScoreDoc;
import org.jsoup.Jsoup;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Map;
import java.util.HashMap;



import org.json.JSONArray;
import org.json.JSONObject;

public class TableSearcherHttpServer {
    private IndexSearcher searcher;
    private QueryParser captionParser;
    private QueryParser referencesParser;
    private QueryParser footnotesParser;
    private MultiFieldQueryParser multiFieldQueryParser;
    private MultiFieldQueryParser multiFieldQueryParser_Caption_References;
    private MultiFieldQueryParser multiFieldQueryParser_Caption_Footnotes;
    private MultiFieldQueryParser multiFieldQueryParser_References_Footnotes;



    public static String parseHtmlToPlainText(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        return Jsoup.parse(html).text();
    }

    public TableSearcherHttpServer(String indexDirectoryPath) throws Exception {
        DirectoryReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexDirectoryPath)));
        searcher = new IndexSearcher(reader);

        // Inizializza i QueryParser per i campi desiderati
        captionParser = new QueryParser("caption", new StandardAnalyzer());
        referencesParser = new QueryParser("references", new StandardAnalyzer());
        footnotesParser = new QueryParser("footnotes", new StandardAnalyzer());

        // Inizializza il MultiFieldQueryParser per i tutti i campi
        String[] fields_all = {"caption", "references", "footnotes"};
        multiFieldQueryParser = new MultiFieldQueryParser(fields_all, new StandardAnalyzer());

        // Inizializza il MultiFieldQueryParser per i campi caption e references
        String[] fields_cr = {"caption", "references"};
        multiFieldQueryParser_Caption_References = new MultiFieldQueryParser(fields_cr, new StandardAnalyzer());

         // Inizializza il MultiFieldQueryParser per i campi caption e footnotes
        String[] fields_cf = {"caption", "footnotes"};
        multiFieldQueryParser_Caption_Footnotes = new MultiFieldQueryParser(fields_cf, new StandardAnalyzer());

         // Inizializza il MultiFieldQueryParser per i campi caption e footnotes
         String[] fields_rf = {"references", "footnotes"};
         multiFieldQueryParser_References_Footnotes = new MultiFieldQueryParser(fields_rf, new StandardAnalyzer());        
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

    public TopDocs searchMultipleFields_Caption_Footnotes(String queryStr) throws Exception {
        Query query = multiFieldQueryParser_Caption_Footnotes.parse(queryStr);
        return searcher.search(query, 10);
    }    

    public TopDocs searchMultipleFields_Caption_References(String queryStr) throws Exception {
        Query query = multiFieldQueryParser_Caption_References.parse(queryStr);
        return searcher.search(query, 10);
    }

    public TopDocs searchMultipleFields_References_Footnotes(String queryStr) throws Exception {
        Query query = multiFieldQueryParser_References_Footnotes.parse(queryStr);
        return searcher.search(query, 10);
    }

    public static void main(String[] args) {
        try {
            String indexDirectory = "indexDir";  // Percorso dell'indice Lucene
            TableSearcherHttpServer tableSearcher = new TableSearcherHttpServer(indexDirectory);

            // Crea un server HTTP per gestire le richieste di ricerca
            HttpServer server = HttpServer.create(new java.net.InetSocketAddress(8000), 0);
            server.createContext("/search", new SearchHandler(tableSearcher));      // Gestisce le richieste di ricerca
            server.createContext("/metrics", new MetricsHandler(tableSearcher));    // Gestisce le richieste di valutazione
            server.setExecutor(null); // Default executor
            server.start();

            System.out.println("Server HTTP in esecuzione sulla porta 8000");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Nuovo handler per l'endpoint /metrics
    static class MetricsHandler implements HttpHandler {
        private TableSearcherHttpServer tableSearcher;

        // Variabili per calcolare la media delle metriche
        private double mean_mrr = 0.0;
        private double mean_ndcg = 0.0;

        public MetricsHandler(TableSearcherHttpServer tableSearcher) {
            this.tableSearcher = tableSearcher;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "";

            // Aggiungi intestazioni CORS per le risposte
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            // Se la richiesta è OPTIONS (pre-flight), rispondi senza fare nulla
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1); // Risposta vuota per OPTIONS
                return;
            }

            // Se la richiesta è GET, calcola le metriche
            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    // Estrai i parametri dalla query string
                    Map<String, String> queryParams = queryToMap(exchange.getRequestURI().getQuery());
                    String relevantDocs = queryParams.get("relevantDocs");
                    String score = queryParams.get("score");
                    String countS = queryParams.get("count");
                    String docListS = queryParams.get("docList");

                    // Converte i parametri in tipi appropriati
                    int count = Integer.parseInt(countS);

                    // Converte la stringa di 'relevantDocs' in un array di Integer
                    String[] relevantDocsArrayStr = relevantDocs.split(",");
                    Integer[] relevantDocsArray = new Integer[relevantDocsArrayStr.length];
                    for (int i = 0; i < relevantDocsArrayStr.length; i++) {
                        relevantDocsArray[i] = Integer.parseInt(relevantDocsArrayStr[i]);
                    }

                    // Converte la stringa di 'score' in un array di Float
                    String[] scoreArrayStr = score.split(",");
                    Float[] scoreArray = new Float[scoreArrayStr.length];
                    for (int i = 0; i < scoreArrayStr.length; i++) {
                        scoreArray[i] = Float.parseFloat(scoreArrayStr[i]);
                    }

                    // Converte la stringa di 'docList' in un array di Integer
                    String[] docListArrayStr = docListS.split(",");
                    Integer[] docListArray = new Integer[docListArrayStr.length];
                    for (int i = 0; i < docListArrayStr.length; i++) {
                        docListArray[i] = Integer.parseInt(docListArrayStr[i]);
                    }

                    // Calcola i risultati della valutazione
                    double[] evaluationResults = Evaluation.calculateMetrics(relevantDocsArray, docListArray);
                    double mrr = evaluationResults[0];
                    double ndcg = evaluationResults[1];

                    // Calcola la media delle metriche
                    if (count == 1) {
                        mean_mrr = mrr;
                        mean_ndcg = ndcg;
                    } else {
                        mean_mrr = (mean_mrr * (count - 1) + mrr) / count;
                        mean_ndcg = (mean_ndcg * (count - 1) + ndcg) / count;
                    }

                    // Crea la risposta in formato JSON
                    JSONObject metrics = new JSONObject();
                    metrics.put("mrr", mrr);
                    metrics.put("ndcg", ndcg);
                    metrics.put("mean_mrr", mean_mrr);
                    metrics.put("mean_ndcg", mean_ndcg);

                    response = metrics.toString();

                } catch (Exception e) {
                    // Gestione errori
                    JSONObject errorResponse = new JSONObject();
                    errorResponse.put("error", "An error occurred while calculating metrics");
                    response = errorResponse.toString();
                    e.printStackTrace();
                }
            }

            // Invio la risposta
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
        // Funzione per convertire la query string in una mappa di parametri
        private Map<String, String> queryToMap(String query) {
            Map<String, String> queryPairs = new HashMap<>();
            if (query != null) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    if (keyValue.length == 2) {
                        queryPairs.put(keyValue[0], keyValue[1]);
                    }
                }
            }
            return queryPairs;
        }
    }


    // Nuovo handler per l'endpoint /search
    static class SearchHandler implements HttpHandler {
        private TableSearcherHttpServer tableSearcher;

        public SearchHandler(TableSearcherHttpServer tableSearcher) {
            this.tableSearcher = tableSearcher;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "";

            // Aggiungi intestazioni CORS per le risposte
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            // Se la richiesta è OPTIONS (pre-flight), rispondi senza fare nulla
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(200, -1); // Risposta vuota per OPTIONS
                return;
            }

            // Se la richiesta è GET, esegui la ricerca
            if ("GET".equals(exchange.getRequestMethod())) {
                // Estrai i parametri dalla query string
                Map<String, String> queryParams = queryToMap(exchange.getRequestURI().getQuery());
                String query = queryParams.get("query");
                String field = queryParams.get("fields");
            
                // Esegui la ricerca in base al campo specificato
                try {
                    TopDocs results = null;
                    if (field == null || field.equals("caption")) {
                        results = tableSearcher.searchCaption(query);
                    } else if (field.equals("references")) {
                        results = tableSearcher.searchReferences(query);
                    } else if (field.equals("footnotes")) {
                        results = tableSearcher.searchFootnotes(query);
                    } else if (field.equals("caption,references,footnotes")) {
                        results = tableSearcher.searchMultipleFields(query);
                    } else if (field.equals("caption,references")) {
                        results = tableSearcher.searchMultipleFields_Caption_References(query);
                    } else if (field.equals("caption,footnotes")) {
                        results = tableSearcher.searchMultipleFields_Caption_Footnotes(query);
                    } else if (field.equals("references,footnotes")) {
                        results = tableSearcher.searchMultipleFields_References_Footnotes(query);
                    }

                    // Creazione risposta in formato JSON
                    JSONArray resultArray = new JSONArray();
            
                    if (results != null) {
                        // Scorrere i risultati e creare un oggetto JSON per ciascun documento
                        for (ScoreDoc scoreDoc : results.scoreDocs) {
                            int docId = scoreDoc.doc;
                            float score = scoreDoc.score;  // Ottieni lo score del documento
                            Document doc = tableSearcher.searcher.storedFields().document(docId);
                            String caption = doc.get("caption");
                            String footnotes = doc.get("footnotes");
                            String references = doc.get("references");
                            String table = doc.get("table");
                            String sourceFile = doc.get("source_file");

                            // Crea un oggetto JSON per ogni documento e aggiungilo all'array
                            JSONObject resultObject = new JSONObject();
                            resultObject.put("DocId", String.valueOf(docId)); // ID del documento
                            resultObject.put("caption", caption != null ? caption : "N/A");
                            resultObject.put("footnotes", footnotes != null ? footnotes : "N/A");
                            resultObject.put("references", references != null ? references : "N/A");
                            resultObject.put("table", table != null ? table : "N/A");
                            resultObject.put("source_file", sourceFile != null ? sourceFile : "N/A");
                            resultObject.put("score", score);  // Aggiungi lo score del documento

                            resultArray.put(resultObject);
                        }
                    } else {
                        // Nessun risultato trovato
                        JSONObject noResults = new JSONObject();
                        noResults.put("message", "No results found");
                        resultArray.put(noResults);
                    }

                    // Imposta la risposta come stringa JSON
                    response = resultArray.toString();
            
                } catch (Exception e) {
                    // Gestione errori
                    JSONObject errorResponse = new JSONObject();
                    errorResponse.put("error", "An error occurred during search");
                    response = errorResponse.toString();
                    e.printStackTrace();
                }
            }
            // Invio la risposta
            exchange.sendResponseHeaders(200, response.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }

        // Funzione per convertire query string in map
        private Map<String, String> queryToMap(String query) {
            Map<String, String> queryPairs = new HashMap<>();
            if (query != null) {
                String[] pairs = query.split("&");
                for (String pair : pairs) {
                    String[] keyValue = pair.split("=");
                    queryPairs.put(keyValue[0], keyValue[1]);
                }
            }
            return queryPairs;
        }
    }
}

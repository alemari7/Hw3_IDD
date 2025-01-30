package util;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;

public class JSONIndexer {

    private IndexWriter writer;
    private int indexedFileCount = 0; // Conta il numero di file indicizzati

    public JSONIndexer(String indexPath) throws IOException {
        Directory dir = FSDirectory.open(Paths.get(indexPath)); // Crea un oggetto Directory per l'indice
        Analyzer analyzer = new StandardAnalyzer(); // Crea un analizzatore per l'indice
        IndexWriterConfig config = new IndexWriterConfig(analyzer); // Configura l'IndexWriter
        
        // Imposta l'IndexWriter in modalità CREATE per sovrascrivere l'indice esistente
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        writer = new IndexWriter(dir, config);
    }

    public void indexJsonFiles(String jsonDirPath) throws IOException {
        // Metodo che accetta il percorso della directory con i file JSON da indicizzare
        File jsonDir = new File(jsonDirPath);
        
        if (!jsonDir.exists()) {
            System.out.println("Directory does not exist: " + jsonDir.getAbsolutePath());
            return;
        }

        File[] jsonFiles = jsonDir.listFiles((dir, name) -> name.endsWith(".json") && !name.startsWith("._"));
        
        if (jsonFiles == null || jsonFiles.length == 0) {
            System.out.println("No JSON files found in directory: " + jsonDir.getAbsolutePath());
            return;
        }
        
        System.out.println("Found " + jsonFiles.length + " JSON files in directory: " + jsonDir.getAbsolutePath());
        
        for (File jsonFile : jsonFiles) {
            if (jsonFile.canRead()) {
                System.out.println("Indexing file: " + jsonFile.getName());
                indexDocument(jsonFile);
                indexedFileCount++; // Incrementa il contatore dei file indicizzati
            } else {
                System.out.println("Cannot read file: " + jsonFile.getName());
            }
        }
    }

    private void indexDocument(File jsonFile) throws IOException {
        try (FileReader fileReader = new FileReader(jsonFile)) {
            StringBuilder jsonContent = new StringBuilder();
            int i;
            while ((i = fileReader.read()) != -1) {
                jsonContent.append((char) i);
            }

            // Parsing del contenuto JSON
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(jsonContent.toString());
            } catch (JSONException e) {
                System.out.println("Skipping invalid JSON file: " + jsonFile.getName());
                return; // Salta il file se non è JSON valido
            }
            
            // Itera sulle chiavi del JSON principale
            for (String key : jsonObject.keySet()) {
                Object value = jsonObject.get(key); // Ottieni il valore associato alla chiave
                
                Document doc = new Document();
                
                // Estrarre e indicizzare la "table"
                if (value instanceof JSONObject) {
                    JSONObject tableInfo = (JSONObject) value;

                    // Verifica che "table" sia un oggetto JSON
                    if (tableInfo.has("table") && tableInfo.get("table") instanceof JSONObject) {
                        JSONObject table = tableInfo.getJSONObject("table");
                        String tableContent = table.toString(); // Oppure estrai altri dati dalla tabella
                        doc.add(new TextField("table", tableContent, Field.Store.YES));
                    } else {
                        String tableContent = tableInfo.optString("table", "No table content available");
                        doc.add(new TextField("table", tableContent, Field.Store.YES));
                    }
                }

                // Estrarre e indicizzare "caption"
                if (value instanceof JSONObject && ((JSONObject) value).has("caption")) {
                    JSONObject tableInfo = (JSONObject) value;
                    String caption = tableInfo.optString("caption", "No caption available");
                    doc.add(new TextField("caption", caption, Field.Store.YES));
                }

                // Estrarre e indicizzare "footnotes"
                if (value instanceof JSONObject && ((JSONObject) value).has("footnotes")) {
                    JSONObject tableInfo = (JSONObject) value;
                    String footnotes = tableInfo.optString("footnotes", "No footnotes available");
                    doc.add(new TextField("footnotes", footnotes, Field.Store.YES));
                }

                // Estrarre e indicizzare "reference"
                if (value instanceof JSONObject && ((JSONObject) value).has("references")) {
                    JSONObject tableInfo = (JSONObject) value;
                    String reference = tableInfo.optString("references", "No reference available");
                    doc.add(new TextField("references", reference, Field.Store.YES));
                }

                // Aggiungi anche il nome del file per tracciare la provenienza dei dati
                doc.add(new StringField("source_file", jsonFile.getName(), Field.Store.YES));

                // Aggiungi il documento all'indice
                writer.addDocument(doc);
            }
        } catch (Exception e) {
            System.out.println("Error indexing file: " + jsonFile.getName());
            e.printStackTrace();
        }
    }

    public void close() throws IOException {
        writer.close();
        System.out.println("IndexWriter closed.");
        System.out.println("Total files indexed: " + indexedFileCount); // Stampa il numero totale di file indicizzati
    }

    public static void main(String[] args) {
        try {
            String indexPath = "indexDir"; // Specifica il percorso per l'indice
            String jsonDirPath = "json_cleaned"; // Specifica il percorso della directory con i file JSON
            
            JSONIndexer indexer = new JSONIndexer(indexPath);
            indexer.indexJsonFiles(jsonDirPath);
            indexer.close();
            System.out.println("Indexing complete.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

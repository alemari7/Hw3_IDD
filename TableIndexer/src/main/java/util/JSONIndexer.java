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
    private int indexedFilesCount = 0; // Variabile per tenere traccia dei file indicizzati

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

            // Verifica se il contenuto del file JSON è vuoto
            if (jsonContent.length() == 0) {
                System.out.println("Skipping empty file: " + jsonFile.getName());
                return;
            }

            // Parsing del contenuto JSON
            JSONObject jsonObject = null;
            try {
                jsonObject = new JSONObject(jsonContent.toString());
            } catch (JSONException e) {
                System.out.println("Skipping invalid JSON file: " + jsonFile.getName());
                return; // Salta il file se non è JSON valido
            }

            // Verifica che il JSON sia un oggetto e non un array o altro tipo
            if (!(jsonObject instanceof JSONObject)) {
                System.out.println("Skipping non-object JSON file: " + jsonFile.getName());
                return;
            }

            Document doc = new Document();
            StringBuilder tableContent = new StringBuilder();
            StringBuilder captionContent = new StringBuilder();
            StringBuilder footnotesContent = new StringBuilder();
            StringBuilder referencesContent = new StringBuilder();

            // Itera su tutte le chiavi e concatena i valori
            for (String key : jsonObject.keySet()) {
                Object value = jsonObject.get(key);
                
                // Estrarre e concatenare la "table"
                if (key.equals("table") && value instanceof JSONObject) {
                    JSONObject tableInfo = (JSONObject) value;
                    tableContent.append(tableInfo.toString());
                }

                // Estrarre e concatenare "caption"
                if (key.equals("caption") && value instanceof String) {
                    captionContent.append((String) value);
                }

                // Estrarre e concatenare "footnotes"
                if (key.equals("footnotes") && value instanceof String) {
                    footnotesContent.append((String) value);
                }

                // Estrarre e concatenare "references"
                if (key.equals("references") && value instanceof String) {
                    referencesContent.append((String) value);
                }
            }

            // Aggiungi tutti i dati concatenati nel documento
            doc.add(new TextField("table", tableContent.toString(), Field.Store.YES));
            doc.add(new TextField("caption", captionContent.toString(), Field.Store.YES));
            doc.add(new TextField("footnotes", footnotesContent.toString(), Field.Store.YES));
            doc.add(new TextField("references", referencesContent.toString(), Field.Store.YES));
            
            // Aggiungi anche il nome del file per tracciare la provenienza dei dati
            doc.add(new StringField("source_file", jsonFile.getName(), Field.Store.YES));

            // Aggiungi il documento all'indice
            writer.addDocument(doc);

            // Incrementa il contatore dei file indicizzati
            indexedFilesCount++;
        } catch (Exception e) {
            System.out.println("Error indexing file: " + jsonFile.getName());
            e.printStackTrace();
        }
    }

    public void close() throws IOException {
        writer.close();
        System.out.println("IndexWriter closed.");
        // Stampa il numero totale di file indicizzati
        System.out.println("Total indexed files: " + indexedFilesCount);
    }

    public static void main(String[] args) {
        try {
            String indexPath = "indexDir"; // Specifica il percorso per l'indice
            String jsonDirPath = "all_tables"; // Specifica il percorso della directory con i file JSON
            
            JSONIndexer indexer = new JSONIndexer(indexPath);
            indexer.indexJsonFiles(jsonDirPath);
            indexer.close();
            System.out.println("Indexing complete.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

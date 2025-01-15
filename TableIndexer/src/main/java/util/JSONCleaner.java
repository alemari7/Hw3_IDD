package util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class JSONCleaner {

    public static void cleanJsonFiles(String sourceDirPath, String destinationDirPath) throws IOException {
        File sourceDir = new File(sourceDirPath);
        File destinationDir = new File(destinationDirPath);

        // Controlla se la directory sorgente esiste
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            System.out.println("Source directory does not exist or is not a directory: " + sourceDirPath);
            return;
        }

        // Crea la directory di destinazione se non esiste
        if (!destinationDir.exists()) {
            destinationDir.mkdirs();
        }

        File[] jsonFiles = sourceDir.listFiles((dir, name) -> name.endsWith(".json") && !name.startsWith("._"));

        if (jsonFiles == null || jsonFiles.length == 0) {
            System.out.println("No JSON files found in source directory: " + sourceDirPath);
            return;
        }

        System.out.println("Found " + jsonFiles.length + " JSON files in source directory.");

        int validFilesCount = 0;
        for (File jsonFile : jsonFiles) {
            if (jsonFile.canRead()) {
                System.out.println("Processing file: " + jsonFile.getName());
                if (isValidJson(jsonFile)) {
                    // Copia il file valido nella directory di destinazione
                    Files.copy(jsonFile.toPath(), Paths.get(destinationDirPath, jsonFile.getName()));
                    validFilesCount++;
                } else {
                    System.out.println("Invalid JSON file skipped: " + jsonFile.getName());
                }
            } else {
                System.out.println("Cannot read file: " + jsonFile.getName());
            }
        }

        System.out.println("Cleaning complete. Valid files saved: " + validFilesCount);
    }

    private static boolean isValidJson(File jsonFile) {
        try (FileReader fileReader = new FileReader(jsonFile)) {
            StringBuilder jsonContent = new StringBuilder();
            int i;
            while ((i = fileReader.read()) != -1) {
                jsonContent.append((char) i);
            }

            // Salta i file vuoti
            if (jsonContent.length() == 0) {
                System.out.println("Empty file: " + jsonFile.getName());
                return false;
            }

            // Verifica se il contenuto è un oggetto JSON valido
            new JSONObject(jsonContent.toString());
            return true; // File valido
        } catch (JSONException e) {
            System.out.println("Invalid JSON structure in file: " + jsonFile.getName());
            return false;
        } catch (IOException e) {
            System.out.println("Error reading file: " + jsonFile.getName());
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        try {
            String sourceDirPath = "all_tables"; // Directory sorgente con i file JSON
            String destinationDirPath = "cleaned_tables"; // Directory per i file validi

            cleanJsonFiles(sourceDirPath, destinationDirPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

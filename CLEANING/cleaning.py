import os
import json
import shutil

def is_valid_json(file_path):
    """ Verifica se un file JSON è valido e non vuoto """
    try:
        with open(file_path, 'r', encoding='utf-8') as file:
            content = file.read().strip()
            
            if not content:
                print(f"Empty file: {file_path}")
                return False
            
            json.loads(content)  # Prova a caricare il JSON
            return True
    except (json.JSONDecodeError, IOError) as e:
        print(f"Invalid JSON structure in file: {file_path} - {e}")
        return False

def clean_json_files(source_dir, destination_dir):
    """ Pulisce i file JSON, spostando solo quelli validi in una nuova directory """
    if not os.path.exists(source_dir) or not os.path.isdir(source_dir):
        print(f"Source directory does not exist or is not a directory: {source_dir}")
        return
    
    # Rimuove la directory di destinazione se esiste
    if os.path.exists(destination_dir):
        shutil.rmtree(destination_dir)
    os.makedirs(destination_dir)
    
    json_files = [f for f in os.listdir(source_dir) if f.endswith(".json") and not f.startswith("._")]
    
    if not json_files:
        print(f"No JSON files found in source directory: {source_dir}")
        return
    
    print(f"Found {len(json_files)} JSON files in source directory.")
    
    valid_files_count = 0
    for json_file in json_files:
        file_path = os.path.join(source_dir, json_file)
        print(f"Processing file: {json_file}")
        
        if os.path.isfile(file_path) and is_valid_json(file_path):
            shutil.copy(file_path, os.path.join(destination_dir, json_file))
            valid_files_count += 1
        else:
            print(f"Invalid JSON file skipped: {json_file}")
    
    print(f"Cleaning complete. Valid files saved: {valid_files_count}")

# Percorso assoluto delle cartelle
SOURCE_DIR = "/home/alessio/Desktop/UNI MAGISTRALE/ESAMI DA FARE/INGEGNERIA DEI DATI/Homework/Hw3_IDD/Hw3_IDD/TableIndexer/all_tables"  # Sostituisci con il percorso effettivo
DESTINATION_DIR = "/home/alessio/Desktop/UNI MAGISTRALE/ESAMI DA FARE/INGEGNERIA DEI DATI/Homework/Hw3_IDD/Hw3_IDD/TableIndexer/json_cleaned"  # Sostituisci con il percorso effettivo

# Esegui la pulizia
if __name__ == "__main__":
    clean_json_files(SOURCE_DIR, DESTINATION_DIR)

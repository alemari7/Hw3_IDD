import os

def count_files_in_directory(directory_path):
    try:
        # Lista tutti i file e le sottocartelle nella directory
        files = [f for f in os.listdir(directory_path) if os.path.isfile(os.path.join(directory_path, f))]

        # Conta solo i file (esclude le sottocartelle)
        print(f"Numero di file nella cartella '{directory_path}': {len(files)}")
    except FileNotFoundError:
        print("Errore: La cartella specificata non esiste.")
    except PermissionError:
        print("Errore: Permesso negato per accedere alla cartella.")

# Esempio di utilizzo
directory_path = "/home/alessio/Desktop/UNI MAGISTRALE/ESAMI DA FARE/INGEGNERIA DEI DATI/Homework/Hw3_IDD/Hw3_IDD/TableIndexer/all_tables"  # Sostituisci con il percorso reale
count_files_in_directory(directory_path)

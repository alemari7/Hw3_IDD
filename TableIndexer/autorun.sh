#!/bin/bash

# Funzione per mostrare l'uso dello script
usage() {
    echo "Usage: $0 -type [console|server]"
    exit 1
}

# Inizializza le variabili
TYPE=""

# Parse degli argomenti
while [[ "$#" -gt 0 ]]; do
    case $1 in
        -type)
            TYPE="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            usage
            ;;
    esac
done

# Controllo che il parametro type sia specificato
if [[ -z "$TYPE" ]]; then
    echo "Error: -type parameter is required."
    usage
fi

# Effettua il building del progetto
mvn clean install

# Esegui la pulizia dei file JSON
mvn exec:java -Dexec.mainClass="util.JSONCleaner"

# Esegui l'indicizzazione dei file JSON
mvn exec:java -Dexec.mainClass="util.JSONIndexer"

# Esegui il comando corrispondente al tipo
case "$TYPE" in
    server)
        # Esegui il server HTTP
        mvn exec:java -Dexec.mainClass="util.TableSearcherHttpServer"
        ;;
    console)
        # Esegui la console di ricerca
        mvn exec:java -Dexec.mainClass="util.TableSearcherConsole"
        ;;
    *)
        echo "Error: Invalid type '$TYPE'. Allowed values are 'console' or 'server'."
        usage
        ;;
esac

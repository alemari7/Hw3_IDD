#!/bin/bash

# Funzione per mostrare l'uso dello script
usage() {
    echo "Usage: $0 -type [console|server] [-i]"
    echo "  -type: Specify 'console' or 'server' mode."
    echo "  -i:    Run indexing before executing the type-specific command."
    exit 1
}

# Inizializza le variabili
TYPE=""
RUN_INDEXING=false

# Parse degli argomenti
while [[ "$#" -gt 0 ]]; do
    case $1 in
        -type)
            TYPE="$2"
            shift 2
            ;;
        -i)
            RUN_INDEXING=true
            shift
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

# Esegui sempre il comando mvn clean install
mvn clean install

# Esegui l'indicizzazione solo se richiesto con -i
if $RUN_INDEXING; then
    mvn exec:java -Dexec.mainClass="util.JSONCleaner"
    mvn exec:java -Dexec.mainClass="util.JSONIndexer"
fi

# Esegui il comando corrispondente al tipo
case "$TYPE" in
    server)
        mvn exec:java -Dexec.mainClass="util.TableSearcherHttpServer"
        ;;
    console)
        mvn exec:java -Dexec.mainClass="util.TableSearcherConsole"
        ;;
    *)
        echo "Error: Invalid type '$TYPE'. Allowed values are 'console' or 'server'."
        usage
        ;;
esac

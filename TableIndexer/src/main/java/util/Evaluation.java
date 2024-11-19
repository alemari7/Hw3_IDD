package util;

import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

import java.util.List;

public class Evaluation {

    // Calcolo del Mean Reciprocal Rank (MRR)
    public static double calculateMRR(ScoreDoc[] scoreDocs, List<Integer> relevantDocIndices) {
        // Trova il documento con il punteggio più alto tra quelli restituiti
        int highestScoringDocId = scoreDocs[0].doc;
        double highestScore = scoreDocs[0].score;

        // Scorri i risultati per trovare il documento con il punteggio più alto
        for (int i = 1; i < scoreDocs.length; i++) {
            if (scoreDocs[i].score > highestScore) {
                highestScoringDocId = scoreDocs[i].doc;
                highestScore = scoreDocs[i].score;
            }
        }

        // Trova la posizione di quel documento più rilevante nella lista fornita dall'utente
        int userPosition = relevantDocIndices.indexOf(highestScoringDocId);

        // Calcola la MRR come l'inverso della posizione nella lista (1-based index)
        if (userPosition != -1) {
            return 1.0 / (userPosition + 1);  // Reciprocal Rank
        }

        return 0.0; // Nessun documento rilevante trovato
    }

    // Calcolo del Normalized Discounted Cumulative Gain (NDCG)
    public static double calculateNDCG(TopDocs topDocs, List<Integer> relevantDocIndices) {
        double dcg = 0.0;
        double idcg = 0.0;

        // Calcola il DCG (basato sui risultati restituiti)
        for (int i = 0; i < topDocs.scoreDocs.length; i++) {
            int docId = topDocs.scoreDocs[i].doc; // Ottieni l'ID del documento
            int relevance = getRelevance(docId, relevantDocIndices); // Ottieni la rilevanza relativa
            if (relevance > 0) {
                dcg += relevance / (Math.log(i + 2) / Math.log(2)); // Calcolo DCG
            }
        }

        // Calcola l'IDCG (ordine ideale basato sulla lista dei rilevanti fornita)
        for (int i = 0; i < relevantDocIndices.size(); i++) {
            int relevance = relevantDocIndices.size() - i; // Rilevanza ideale decrescente
            idcg += relevance / (Math.log(i + 2) / Math.log(2));
        }

        return idcg > 0 ? dcg / idcg : 0.0;
    }

    // Ottieni la rilevanza relativa di un documento (in base alla lista fornita)
    private static int getRelevance(int docId, List<Integer> relevantDocIndices) {
        int position = relevantDocIndices.indexOf(docId);
        if (position != -1) {
            return relevantDocIndices.size() - position; // Rilevanza decrescente
        }
        return 0; // Non rilevante
    }

    // Metodo di valutazione
    public static void evaluateSearchResults(TopDocs topDocs, List<Integer> relevantDocIndices) {
        double mrr = calculateMRR(topDocs.scoreDocs, relevantDocIndices);
        double ndcg = calculateNDCG(topDocs, relevantDocIndices);

        System.out.println("MRR: " + mrr);
        System.out.println("NDCG: " + ndcg);
    }
}

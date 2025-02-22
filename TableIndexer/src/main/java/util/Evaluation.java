package util;



import java.util.Arrays;



public class Evaluation {

    // Funzione per calcolare MRR e NDCG
    public static double[] calculateMetrics(Integer[] relevantDocsArray, Integer[] docListArray) {
        double mrr = calculateMRR(relevantDocsArray, docListArray);
        double ndcg = calculateNDCG(relevantDocsArray, docListArray);
        return new double[]{mrr, ndcg};
    }

    // Calcolo del Mean Reciprocal Rank (MRR)
    public static double calculateMRR(Integer[] relevantDocsArray, Integer[] docListArray) {
        for (int i = 0; i < docListArray.length; i++) {
            int docId = docListArray[i];
            if (Arrays.asList(relevantDocsArray).contains(docId)) {
                return 1.0 / (i + 1); // MRR è l'inverso della posizione del primo documento rilevante
            }
        }
        return 0.0; // Se nessun documento rilevante è trovato
    }

    // Calcolo del Normalized Discounted Cumulative Gain (NDCG)
    public static double calculateNDCG(Integer[] relevantDocsArray, Integer[] docListArray) {
        double dcg = 0.0;
        double idcg = 0.0;

        // Calcolo il DCG
        for (int i = 0; i < docListArray.length; i++) {
            int docId = docListArray[i];
            if (Arrays.asList(relevantDocsArray).contains(docId)) {
                dcg += 1 / (Math.log(i + 2) / Math.log(2)); // Aggiungi il contributo al DCG
            }
        }

        // Calcolo l'IDCG (ideal DCG)
        for (int i = 0; i < relevantDocsArray.length; i++) {
            idcg += 1 / (Math.log(i + 2) / Math.log(2)); // Aggiungi il contributo al IDCG
        }

        return (idcg > 0) ? dcg / idcg : 0.0;
    }

}

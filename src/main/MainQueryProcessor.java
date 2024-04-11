package main;

import queryProcessing.Probability;
import queryProcessing.QueryProcessor;
import queryProcessing.Result;

import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class MainQueryProcessor {

    public static void main(String[] args) {
        QueryProcessor queryProcessor = new QueryProcessor();

        while (true) {
            System.out.println("Type in your query:");
            Scanner input = new Scanner(System.in);

            Map<String, List<Result>> map_premiseClusterId_result = queryProcessor.processQuery(
                    input.nextLine(),
                    QueryProcessor.defaultSimilarityMethodForClaimRetrieval,
                    10,
                    Probability.ProbabilityType.dcfsAggregated
            );

            System.out.println("Finished");
        }
    }

}

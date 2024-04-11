package queryProcessing;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import utils.Util;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

import static utils.Util.*;

public class QueryProcessor {

    /* --- variables --- */

    // indexes
    StandardAnalyzer standardAnalyzer = new StandardAnalyzer();

    Directory fsDirectory_claims;
    IndexWriterConfig indexWriterConfig_claims = new IndexWriterConfig(standardAnalyzer);
    IndexWriter indexWriter_claims;
    IndexReader indexReader_claims;
    IndexSearcher indexSearcher_claims;

    Directory fsDirectory_premises;
    IndexWriterConfig indexWriterConfig_premises = new IndexWriterConfig(standardAnalyzer);
    IndexWriter indexWriter_premises;
    IndexReader indexReader_premises;


    // intern variables
    Map<String, String> map_premiseId_claimId;
    Map<String, List<String>> map_claimId_premiseIds;

    Map<String, List<String>> map_claimClusterId_claimIds;
    Map<String, String> map_claimId_claimClusterId;

    Map<String, List<String>> map_premiseClusterId_premiseId;
    Map<String, String> map_premiseId_premiseClusterId;


    // extern variables
    public static final Similarity defaultSimilarityMethodForClaimRetrieval = new DFRSimilarity(
            new BasicModelIne(),
            new AfterEffectL(),
            new NormalizationZ()
    );
    Similarity similarityMethodForClaimRetrieval;

    /* ----------------- */


    // Constructor
    public QueryProcessor() {
        initialize();
    }


    /* --- methods --- */
    // initializing methods
    void initialize () {
        initializeIndexes();
        initializeGraphStructure();
        BooleanQuery.setMaxClauseCount(4096); // https://stackoverflow.com/a/18679736
        similarityMethodForClaimRetrieval = defaultSimilarityMethodForClaimRetrieval;
    }

    private void initializeIndexes() {
        try {
            fsDirectory_claims = FSDirectory.open(pathToClaimsAndPremiseIdsIndex);
            indexWriter_claims = new IndexWriter(fsDirectory_claims, indexWriterConfig_claims);
            indexReader_claims = DirectoryReader.open(indexWriter_claims);
            indexSearcher_claims = new IndexSearcher(indexReader_claims);

            fsDirectory_premises = FSDirectory.open(pathToPremisesAndPremiseIdsIndex);
            indexWriter_premises = new IndexWriter(fsDirectory_premises, indexWriterConfig_premises);
            indexReader_premises = DirectoryReader.open(indexWriter_premises);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeGraphStructure () {
        map_claimId_premiseIds = getMapWithArgsMeClaimIdsAndPremiseIds(pathToCSVFileWithClaimsAndPremiseIds);
        map_premiseId_claimId = getMapWithAndPremiseIdAndArgsMeClaimId(pathToCSVFileWithClaimsAndPremiseIds);

        getMapsWithClaimClusterIdsAndTheirClaimIds();
        getMapsWithPremiseIdAndPremiseClusterId();
    }

    private Map<String, List<String>> getMapWithArgsMeClaimIdsAndPremiseIds (Path pathToCSVFileWithClaimsAndPremiseIds) {
        Map<String, List<String>> map_claimId_premiseIds = new HashMap<>();

        CSVParser csvParser = Util.readCSVFile(pathToCSVFileWithClaimsAndPremiseIds);
        for (CSVRecord csvRecord : Objects.requireNonNull(csvParser)) {
            List<String> premiseIds = new ArrayList<>();
            String premiseIdsString = csvRecord.get("premiseIds");
            StringTokenizer tokens = new StringTokenizer(premiseIdsString, "[], ");
            while (tokens.hasMoreTokens()) {
                premiseIds.add(tokens.nextToken());
            }
            map_claimId_premiseIds.put(csvRecord.get("claimId"), premiseIds);
        }

        return map_claimId_premiseIds;
    }

    private Map<String, String> getMapWithAndPremiseIdAndArgsMeClaimId (Path pathToCSVFileWithClaimsAndPremiseIds) {
        Map<String, String> map_premiseId_claimId = new HashMap<>();

        CSVParser csvParser = Util.readCSVFile(pathToCSVFileWithClaimsAndPremiseIds);
        for (CSVRecord csvRecord : Objects.requireNonNull(csvParser)) {
            List<String> premiseIds = new ArrayList<>();
            String premiseIdsString = csvRecord.get("premiseIds");
            StringTokenizer tokens = new StringTokenizer(premiseIdsString, "[], ");
            while (tokens.hasMoreTokens()) {
                premiseIds.add(tokens.nextToken());
            }
            for (String argumentId : premiseIds) {
                map_premiseId_claimId.put(argumentId, csvRecord.get("claimId"));
            }
        }

        return map_premiseId_claimId;
    }

    private void getMapsWithClaimClusterIdsAndTheirClaimIds () {

        try {
            map_claimClusterId_claimIds = new HashMap<>();
            map_claimId_claimClusterId = new HashMap<>();

            for (int i=0; i<indexReader_claims.maxDoc(); i++) {
                Document document = indexReader_claims.document(i);
                String claimId = document.get("claimId");
                String claimClusterId = document.get("claimClusterId");
                
                map_claimId_claimClusterId.put(claimId, claimClusterId);

                map_claimClusterId_claimIds.putIfAbsent(claimClusterId, new ArrayList<>());
                map_claimClusterId_claimIds.get(claimClusterId).add(document.get("claimId"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void getMapsWithPremiseIdAndPremiseClusterId () {

        try {
            map_premiseId_premiseClusterId = new HashMap<>();
            map_premiseClusterId_premiseId = new HashMap<>();

            for (int i=0; i<indexReader_premises.maxDoc(); i++) {
                Document document = indexReader_premises.document(i);
                String premiseId = document.get("premiseId");
                String premiseClusterId = document.get("premiseClusterId");

                map_premiseId_premiseClusterId.put(premiseId, premiseClusterId);

                map_premiseClusterId_premiseId.putIfAbsent(premiseClusterId, new ArrayList<>());
                map_premiseClusterId_premiseId.get(premiseClusterId).add(premiseId);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    // query processing methods
    public Map<String, List<Result>> processQuery(
            String queryClaim,
            Similarity similarityMethodForClaimRetrieval,
            int numberOfTopKMostSimilarClaims,
            Probability.ProbabilityType probabilityType) {

        Map<String, Double> map_claimId_similarityScoreToQuery = new HashMap<>();
        List<ArgsMeClaim> resultClaims = getMostSimilarResultClaimsWithScores(queryClaim, similarityMethodForClaimRetrieval, numberOfTopKMostSimilarClaims, map_claimId_similarityScoreToQuery);

        Set<String> claimClusterIds = getClaimClusterIds(resultClaims);
        HashSet<String> claimIdsOfClaimsInSameClusters = getClaimIdsOfClaimsInSameClusters(claimClusterIds);
        List<String> premiseIds = getRelevantPremiseIds(claimIdsOfClaimsInSameClusters);
        List<ArgsMePremise> premises = getRelevantPremises(premiseIds);
        List<ArgsMePremise> premises_extendedSet = getExtendedSetOfPremises (premises);

        Map<String, List<String>> map_premiseClusterId_premiseIds = getMapWithPremiseClusterIdsAndAListWithAllItsPremiseIds(premises_extendedSet);

        Map<String, Probability> map_premiseId_probabilities = estimateAndComputeSingleProbabilities(premises, map_premiseClusterId_premiseIds, map_claimId_similarityScoreToQuery, probabilityType);
        Map<String, List<Probability>> map_premiseClusterId_listOfItsProbabilities = gatherAllProbabilitiesForEachCluster(premises, map_premiseId_probabilities);
        Map<String, List<Result>> map_premiseClusterId_result = getMapWithResultClusters(queryClaim, map_claimId_similarityScoreToQuery, premises, map_premiseId_probabilities, map_premiseClusterId_listOfItsProbabilities, probabilityType);

        return map_premiseClusterId_result;
    }


    private List<ArgsMeClaim> getMostSimilarResultClaimsWithScores(String queryClaim, Similarity similarityMethodForClaimRetrieval, int numberOfTopKMostSimilarClaims, Map<String, Double> map_claimId_similarityScoreToQuery) {
        List<ArgsMeClaim> resultClaims = null;
        try {
            resultClaims = new ArrayList<>();

            indexSearcher_claims.setSimilarity(similarityMethodForClaimRetrieval);

            Query query = new QueryParser("claimText", standardAnalyzer).parse(queryClaim);
            TopDocs topDocs = indexSearcher_claims.search(query, Integer.MAX_VALUE);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;

            for (ScoreDoc scoreDoc : scoreDocs) {
                int docId = scoreDoc.doc;
                Document document = indexSearcher_claims.doc(docId);

                ArgsMeClaim argsMeClaim = new ArgsMeClaim(
                        document.get("claimId"),
                        document.get("claimText"),
                        document.get("claimClusterId"),
                        document.get("premiseId")
                );

                map_claimId_similarityScoreToQuery.put(
                        document.get("claimId"),
                        (double) scoreDoc.score
                );

                if (resultClaims.size() < numberOfTopKMostSimilarClaims) {
                    resultClaims.add(argsMeClaim);
                }
            }


        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return resultClaims;
    }

    private Set<String> getClaimClusterIds(List<ArgsMeClaim> resultClaims) {
        Set<String> claimClusterIds = new HashSet<>();
        for (ArgsMeClaim resultClaim : resultClaims) {
            claimClusterIds.add(resultClaim.getClaimClusterId());
        }
        return claimClusterIds;
    }

    private HashSet<String> getClaimIdsOfClaimsInSameClusters(Set<String> claimClusterIds) {
        HashSet<String> claimIdsOfClaimsInSameClusters = new HashSet<>();
        try {
            for (String claimClusterId : claimClusterIds) {

                Query query = new QueryParser("claimClusterId", standardAnalyzer).parse(claimClusterId);
                TopDocs topDocs = indexSearcher_claims.search(query, Integer.MAX_VALUE);
                ScoreDoc[] scoreDocs = topDocs.scoreDocs;

                for (ScoreDoc scoreDoc : scoreDocs) {
                    int docId = scoreDoc.doc;
                    Document document = indexSearcher_claims.doc(docId);

                    if (document.get("claimClusterId").equals(claimClusterId)) {
                        claimIdsOfClaimsInSameClusters.add(document.get("claimId"));
                    }

                }
            }
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        }
        return claimIdsOfClaimsInSameClusters;
    }

    private List<String> getRelevantPremiseIds(HashSet<String> claimIdsOfClaimsInSameClusters) {
        List<String> premiseIds = new ArrayList<>();
        for (String claimId : claimIdsOfClaimsInSameClusters) {
            premiseIds.addAll(map_claimId_premiseIds.get(claimId));
        }
        return premiseIds;
    }

    private List<ArgsMePremise> getRelevantPremises(List<String> premiseIds) {
        List<ArgsMePremise> premises = null;

        try {
            premises = new ArrayList<>();

            for (int i=0; i<indexReader_premises.maxDoc(); i++) {
                Document document = indexReader_premises.document(i);
                String premiseId = document.get("premiseId");
                if (premiseIds.contains(premiseId)) {
                    ArgsMePremise argsMePremise = new ArgsMePremise(
                            document.get("premiseId"),
                            document.get("premiseText"),
                            document.get("premiseStance"),
                            document.get("claimText"),
                            document.get("discussionTitle"),
                            document.get("premiseClusterId"),
                            document.get("dcf_cogency"),
                            document.get("dcf_reasonableness"),
                            document.get("dcf_effectiveness")
                    );
                    premises.add(argsMePremise);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return premises;
    }

    private List<ArgsMePremise> getExtendedSetOfPremises (List<ArgsMePremise> premises) {
        List<ArgsMePremise> premises_extendedSet = null;

        try {
            premises_extendedSet = new ArrayList<>();

            Set<String> premiseClusterIds = new HashSet<>();
            for (ArgsMePremise premise : premises) {
                premiseClusterIds.add(premise.getPremiseClusterId());
            }

            for (int i=0; i<indexReader_premises.maxDoc(); i++) {
                Document document = indexReader_premises.document(i);
                String tmpPremiseClusterId = document.get("premiseClusterId");
                if (premiseClusterIds.contains(tmpPremiseClusterId)) {
                    ArgsMePremise argsMePremise = new ArgsMePremise(
                            document.get("premiseId"),
                            document.get("premiseText"),
                            document.get("premiseStance"),
                            document.get("claimText"),
                            document.get("discussionTitle"),
                            document.get("premiseClusterId"),
                            document.get("dcf_cogency"),
                            document.get("dcf_reasonableness"),
                            document.get("dcf_effectiveness")
                    );
                    premises_extendedSet.add(argsMePremise);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return premises_extendedSet;
    }

    private Map<String, List<String>> getMapWithPremiseClusterIdsAndAListWithAllItsPremiseIds(List<ArgsMePremise> premises) {
        Map<String, List<String>> map_premiseClusterId_premiseIds = new HashMap<>();
        for (ArgsMePremise premise : premises) {
            map_premiseClusterId_premiseIds.putIfAbsent(premise.getPremiseClusterId(), new ArrayList<>());
            map_premiseClusterId_premiseIds.get(premise.getPremiseClusterId()).add(premise.getPremiseId());
        }
        return map_premiseClusterId_premiseIds;
    }


    private Map<String, Probability> estimateAndComputeSingleProbabilities(
            List<ArgsMePremise> premises,
            Map<String, List<String>> map_premiseClusterId_premiseIds,
            Map<String, Double> map_claimId_similarityScoreToQuery,
            Probability.ProbabilityType probabilityType) {
        Map<String, Probability> map_premiseId_probabilities = new HashMap<>();
        for (ArgsMePremise premise : premises) {
            // premise frequency
            String claimId = map_premiseId_claimId.get(premise.getPremiseId());
            int premiseFrequency = map_premiseClusterId_premiseIds.get(premise.getPremiseClusterId()).size()
                                 * map_claimClusterId_claimIds.get(map_claimId_claimClusterId.get(claimId)
            ).size();

            // inverse claim frequency
            HashSet<String> claimClusterIdsOfClaimFrequency = new HashSet<>();
            for (Map.Entry<String, List<String>> entry_claimClusterId_claimIds : map_claimClusterId_claimIds.entrySet()) {
                String claimClusterId = entry_claimClusterId_claimIds.getKey();
                List<String> claimIds = entry_claimClusterId_claimIds.getValue();
                for (String tmpClaimId : claimIds) {
                    List<String> premiseIds = map_claimId_premiseIds.get(tmpClaimId);

                    for (String premiseId : premiseIds) {
                        String tmpPremiseClusterID =  map_premiseId_premiseClusterId.get(premiseId);

                        if (tmpPremiseClusterID != null && tmpPremiseClusterID.equals(premise.getPremiseClusterId())) {
                            claimClusterIdsOfClaimFrequency.add(claimClusterId);
                        }
                    }

                }
            }
            int claimFrequency = claimClusterIdsOfClaimFrequency.size();

            double inverseClaimFrequency = Math.log(map_claimClusterId_claimIds.size() / (1. * claimFrequency))
                                           /
                                           Math.log(2);

            // P(c|q)
            double similarityScoreToQuery = 0;
            try {
                similarityScoreToQuery = map_claimId_similarityScoreToQuery.get(map_premiseId_claimId.get(premise.getPremiseId()));
            } catch (Exception e) {
                map_claimId_similarityScoreToQuery.put(premise.getPremiseId(), 0d);
            }

            map_premiseId_probabilities.put(
                    premise.getPremiseId(),
                    new Probability(
                            similarityScoreToQuery,
                            premiseFrequency,
                            claimFrequency,
                            inverseClaimFrequency,
                            Integer.parseInt(premise.getDcf_cogency()),
                            Integer.parseInt(premise.getDcf_reasonableness()),
                            Integer.parseInt(premise.getDcf_effectiveness())
                    )
            );
        }

        // normalize to get probabilities
        double sum_similarityQueryClaim = 0;
        double sum_pfIcf = 0;
        double sum_cogency = 0;
        double sum_reasonableness = 0;
        double sum_effectiveness = 0;

        for (Map.Entry<String, Probability> entry : map_premiseId_probabilities.entrySet()) {
            sum_similarityQueryClaim += entry.getValue().similarityScoreFromClaimToQuery;
            sum_pfIcf += entry.getValue().pfIcf_notNormalized;
            sum_cogency += entry.getValue().dcf_cogency;
            sum_reasonableness += entry.getValue().dcf_reasonableness;
            sum_effectiveness += entry.getValue().dcf_effectiveness;
        }

        for (Map.Entry<String, Probability> entry : map_premiseId_probabilities.entrySet()) {

            entry.getValue().normalizationForClaimRetrieval = sum_similarityQueryClaim;
            entry.getValue().prob_similarityScoreFromClaimToQuery = entry.getValue().similarityScoreFromClaimToQuery / sum_similarityQueryClaim;


            entry.getValue().normalization_cogency = sum_cogency;
            entry.getValue().prob_cogency = entry.getValue().dcf_cogency / sum_cogency;

            entry.getValue().normalization_reasonableness = sum_reasonableness;
            entry.getValue().prob_reasonableness = entry.getValue().dcf_reasonableness / sum_reasonableness;

            entry.getValue().normalization_effectiveness = sum_effectiveness;
            entry.getValue().prob_effectiveness = entry.getValue().dcf_effectiveness / sum_effectiveness;


            entry.getValue().normalization_pfIcf = sum_pfIcf;
            entry.getValue().prob_pfIcf = entry.getValue().pfIcf_notNormalized / sum_pfIcf;

            entry.getValue().normalization_DCFsCombined = sum_cogency + sum_reasonableness + sum_effectiveness;
            entry.getValue().prob_DCFsAggregated = entry.getValue().dcf_effectiveness / entry.getValue().normalization_DCFsCombined;

            entry.getValue().prob_average_pfIcf_prob_DCFsCombined = (entry.getValue().prob_pfIcf + entry.getValue().prob_DCFsAggregated) / 2;

            entry.getValue().setFinalSinglePremiseProbability(probabilityType);
        }

        return map_premiseId_probabilities;
    }

    private Map<String, List<Probability>> gatherAllProbabilitiesForEachCluster(List<ArgsMePremise> premises, Map<String, Probability> map_premiseId_probabilities) {
        Map<String, List<Probability>> map_premiseClusterId_probabilities = new HashMap<>();
        for (ArgsMePremise premise : premises) {
            map_premiseClusterId_probabilities.putIfAbsent(premise.getPremiseClusterId(), new ArrayList<>());
            map_premiseClusterId_probabilities.get(premise.getPremiseClusterId()).add(map_premiseId_probabilities.get(premise.getPremiseId()));
        }
        return map_premiseClusterId_probabilities;
    }

    private Map<String, List<Result>> getMapWithResultClusters(String queryClaim, Map<String, Double> map_claimId_similarityScoreToQuery, List<ArgsMePremise> premises, Map<String, Probability> map_premiseId_probabilities, Map<String, List<Probability>> map_premiseClusterId_listOfItsProbabilities, Probability.ProbabilityType probabilityType) {
        Map<String, List<Result>> map_premiseClusterId_result = new HashMap<>();
        for (ArgsMePremise premise : premises) {
            String resultClaimId = map_premiseId_claimId.get(premise.getPremiseId());
            Probability premiseProbability = map_premiseId_probabilities.get(premise.getPremiseId());

            Result result = new Result(
                    queryClaim,
                    resultClaimId,
                    premise.getClaimText(),
                    map_claimId_claimClusterId.get(resultClaimId),
                    map_claimId_similarityScoreToQuery.get(resultClaimId) == null ? 0 : map_claimId_similarityScoreToQuery.get(resultClaimId),
                    premiseProbability.normalizationForClaimRetrieval,

                    premise.getPremiseId(),
                    premise.getPremiseText(),
                    premise.getPremiseStance(),
                    premise.getPremiseClusterId(),

                    premiseProbability.pf,
                    premiseProbability.cf,
                    premiseProbability.icf,
                    premiseProbability.normalization_pfIcf,

                    premiseProbability.dcf_cogency,
                    premiseProbability.dcf_reasonableness,
                    premiseProbability.dcf_effectiveness,
                    premiseProbability.normalization_cogency,
                    premiseProbability.normalization_reasonableness,
                    premiseProbability.normalization_effectiveness,

                    probabilityType,
                    0
            );

            map_premiseClusterId_result.putIfAbsent(
                    premise.getPremiseClusterId(),
                    new ArrayList<>()
            );
            map_premiseClusterId_result.get(premise.getPremiseClusterId()).add(result);
        }

        for (Map.Entry<String, List<Result>> entry : map_premiseClusterId_result.entrySet()) {
            double finalClusterProbability = 0d;
            for (Result result : entry.getValue()) {
                finalClusterProbability += result.singleProbability;
            }
            for (Result result : entry.getValue()) {
                result.setClusterProbability(finalClusterProbability);
            }
        }

        for (Map.Entry<String, List<Result>> entry : map_premiseClusterId_result.entrySet()) {
            List<Result> results = entry.getValue();
            results.sort((o1, o2) -> {
                if (o1.premiseText.length() > o2.premiseText.length()) {
                    return -1;
                } else if (o1.premiseText.length() < o2.premiseText.length()) {
                    return 1;
                } else {
                    return Integer.compare(o1.premiseText.compareTo(o2.premiseText), 0);
                }
            });
            results.get(0).isRepresentative = true;
        }
        return map_premiseClusterId_result;
    }

}

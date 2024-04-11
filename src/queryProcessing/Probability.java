package queryProcessing;

public class Probability {

    double similarityScoreFromClaimToQuery;
    double normalizationForClaimRetrieval;
    double prob_similarityScoreFromClaimToQuery;

    int pf;
    int cf;
    double icf;
    double pfIcf_notNormalized;
    double normalization_pfIcf;
    double prob_pfIcf;

    double dcf_cogency;
    double dcf_reasonableness;
    double dcf_effectiveness;
    double dcf_DCFsCombined;
    double normalization_cogency;
    double normalization_reasonableness;
    double normalization_effectiveness;
    double normalization_DCFsCombined;
    double prob_cogency;
    double prob_reasonableness;
    double prob_effectiveness;
    double prob_DCFsAggregated;

    double prob_average_pfIcf_prob_DCFsCombined;

    double finalSinglePremiseProbability;


    public enum ProbabilityType {pfIcf, dcfsAggregated, average}


    public Probability(
            double similarityScoreFromClaimToQuery,
            int pf,
            int cf,
            double icf,
            double dcf_cogency,
            double dcf_reasonableness,
            double dcf_effectiveness
    ) {

        this.similarityScoreFromClaimToQuery = similarityScoreFromClaimToQuery;

        this.pf = pf;
        this.cf = cf;
        this.icf = icf;
        this.pfIcf_notNormalized = pf * icf;

        this.dcf_cogency = dcf_cogency;
        this.dcf_reasonableness = dcf_reasonableness;
        this.dcf_effectiveness = dcf_effectiveness;
        this.dcf_DCFsCombined = dcf_cogency + dcf_reasonableness + dcf_effectiveness;
    }

    public double getFinalSinglePremiseProbability (ProbabilityType probabilityType) {
        switch (probabilityType) {
            case pfIcf:
                return this.prob_similarityScoreFromClaimToQuery * prob_pfIcf;
            case dcfsAggregated:
                return this.prob_similarityScoreFromClaimToQuery * prob_DCFsAggregated;
            case average:
                return this.prob_similarityScoreFromClaimToQuery * prob_average_pfIcf_prob_DCFsCombined;
            default:
                return 0;
        }
    }

    public void setFinalSinglePremiseProbability (ProbabilityType probabilityType) {
        this.finalSinglePremiseProbability = getFinalSinglePremiseProbability(probabilityType);
    }
}
